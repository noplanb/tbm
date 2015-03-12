package com.zazoapp.client.multimedia;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.util.Log;

/**
 * Uses native Android metadata retriever to get thumbnail or notify error
 * Created by skamenkovych@codeminders.com on 3/10/2015.
 */
public class ThumbnailRetriever {
    private static final String TAG = ThumbnailRetriever.class.getSimpleName();

    private MediaMetadataRetriever nativeRetriever = new MediaMetadataRetriever();

    private String errorMessage;
    boolean nativeWorks = true;

    /**
     * Gets thumbnail for video located in path.
     * If operation is wrong it throws {@code ThumbnailBrokenException}
     * @param path video file location
     * @return thumbnail bitmap
     */
    public Bitmap getThumbnail(String path) throws ThumbnailBrokenException {
        //ThumbnailUtils.createVideoThumbnail()
        Bitmap thumb = null;
        try {
            try {
                nativeRetriever.setDataSource(path);
            } catch (RuntimeException e) {
                markFailed("native: Error setting datasource. Assume that file is corrupted", e.toString());
                return null;
            }

            String time = nativeRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (time == null) {
                markFailed("native: Error getting duration");
                return null;
            }
            long nativeDuration = Long.parseLong(time);
            long pos = getPos(nativeDuration);
            thumb = nativeRetriever.getFrameAtTime(pos*1000);
            if (thumb == null) {
                Log.e(TAG, "native: Error getting thumb");
                thumb = nativeRetriever.getFrameAtTime(nativeDuration*1000);
                if (thumb == null) {
                    Log.e(TAG, "native: Error getting end frame");
                    thumb = nativeRetriever.getFrameAtTime();
                    if (thumb == null) {
                        markFailed("native: Error getting representative frame");
                    }
                }
            }
        } finally {
            verifyAndRelease();
            return thumb;
        }
    }

    /**
     * ensure
     */
    private void verifyAndRelease() throws ThumbnailBrokenException {
        nativeRetriever.release();
        if (!nativeWorks) {
            StringBuilder message = new StringBuilder();
            message.append("Thumbnail has not been retrieved\n").append(errorMessage);
            throw new ThumbnailBrokenException(message.toString());
        } else {
            Log.i(TAG, "Thumbnail retrieved successfully");
        }
    }

    private void markFailed(String... messages) {
        nativeWorks = false;
        StringBuilder builder = new StringBuilder();
        for (String message : messages) {
            builder.append(message).append("\n");
        }
        errorMessage = builder.toString();
    }

    private static long getPos(long duration) {
        return (duration > 2500) ? duration - 2000 : duration / 2;
    }

    public class ThumbnailBrokenException extends Exception {
        public ThumbnailBrokenException() {
            super();
        }

        public ThumbnailBrokenException(String detailMessage) {
            super(detailMessage);
        }
    }
}
