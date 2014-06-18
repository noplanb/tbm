package com.noplanbees.tbm;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.widget.TextView;

public class IntentHandler {
	private final String TAG = this.getClass().getSimpleName();

	public static final int TYPE_VIDEO_RECEIVED = 0;
	public static final int TYPE_VIDEO_STATUS_UPDATE = 1;

	public static final String INTENT_TYPE_KEY = "type";

	public static final int RESULT_RUN_IN_BACKGROUND = 0;
	public static final int RESULT_RUN_IN_FOREGROUND = 1;
	public static final int RESULT_FINISH = 2; // not used



	private HomeActivity homeActivity;
	private Intent intent;
	private Bundle extras;
	private Friend friend;

	public IntentHandler(HomeActivity a, Intent i){
		homeActivity = a;
		intent = i;
		extras = intent.getExtras();
		friend = FriendFactory.getFactoryInstance().getFriendFromIntent(intent);
	}

	public Integer handle(Boolean isOncreate){
		Log.i(TAG, "handle: isOncreate = " + isOncreate.toString());
		Integer r = RESULT_RUN_IN_FOREGROUND;
		if (typeIsVideoReceived()){
			r = handleVideoReceived(isOncreate);
		} else if (typeIsVideoStatusUpdate()){
			r = handleVideoStatusUpdate(isOncreate);
		} else {
			Log.i(TAG, "handle: no intent type ");
			r = RESULT_RUN_IN_FOREGROUND;
		}
		return r;
	}

	//-----------------------
	// VideoStausUpdate stuff
	//-----------------------
	private int handleVideoStatusUpdate(Boolean isOncreate) {
		Log.i(TAG, "handleVideoStatusUpdate");
		// printState(isOncreate);
		if (isOncreate || !homeActivity.isForeground || screenIsLockedOrOff()){
			return RESULT_RUN_IN_BACKGROUND;
		} 
		if (homeActivity.isForeground){
			updateHomeViewSentVideoStatus(friend);
		} 
		return RESULT_RUN_IN_FOREGROUND;
	}

	public void updateHomeViewSentVideoStatus(Friend friend) {
		if (friend != null){
			Log.i(TAG, "updateHomeViewSentVideoStatus: friend = " + friend.get("firstName"));
			Integer nameTextId = Integer.parseInt(friend.get("nameTextId"));
			TextView nameText = (TextView) homeActivity.findViewById(nameTextId);
			nameText.setText(new VideoStatusHandler(homeActivity).getStatusStr(friend));
			nameText.invalidate();
		} else {
			Log.e(TAG, "updateHomeViewSentVideoStatus: Error friend was null");
		}
	}

	//-----------------------
	// VideoRecievedStuff stuff
	//-----------------------
	private Integer handleVideoReceived(Boolean isOncreate) {
		Log.i(TAG, "handleVideoReceived");
		// printState(isOncreate);
		Integer r = null;
		if (isOncreate || !homeActivity.isForeground || screenIsLockedOrOff()){
			NotificationAlertManager.alert(homeActivity, friend);
			r = RESULT_RUN_IN_BACKGROUND;
		} else {
			updateHomeViewReceivedVideo();
			r = RESULT_RUN_IN_FOREGROUND;
		}
		return r;
	}
	
	private Boolean screenIsOff(){
		PowerManager pm = (PowerManager) homeActivity.getSystemService(Context.POWER_SERVICE);
		return !pm.isScreenOn();
	}
	
	private Boolean screenIsLocked(){
		KeyguardManager km = (KeyguardManager) homeActivity.getSystemService(Context.KEYGUARD_SERVICE);
		return (Boolean) km.inKeyguardRestrictedInputMode();
	}
	
	private Boolean screenIsLockedOrOff(){
		return screenIsLocked() || screenIsOff();
	}

	private void updateHomeViewReceivedVideo() {
		Log.i(TAG, "updateHomeViewReceivedVideo");
		if (friend != null){
			homeActivity.getVideoPlayerForFriend(friend).refreshThumb();
			playNotificationTone();
			updateHomeViewSentVideoStatus(friend);
		}	
	}

	private boolean typeIsVideoStatusUpdate() {
		return  type() != null && type() == TYPE_VIDEO_STATUS_UPDATE;
	}

	private Integer type(){
		Integer t = null;
		if (extras != null){
			t = extras.getInt(INTENT_TYPE_KEY);
		}
		return t; 
	}

	private boolean typeIsVideoReceived() {
		return type() != null && type() == TYPE_VIDEO_RECEIVED;
	}

	private void playNotificationTone(){
		Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		Ringtone r = RingtoneManager.getRingtone(homeActivity.getApplicationContext(), notification);
		r.play();
	}
	
	private void printState(Boolean isOncreate){
		Convenience.printRunningTaskInfo(homeActivity);
		Log.i(TAG,"isOncreate=" + isOncreate.toString());
		Log.i(TAG,"isForeground=" + homeActivity.isForeground.toString());
		Log.i(TAG,"screenIsOff=" + screenIsOff().toString());
		Log.i(TAG,"screenIsLocked=" + screenIsLocked().toString());
		Log.i(TAG,"numActivities=" + Convenience.numActivitiesInOurTask(homeActivity));
	}
}
