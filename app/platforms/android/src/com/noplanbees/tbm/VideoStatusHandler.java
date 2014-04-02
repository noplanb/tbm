package com.noplanbees.tbm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.internal.LinkedTreeMap;

public class VideoStatusHandler {
	private final String TAG = this.getClass().getSimpleName();
	
	private Context context;
	public VideoStatusHandler(Context c){
		context = c;
	}
	
	public VideoStatusHandler(){}
	
	
	//-------------------
	// Local videoViewed stuff
	//-------------------

	public void setVideoViewed(Friend f) {
		setVideoNotViewedState(f, false);
		notifyServerVideoViewed(f);
	}

	public void setVideoNotViewed(Friend f){
		setVideoNotViewedState(f, true);
		// Also clear the sentVideoStatus for the last received video here because it makes sense in the ui
		// if you got a new video it definitely means the other person has seen your last 
		String friendId = f.getId();
		FriendFactory friendFactory = ActiveModelsHandler.retrieveFriend();
		Friend friend = (Friend) friendFactory.find(friendId);
		friend.set("sentVideoStatus", ((Integer) NEW).toString());
		Log.i(TAG, "setVideoNotViewed" + f.attributes.toString());
		ActiveModelsHandler.saveFriend();
	}

	// Assume that multiple processes can update the videoViewed field in the friend model
	// let the saved state be used for interprocess communication. Therefore always read
	// from file update model then save to file when writing.
	private void setVideoNotViewedState(Friend f, Boolean value){
		Log.i(TAG, String.format("setVideoNotViewedState: %s %b", f.get("firstName"), value ));
		String friendId = f.getId();
		FriendFactory friendFactory = ActiveModelsHandler.retrieveFriend();
		Friend friend = (Friend) friendFactory.find(friendId);
		if (value){
			friend.set("videoNotViewed", "true");
		} else{
			friend.set("videoNotViewed", "false");
		}
		friendFactory.save();
	}
	
	// Assumes multiple processes can read and the file saved model is used for IPC
	// so always read from file.
	public boolean videoNotViewed(Friend f){
		Log.i(TAG, "videoNotViewed: checked for " + f.get("firstName"));
		String friendId = f.getId();
		FriendFactory friendFactory = ActiveModelsHandler.retrieveFriend();
		Friend friend = (Friend) friendFactory.find(friendId);
		return friend.get("videoNotViewed") != null && friend.get("videoNotViewed").startsWith("t");
	}
	
	private void notifyServerVideoViewed(Friend f) {
		LinkedTreeMap<String, String>params = new LinkedTreeMap<String, String>();
		params.put("from_id", f.getId());
		params.put("to_id", User.userId());
		new SGet("videos/update_viewed", params);
	}
	
	private class SGet extends Server{
		public SGet(String uri, LinkedTreeMap<String, String> params) {
			super(uri, params);
		}
		@Override
		public void callback(String response) {
			Log.i(TAG, "callback: " + response);
		}
	}
	
	//-------------------
	// SentVideoStatus model stuff
	//-------------------
	public static final int NEW 		= 0;
	public static final int UPLOADING 	= 1;
	public static final int RETRY 		= 2;
	public static final int UPLOADED 	= 3;
	public static final int DOWNLOADED 	= 4;
	public static final int SENT_VIEWED	= 5;

	public static final String STATUS_KEY = "status";
	public static final String RETRY_COUNT_KEY = "retryCount";

	public void updateSentVideoStatus(Intent intent){
		Log.i(TAG, "update");
		updateFriendSentVideoStatus(intent);
		Friend friend = ActiveModelsHandler.ensureFriend().getFriendFromIntent(intent);
		notifyHomeActivityOfVideoStatus(friend);
	}

	// Assume that multiple processes can update the videoStatus fields in the friend model
	// let the saved state be used for interprocess communication. Therefore always read
	// from file update model then save to file when writing.
	private synchronized void updateFriendSentVideoStatus(Intent intent) {
		FriendFactory friendFactory = ActiveModelsHandler.retrieveFriend();
		Friend friend = friendFactory.getFriendFromIntent(intent);
		Bundle extras = intent.getExtras();
		if(friend != null && extras != null){
			Integer status = extras.getInt(STATUS_KEY);
			Integer retryCount = extras.getInt(RETRY_COUNT_KEY);

			if (status != null)
				friend.set("sentVideoStatus", status.toString());
			if (retryCount != null)
				friend.set("sentVideoRetryCount", retryCount.toString());
			friendFactory.save();
		} else {
			Log.e(TAG, "updateFriend: Tried to update where friend or extras where null");
		}
	}

	// Assumes multiple processes can read and the file saved model is used for IPC
	// so always read from file.	
	private Bundle getSentVideoStatus(Friend f){
		String friendId = f.getId();
		Bundle r = new Bundle();
		FriendFactory friendFactory = ActiveModelsHandler.retrieveFriend();
		Friend friend = (Friend) friendFactory.find(friendId);

		String status = friend.get("sentVideoStatus");
		if (!status.isEmpty())
			r.putInt( STATUS_KEY, Integer.parseInt(status) );

		String retryCount = friend.get("sentVideoRetryCount");
		if (!retryCount.isEmpty())
			r.putInt( RETRY_COUNT_KEY, Integer.parseInt(retryCount) );

		return r;
	}

	public void notifyHomeActivityOfVideoStatus(Friend friend){
		Log.i(TAG, "notifyHomeActivity");
		Bundle extras = new Bundle();		
		Intent i = new Intent(context, HomeActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		extras.putInt(IntentHandler.INTENT_TYPE_KEY, IntentHandler.TYPE_VIDEO_STATUS_UPDATE);
		extras.putString("friendId", friend.getId());
		i.putExtras(extras);
		context.startActivity(i);
	}

	//------------------
	// sentVideoStatus view helper stuff
	//-----------------
	private String statusStr(int status, int retryCount) {
		switch (status){
		case NEW: 
			return "";
		case UPLOADING:
			return "p...";
		case RETRY: 
			return String.format("r%d...", retryCount);
		case UPLOADED:
			return ".s..";
		case DOWNLOADED:
			return "..p.";
		case SENT_VIEWED:
			return "v!";
		}
		return "";
	}

	public String getStatusStr(Friend friend){
		Log.i(TAG, "getStatusStr: ");
		Bundle b = getSentVideoStatus(friend);
		Integer status = b.getInt(STATUS_KEY);
		String ss = statusStr( status, b.getInt(RETRY_COUNT_KEY) );
		return getFirstName(friend, status) + " " + ss;
	}

	public String getFirstName(Friend friend, Integer status){
		String fn = friend.get("firstName");
		int shortLen = Math.min(7, fn.length());
		String shortFn = fn.substring(0, shortLen);
		String r = shortFn;
		if (status == null || status == SENT_VIEWED  || status == NEW){
			r = fn;
		}
		return r;
	}
}
