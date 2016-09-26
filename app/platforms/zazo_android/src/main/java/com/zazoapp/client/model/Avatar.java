package com.zazoapp.client.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferType;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.zazoapp.client.Config;
import com.zazoapp.client.multimedia.VideoIdUtils;
import com.zazoapp.client.network.HttpRequest;
import com.zazoapp.client.network.aws.S3AvatarDownloadHelper;
import com.zazoapp.client.network.aws.S3CredentialsStore;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 9/5/2016.
 */
public class Avatar<T extends ActiveModel & AvatarProvidable> {

    private static final String TAG = Avatar.class.getSimpleName();
    public static final String FILE_EXTENSION = ".png";
    public static final String KEY_TEMPLATE = "%s_%s";

    private static final String AVATARS_API = "/api/v1/avatars";

    public enum ThumbnailType {
        LAST_FRAME("last_frame"),
        PHOTO("avatar");

        private String optionName;

        ThumbnailType(String optionName) {
            this.optionName = optionName;
        }

        public String optionName() {
            return optionName;
        }

        public static ThumbnailType getType(String name) {
            if (!TextUtils.isEmpty(name)) {
                for (ThumbnailType type : values()) {
                    if (type.optionName().equals(name)) {
                        return type;
                    }
                }
            }
            return LAST_FRAME;
        }
    }

    private T model;

    public Avatar(T model) {
        this.model = model;
    }

    public boolean exists() {
        File file = new File(getAvatarPath());
        return file.exists();
    }

    public boolean existsOnServer() {
        return !TextUtils.isEmpty(model.getAvatarTimestamp());
    }

    public boolean existsSomewhere() {
        return exists() || getType() == ThumbnailType.PHOTO && existsOnServer();
    }

    public void updateBitmap() {
        ThumbnailType thumbnailType = ThumbnailType.getType(model.getAvatarOption());
        final String imageKey = model.getAvatarFileName(thumbnailType);
        removeFromCache(imageKey);
        if (exists()) {
            loadBitmapInner(imageKey);
        }
    }

    public Bitmap loadBitmap() {
        ThumbnailType thumbnailType = ThumbnailType.getType(model.getAvatarOption());
        final String imageKey = model.getAvatarFileName(thumbnailType);

        return loadBitmapInner(imageKey);
    }

