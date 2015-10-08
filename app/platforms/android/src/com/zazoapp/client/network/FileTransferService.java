package com.zazoapp.client.network;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import com.zazoapp.client.core.IntentHandlerService;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.network.aws.S3FileTransferAgent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public abstract class FileTransferService extends IntentService {
    private static final String TAG = FileTransferService.class.getSimpleName();
    private final String OTAG = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + ": ";
    public static final String RESET_ACTION = "reset";
    private final static Integer MAX_RETRIES = 100;

    protected String id;

    protected IFileTransferAgent fileTransferAgent;

    // Interrupt support
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition reset = lock.newCondition();
    private final AtomicInteger retryCount = new AtomicInteger();

    public static class IntentFields {
        public static final String ID_KEY = "id";
        public static final String TRANSFER_TYPE_KEY = "transferType";
        public static final String RETRY_COUNT_KEY = "retryCount";
        public static final String FILE_PATH_KEY = "filePath";
        public static final String FILE_NAME_KEY = "filename";
        public static final String PARAMS_KEY = "params";
        public static final String STATUS_KEY = "status";
        public static final String VIDEO_ID_KEY = "videoIdKey";
        public static final String METADATA = "metadata";

        public static final String TRANSFER_TYPE_UPLOAD = "upload";
        public static final String TRANSFER_TYPE_DOWNLOAD = "download";
        public static final String TRANSFER_TYPE_DELETE = "delete";
    }

    public static class MetaData {
        private static final String FIELD_VIDEO_ID = "video_id";
        private static final String FIELD_SENDER_MKEY = "sender_mkey";
        private static final String FIELD_RECEIVER_MKEY = "receiver_mkey";
        private static final String FIELD_CLIENT_VERSION = "client_version";
        private static final String FIELD_CLIENT_PLATFORM = "client_platform";

        private static final String PLATFORM = "android";
        /**
         * Returns bundle with metadata for video based on params
         * @param videoId video ID
         * @param sender sender MKEY
         * @param receiver receiver MKEY
         * @return metadata
         */
        public static Bundle getMetadata(@NonNull String videoId, @NonNull String sender, @NonNull String receiver) {
            Bundle metadata = new Bundle();
            metadata.putString(FIELD_VIDEO_ID, videoId);
            metadata.putString(FIELD_SENDER_MKEY, sender);
            metadata.putString(FIELD_RECEIVER_MKEY, receiver);
            metadata.putString(FIELD_CLIENT_VERSION, TbmApplication.getVersionNumber());
            metadata.putString(FIELD_CLIENT_PLATFORM, PLATFORM);
            return metadata;
        }
    }

    protected abstract void maxRetriesReached(Intent intent) throws InterruptedException;

    protected abstract boolean doTransfer(Intent intent) throws InterruptedException;

    public FileTransferService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (NetworkConfig.IS_AWS_USING) {
            fileTransferAgent = new S3FileTransferAgent(this);
        } else {
            fileTransferAgent = new ServerFileTransferAgent(this);
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        if (RESET_ACTION.equals(intent.getAction())) {
            Log.i(TAG, OTAG + "Reset retries");
            retryCount.set(0);
            if (lock.tryLock()) {
                try {
                    reset.signal();
                } finally {
                    lock.unlock();
                }
            }
        }
        super.onStart(intent, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        lock.lock();
        try {
            Log.i(TAG, OTAG + "onHandleIntent");
            if (RESET_ACTION.equals(intent.getAction())) {
                return;
            }
            fileTransferAgent.setInstanceVariables(intent);
            retryCount.set(0);
            while (true) {
                if (!isConnected()) {
                    if (!reset.await(2, TimeUnit.SECONDS)) {
                        continue;
                    }
                    intent.putExtra(IntentFields.RETRY_COUNT_KEY, retryCount.get());
                }
                if (retryCount.get() > MAX_RETRIES) {
                    maxRetriesReached(intent);
                    break;
                }
                if (doTransfer(intent))
                    break;
                retrySleep(intent);
            }
        } catch (InterruptedException e) {
        } finally {
            lock.unlock();
        }
    }

    public void reportStatus(Intent intent, int status) {
        Log.i(TAG, OTAG + "reportStatus: " + status);
        Intent newIntent = new Intent(intent);
        newIntent.setClass(getApplicationContext(), IntentHandlerService.class);
        newIntent.putExtra(IntentFields.STATUS_KEY, status);
        getApplicationContext().startService(newIntent);
    }

    private void retrySleep(Intent intent) throws InterruptedException {
        intent.putExtra(IntentFields.RETRY_COUNT_KEY, retryCount.incrementAndGet());
        Log.i(TAG, OTAG + "retry: " + retryCount.get());

        long sleepTime = (DebugConfig.getInstance(this).isDebugEnabled()) ? 1000L : sleepTime(retryCount.get());

        Log.i(TAG, OTAG + "Sleeping for: " + sleepTime + "ms");
        if (reset.await(sleepTime, TimeUnit.MILLISECONDS)) {
            intent.putExtra(IntentFields.RETRY_COUNT_KEY, retryCount.get());
        }
    }

    private long sleepTime(int retryCount) {
        if (retryCount <= 9) {
            return 1000 * (1 << retryCount);
        }
        return 512000;
    }

    public static void reset(Context context, Class<? extends FileTransferService> clazz) {
        Intent i = new Intent(context, clazz);
        i.setAction(FileUploadService.RESET_ACTION);
        context.startService(i);
    }

    private boolean isConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}