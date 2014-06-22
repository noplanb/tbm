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

	public static final int RESULT_RUN_IN_BACKGROUND = 0;
	public static final int RESULT_RUN_IN_FOREGROUND = 1;
	public static final int RESULT_FINISH = 2; // not used

	private HomeActivity homeActivity;
	private Intent intent;
	private Friend friend;
	private String transferType;
	private int status;

	public IntentHandler(HomeActivity a, Intent i){
		// Convenience.printBundle(i.getExtras());
		homeActivity = a;
		intent = i;
		friend = FriendFactory.getFactoryInstance().getFriendFromIntent(intent);
		transferType = intent.getStringExtra(FileTransferService.IntentFields.TRANSFER_TYPE_KEY);
		status = intent.getIntExtra(FileTransferService.IntentFields.STATUS_KEY, -1);
		Log.e(TAG, status + "");
	}
	
	public Integer handle(Boolean isOncreate){
		Log.i(TAG, "handle: isOncreate = " + isOncreate.toString());
		if (isDownloadIntent()){
			handleDownloadIntent(isOncreate);
			return getResultForDownloadIntent(isOncreate);
		} else if (isUploadIntent()){
			handleUploadIntent();
			return getResultForUploadIntent(isOncreate);
		} else {
			Log.i(TAG, "handle: no intent type ");
			return RESULT_RUN_IN_FOREGROUND;
		}
	}
	
	//------------
	// Convenience
	//------------
	private boolean isUploadIntent() {
		if (transferType == null)
			return false;
		return  transferType.equals(FileTransferService.IntentFields.TRANSFER_TYPE_UPLOAD);
	}

	private boolean isDownloadIntent() {
		if (transferType == null)
			return false;
		return transferType.equals(FileTransferService.IntentFields.TRANSFER_TYPE_DOWNLOAD);
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

	private void printState(Boolean isOncreate){
		//Convenience.printRunningTaskInfo(homeActivity);
		Log.i(TAG,"isOncreate=" + isOncreate.toString());
		Log.i(TAG,"isForeground=" + homeActivity.isForeground.toString());
		Log.i(TAG,"screenIsOff=" + screenIsOff().toString());
		Log.i(TAG,"screenIsLocked=" + screenIsLocked().toString());
		Log.i(TAG,"numActivities=" + Convenience.numActivitiesInOurTask(homeActivity));
	}
	
	//---------------------
	// Handle upload intent 
	//---------------------
	private void handleUploadIntent() {
		Log.i(TAG, "handleUploadIntent");
		friend.updateStatus(intent);
	}
	
	private int getResultForUploadIntent(Boolean isOncreate){
		Log.i(TAG, "getResultForUploadIntent");
		 printState(isOncreate);
		if (isOncreate || !homeActivity.isForeground || screenIsLockedOrOff()){
			return RESULT_RUN_IN_BACKGROUND;
		} else {
			return RESULT_RUN_IN_FOREGROUND;
		}
	}

	//-------------------------
	// Handle Download Intent
	//-------------------------
	private void handleDownloadIntent(Boolean isOncreate){
		friend.updateStatus(intent);
		if (status == Friend.IncomingVideoStatus.NEW){
			friend.downloadVideo(intent);
		}
		
		if (status == Friend.IncomingVideoStatus.DOWNLOADED){
			friend.createThumb();
			if (isOncreate || !homeActivity.isForeground || screenIsLockedOrOff()){
				NotificationAlertManager.alert(homeActivity, friend);
			} else {
				homeActivity.getVideoPlayerForFriend(friend).refreshThumb();
				playNotificationTone();
			}
		}
	}
	
	private int getResultForDownloadIntent(Boolean isOncreate) {
		Log.i(TAG, "getResultForDownloadIntent");
		printState(isOncreate);
		if (isOncreate || !homeActivity.isForeground || screenIsLockedOrOff()){
			return RESULT_RUN_IN_BACKGROUND;
		} else {
			return RESULT_RUN_IN_FOREGROUND;
		}
	}

	private void playNotificationTone(){
		Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		Ringtone r = RingtoneManager.getRingtone(homeActivity.getApplicationContext(), notification);
		r.play();
	}
	

}