    private Bitmap loadBitmapInner(String key) {
        Bitmap bitmap = getBitmapFromMemCache(key);
        Log.d(TAG, "loadTo " + mMemoryCache.size());
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeFile(getAvatarPath());
            addBitmapToMemoryCache(key, bitmap);
        }
        return bitmap;
    }

    public ThumbnailType getType() {
        return ThumbnailType.getType(model.getAvatarOption());
    }

    public String getAvatarPath() {
        return model.getAvatarFileName(getType());
    }

    public void delete(boolean onServer, HttpRequest.Callbacks callbacks) {
        if (onServer) {
            delete(callbacks);
        } else {
            if (exists()) {
                model.set(AvatarProvidable.USE_AS_THUMBNAIL, ThumbnailType.LAST_FRAME.optionName());
                model.set(AvatarProvidable.AVATAR_TIMESTAMP, String.valueOf(0));
            }
        }
    }

    private String getKey() {
        return String.format(KEY_TEMPLATE, model.getMkey(), model.getAvatarTimestamp());
    }

    private static final LruCache<String, Bitmap> mMemoryCache;
    static {
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    private static void addBitmapToMemoryCache(@NonNull String key, @NonNull Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    private static Bitmap getBitmapFromMemCache(@NonNull String key) {
        return mMemoryCache.get(key);
    }

    private static void removeFromCache(@NonNull String key) {
        mMemoryCache.remove(key);
    }

    private static void delete(HttpRequest.Callbacks callbacks) {
        new HttpRequest.Builder()
                .setMethod(HttpRequest.DELETE)
                .setUri(AVATARS_API)
                .setCallbacks(callbacks)
                .build();
    }

    public static void upload(String path, ThumbnailType option) {
        new HttpRequest.Builder()
                .setMethod(HttpRequest.POST)
                .setUri(AVATARS_API)
                .addParam(AvatarProvidable.USE_AS_THUMBNAIL, option.optionName())
                .addParam("avatar", new File(path))
                .build();
    }

    public static void update(ThumbnailType type, HttpRequest.Callbacks callbacks) {
        JSONObject object = new JSONObject();
        try {
            object.put(AvatarProvidable.USE_AS_THUMBNAIL, type.optionName());
            new HttpRequest.Builder()
                    .setMethod(HttpRequest.UPDATE)
                    .setUri(AVATARS_API)
                    .setCallbacks(callbacks)
                    .setJsonParams(object)
                    .build();
        } catch (JSONException e) {
        }
    }

    public static <T extends ActiveModel & AvatarProvidable> void download(String mkey, final String timestamp, final T model) {
        String fileKey = String.format(KEY_TEMPLATE, mkey, timestamp);
        final String name = fileKey + FILE_EXTENSION;
        // Check if current download wasn't started yet, skip if so
        Context context = model.getContext();
        String filepath = Config.homeDirPath(context) + File.separator + name;
        final File file = new File(filepath);
        if (file.exists()) {
            setAvatarFromFile(file, model, timestamp);
            return;
        }
        final S3CredentialsStore credentials = S3AvatarDownloadHelper.getCredentialsStore(context);
        final File cacheFile = new File(model.getContext().getCacheDir(), name);
        final TransferUtility tf = S3AvatarDownloadHelper.getTransferUtility(model.getContext());
        List<TransferObserver> observers = tf.getTransfersWithType(TransferType.DOWNLOAD);
        final String absolutePath = cacheFile.getAbsolutePath();
        TransferObserver observer = null;
        for (TransferObserver o : observers) {
            if (absolutePath.equals(o.getAbsoluteFilePath())) {
                if (o.getState() == TransferState.COMPLETED && cacheFile.exists()) {
                    if (cacheFile.renameTo(file)) {
                        setAvatarFromFile(file, model, timestamp);
                        tf.deleteTransferRecord(o.getId());
                    }
                } else if (o.getState() == TransferState.FAILED) {
                    tf.deleteTransferRecord(o.getId());
                } else {
                    observer = o;
                }
                break;
            }
        }
        if (observer == null) {
           observer = tf.download(credentials.getS3Bucket(), fileKey, cacheFile);
        } else {
            observer.cleanTransferListener();
        }
        final TransferObserver finalObserver = observer;
        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state == TransferState.COMPLETED) {
                    finalObserver.cleanTransferListener();
                    if (cacheFile.exists()) {
                        if (cacheFile.renameTo(file)) {
                            setAvatarFromFile(file, model, timestamp);
                            tf.deleteTransferRecord(finalObserver.getId());
                        }
                    }
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.d(TAG, "transfer listener " + id, ex);
            }
        });
    }

    private static <T extends ActiveModel & AvatarProvidable> void setAvatarFromFile(File file, T model, String timestamp) {
        boolean isFriendModel = model instanceof Friend;
        try {
            FileUtils.copyFile(file, new File(model.getAvatar().getAvatarPath()));
            model.getAvatar().deleteCurrentAvatar();
            model.set(AvatarProvidable.AVATAR_TIMESTAMP, timestamp);
            if (isFriendModel) {
                model.set(AvatarProvidable.USE_AS_THUMBNAIL, ThumbnailType.PHOTO.optionName());
            }
            model.getAvatar().updateBitmap();
            if (isFriendModel) {
                FriendFactory.getFactoryInstance().notifyStatusChanged((Friend) model);
            }
        } catch (IOException e) {
        }
    }

    public void deleteCurrentAvatar() {
        String oldPath = String.format(KEY_TEMPLATE + FILE_EXTENSION, model.getMkey(), model.getAvatarTimestamp());
        File oldAvatar = new File(oldPath);
        if (oldAvatar.exists()) {
            oldAvatar.delete();
        }
    }

    // TODO download user avatar settings on registration step
    public static void getCurrentAvatarInfo(final User user, final HttpRequest.Callbacks callbacks) {
        new HttpRequest.Builder()
                .setMethod(HttpRequest.GET)
                .setUri(AVATARS_API)
                .setCallbacks(new HttpRequest.Callbacks() {
                    @Override
                    public void success(String response) {
                        Gson gson = new Gson();
                        try {
                            GetAvatarResponse avatarResponse = gson.fromJson(response, GetAvatarResponse.class);
                            if (avatarResponse != null && avatarResponse.data != null) {
                                FriendFactory.AvatarData avatarData = avatarResponse.data;
                                ThumbnailType newUseOption = Avatar.ThumbnailType.getType(avatarData.useOption);
                                long newTimestamp = VideoIdUtils.timeStampFromVideoId(avatarData.timestamp);
                                ThumbnailType currentUseOption = user.getAvatar().getType();
                                long currentTimestamp = VideoIdUtils.timeStampFromVideoId(user.getAvatarTimestamp());
                                if (newTimestamp > currentTimestamp) {
                                    download(user.getMkey(), String.valueOf(newTimestamp), user);
                                }
                                if (currentUseOption != newUseOption) {
                                    user.set(AvatarProvidable.USE_AS_THUMBNAIL, newUseOption.optionName());
                                }
                            }
                            if (callbacks != null) {
                                callbacks.success(response);
                            }
                        } catch (JsonSyntaxException e) {
                            error(e.getMessage());
                        }
                    }

                    @Override
                    public void error(String errorString) {
                        if (callbacks != null) {
                            callbacks.error(errorString);
                        }
                    }
                })
                .build();
    }

    private static class GetAvatarResponse {
        FriendFactory.AvatarData data;
    }
}
