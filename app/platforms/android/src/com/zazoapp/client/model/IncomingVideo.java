package com.zazoapp.client.model;

import android.content.Context;

public class IncomingVideo extends Video {

    /**
     * Normal state machine: QUEUED -> NEW <-> DOWNLOADING -> DOWNLOADED -(onViewed)-> VIEWED
     */
    public static class Status {
        public static final int NONE = 0;
        public static final int NEW = 1;
        public static final int QUEUED = 2;
        public static final int DOWNLOADING = 3;
        public static final int DOWNLOADED = 4;
        public static final int VIEWED = 5;
        public static final int FAILED_PERMANENTLY = 6;
    }

    @Override
    public void init(Context context) {
        super.init(context);
        setVideoStatus(Status.NONE);
        setRetryCount(0);
    }

    public boolean isDownloaded() {
        return getVideoStatus() == Status.DOWNLOADED || getVideoStatus() == Status.VIEWED;
    }
}
