package com.zazoapp.client.notification;

import android.util.Log;

import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.network.HttpRequest;

public class NotificationHandler {
	public static String STAG = NotificationHandler.class.getSimpleName();
	
	public static class DataKeys{
		public static String TARGET_MKEY = "target_mkey";
		public static String FROM_MKEY = "from_mkey";
	    public static String SENDER_NAME = "sender_name";
	    public static String VIDEO_ID = "video_id";
	    public static String TO_MKEY = "to_mkey";
	    public static String STATUS = "status";
	    public static String TYPE = "type";
	    public static String SERVER_HOST = "host";
	}
	
	public static class StatusEnum{
		public static String DOWNLOADED = "downloaded";
		public static String VIEWED = "viewed";
	}
	
	public static class TypeEnum{
		public static String VIDEO_RECEIVED = "video_received";
		public static String VIDEO_STATUS_UPDATE = "video_status_update";
	}

	public static void sendForVideoReceived(Friend friend, String videoId) {
		LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
		params.put(NotificationHandler.DataKeys.TARGET_MKEY, friend.get(Friend.Attributes.MKEY));
		params.put(NotificationHandler.DataKeys.FROM_MKEY, UserFactory.current_user().get(User.Attributes.MKEY));
		params.put(NotificationHandler.DataKeys.SENDER_NAME, UserFactory.current_user().get(User.Attributes.FIRST_NAME));
		params.put(NotificationHandler.DataKeys.VIDEO_ID, videoId);
		new SendNotification("notification/send_video_received", params, "POST");
	}
	
	public static void sendForVideoStatusUpdate(Friend friend, String videoId, String status){
		LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
		params.put(NotificationHandler.DataKeys.TARGET_MKEY, friend.get(Friend.Attributes.MKEY));
		params.put(NotificationHandler.DataKeys.TO_MKEY, UserFactory.current_user().get(User.Attributes.MKEY));
		params.put(NotificationHandler.DataKeys.STATUS, status);
		params.put("video_id", videoId);
		new SendNotification("notification/send_video_status_update", params, "POST");
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
