package com.zazoapp.client.notification;

import android.util.Log;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.network.HttpRequest;

public class NotificationHandler {
	public static final String STAG = NotificationHandler.class.getSimpleName();
	
	public static class DataKeys{
		public static final String TARGET_MKEY = "target_mkey";
		public static final String FROM_MKEY = "from_mkey";
	    public static final String SENDER_NAME = "sender_name";
	    public static final String VIDEO_ID = "video_id";
	    public static final String TO_MKEY = "to_mkey";
	    public static final String STATUS = "status";
	    public static final String TYPE = "type";
	    public static final String SERVER_HOST = "host";
        public static final String DATE_START = "date_start";
        public static final String DATE_END = "date_end";

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
    }

    public static class Condition {
        public static final String NO_USER = "no_user";
    }

    public static void sendForVideoReceived(Friend friend, String videoId) {
        LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
        params.put(NotificationHandler.DataKeys.TARGET_MKEY, friend.getMkey());
        params.put(NotificationHandler.DataKeys.FROM_MKEY, UserFactory.getCurrentUserMkey());
        params.put(NotificationHandler.DataKeys.SENDER_NAME, UserFactory.current_user().getFirstName());
        params.put(NotificationHandler.DataKeys.VIDEO_ID, videoId);
        new SendNotification("notification/send_video_received", params, HttpRequest.POST);
    }

    public static void sendForVideoStatusUpdate(Friend friend, String videoId, String status) {
        LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
        params.put(NotificationHandler.DataKeys.TARGET_MKEY, friend.getMkey());
        params.put(NotificationHandler.DataKeys.TO_MKEY, UserFactory.getCurrentUserMkey());
        params.put(NotificationHandler.DataKeys.STATUS, status);
        params.put("video_id", videoId);
        new SendNotification("notification/send_video_status_update", params, HttpRequest.POST);
    }

    private static class SendNotification extends HttpRequest {
        public SendNotification(String uri, LinkedTreeMap<String, String> params, String method) {
            super(uri, params, method, new Callbacks() {
                @Override
                public void success(String response) {
                    Log.i(STAG, "SendNotification: success");
                }

                @Override
                public void error(String errorString) {
                    Log.d(STAG, "SendNotification: ERROR: " + errorString);
                }
            });
        }
    }
}
