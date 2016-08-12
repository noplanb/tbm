package com.zazoapp.client.notification;

public class NotificationHandler {
	public static final String STAG = NotificationHandler.class.getSimpleName();
	
	public static class DataKeys{
		public static final String TARGET_MKEY = "target_mkey";
		public static final String FROM_MKEY = "from_mkey";
	    public static final String SENDER_NAME = "sender_name";
	    public static final String VIDEO_ID = "video_id";
	    public static final String MESSAGE_ID = "message_id";
	    public static final String TO_MKEY = "to_mkey";
	    public static final String STATUS = "status";
	    public static final String TYPE = "type";
	    public static final String SERVER_HOST = "host";
        public static final String DATE_START = "date_start";
        public static final String DATE_END = "date_end";
        public static final String CONTENT_TYPE = "content_type";
        public static final String BODY = "body";

        // Friend finder
        public static final String CONTENT = "content";
        public static final String SUBJECT = "subject";
        public static final String ADDITIONS = "additions";
        public static final String NKEY = "nkey";

        public static final String CONDITION = "condition";
    }

    public static class StatusEnum {
        public static final String DOWNLOADED = "downloaded";
        public static final String VIEWED = "viewed";
    }

    public static class TypeEnum {
        public static final String VIDEO_RECEIVED = "video_received";
        public static final String VIDEO_STATUS_UPDATE = "video_status_update";
        public static final String LOG_REQUEST = "log_request";
        public static final String USER_DATA_REQUEST = "user_data_request";
        public static final String FRIEND_JOINED = "friend_joined";
        public static final String MESSAGE_RECEIVED = "message_received";
        public static final String MESSAGE_STATUS_UPDATE = "message_status_update";
    }

    public static class Condition {
        public static final String NO_USER = "no_user";
    }

}
