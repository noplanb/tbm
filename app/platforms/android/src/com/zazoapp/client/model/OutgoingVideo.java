package com.zazoapp.client.model;

import android.content.Context;

/**
 * Created by skamenkovych@codeminders.com on 5/29/2015.
 */
public class OutgoingVideo extends Video {
    /**
     * Normal state machine (one message): NEW -> QUEUED -> UPLOADING -> UPLOADED -> DOWNLOADED -(onViewed)-> VIEWED
     */
    public static class Status {
        public static final int NONE = 0;
        public static final int NEW = 1;
        public static final int QUEUED = 2;
        public static final int UPLOADING = 3;
        public static final int UPLOADED = 4;
        public static final int DOWNLOADED = 5;
        public static final int VIEWED = 6;
        public static final int FAILED_PERMANENTLY = 7;

        public static boolean isSent(int status) {
            return status == UPLOADED || status == DOWNLOADED || status == VIEWED;
        }
    }

    @Override
    public void init(Context context) {
        super.init(context);
        setVideoStatus(Status.NONE);
        setRetryCount(0);
    }

    public boolean isSent() {
        int status = getVideoStatus();
        return Status.isSent(status);
    }
}
