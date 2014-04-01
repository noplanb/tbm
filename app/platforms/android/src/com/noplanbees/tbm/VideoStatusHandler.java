package com.noplanbees.tbm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class VideoStatusHandler {
	private final String TAG = this.getClass().getSimpleName();
	
	public static final int NEW 		= 0;
	public static final int UPLOADING 	= 1;
	public static final int RETRY 		= 2;
	public static final int UPLOADED 	= 3;
	public static final int DOWNLOADED 	= 4;
	public static final int VIEWED		= 5;
	
	public static final String STATUS_KEY = "status";
	public static final String RETRY_COUNT_KEY = "retryCount";
	
	private FriendFactory friendFactory;
	private Context context;
	
	public VideoStatusHandler(Context c){
		context = c;
		friendFactory = ActiveModelsHandler.ensureFriend();
	}

	private String statusStr(int status, int retryCount) {
		switch (status){
		case NEW: case UPLOADING:
			return "p...";
		case RETRY: 
			return String.format("r%d...", retryCount);
		case UPLOADED:
			return ".s..";
		case DOWNLOADED:
			return "..p.";
		case VIEWED:
			return "v!";
		}
		return "";
	}
	
	public String getStatusStr(Friend friend){
		String sStatus = friend.get("sentVideoStatus");
		if (sStatus.isEmpty())
			return friend.get("firstName");
		
		Integer status = Integer.parseInt(sStatus);
		Integer retryCount = Integer.parseInt(friend.get("sentVideoRetryCount"));
		String name = status != VIEWED ? "" : friend.get("firstName");
		String ss = statusStr(status, retryCount);
		return name + " " + ss;
	}
	
	public void update(Intent intent){
		Log.i(TAG, "update");
		updateFriend(intent);
		notifyHomeActivity(intent);
	}
	
	private void updateFriend(Intent intent) {
		Friend friend = friendFactory.getFriendFromIntent(intent);
		Bundle extras = intent.getExtras();
		if(friend != null && extras != null){
			Integer status = extras.getInt(STATUS_KEY);
			Integer retryCount = extras.getInt(RETRY_COUNT_KEY);
			friend.set("sentVideoStatus", status.toString());
			friend.set("sentVideoRetryCount", retryCount.toString());
			friendFactory.save();
		} else {
			Log.e(TAG, "updateFriend: Tried to update where friend or extras where null");
		}
	}

	public void notifyHomeActivity(Intent intent){
		Log.i(TAG, "notifyHomeActivity");
		Bundle extras = intent.getExtras();		
		Intent i = new Intent(context, HomeActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		extras.putInt(IntentHandler.INTENT_TYPE_KEY, IntentHandler.TYPE_VIDEO_STATUS_UPDATE);
		i.putExtras(extras);
		context.startActivity(i);
	}
}
