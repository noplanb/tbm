package com.noplanbees.tbm;

import android.util.Log;

import com.google.gson.internal.LinkedTreeMap;

public class NotificationHandler {
	public static String STAG = NotificationHandler.class.getSimpleName();
	
	public static class DataKeys{
		public static String TARGET_MKEY = "target_mkey";
		public static String FROM_MKEY = "from_mkey";
	    public static String SENDER_NAME = "sender_name";
	    public static String VIDEO_ID = "video_id";
	    public static String TO_MKEY = "to_mkey";
	    public static String STATUS = "status";
	}
	
	public static class StatusEnum{
		public static String DOWNLOADED = "downloaded";
		public static String VIEWED = "viewed";
	}

	public static void sendForVideoReceived(Friend friend) {
		LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
		params.put(NotificationHandler.DataKeys.TARGET_MKEY, friend.get(Friend.Attributes.MKEY));
		params.put(NotificationHandler.DataKeys.FROM_MKEY, UserFactory.current_user().get(User.Attributes.MKEY));
		params.put(NotificationHandler.DataKeys.SENDER_NAME, UserFactory.current_user().get(User.Attributes.FIRST_NAME));
		params.put(NotificationHandler.DataKeys.VIDEO_ID, friend.get(Friend.Attributes.OUTGOING_VIDEO_ID));
		new SendNotification("notification/send_video_received", params, "POST");
	}
	
	public static void sendForVideoStatusUpdate(Friend friend, String status){
		LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
		params.put(NotificationHandler.DataKeys.TARGET_MKEY, friend.get(Friend.Attributes.MKEY));
		params.put(NotificationHandler.DataKeys.TO_MKEY, UserFactory.current_user().get(User.Attributes.MKEY));
		params.put(NotificationHandler.DataKeys.STATUS, status);
		params.put("video_id", friend.get(Friend.Attributes.INCOMING_VIDEO_ID));
		new SendNotification("notification/send_video_status_update", params, "POST");
	}

	private static class SendNotification extends Server{
		public SendNotification(String uri, LinkedTreeMap<String, String> params, String method) {
			super(uri, params, method);
		}
		@Override
		public void success(String response) {
			Log.i(STAG, "SendNotification: success");
		}
		@Override
		public void error(String errorString) {
			Log.e(STAG, "SendNotification: ERROR: " + errorString);
		}
	}
}
