package com.noplanbees.tbm;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;

import com.google.gson.internal.LinkedTreeMap;

public class IntentHandler {
	private final String TAG = this.getClass().getSimpleName();
	private final static String STAG = IntentHandler.class.getSimpleName();
	
	public static final int RESULT_RUN_IN_BACKGROUND = 0;
	public static final int RESULT_RUN_IN_FOREGROUND = 1;
	public static final int RESULT_FINISH = 2; // not used

	private HomeActivity homeActivity;
	private Intent intent;
	private Friend friend;
	private String transferType;
	private String videoId;
	private RemoteStorageHandler rSHandler;
	private int status;

	public IntentHandler(HomeActivity a, Intent i){
		// Convenience.printBundle(i.getExtras());
		homeActivity = a;
		intent = i;
		friend = FriendFactory.getFactoryInstance().getFriendFromIntent(intent);
		transferType = intent.getStringExtra(FileTransferService.IntentFields.TRANSFER_TYPE_KEY);
		videoId = intent.getStringExtra(FileTransferService.IntentFields.VIDEO_ID_KEY);
		status = intent.getIntExtra(FileTransferService.IntentFields.STATUS_KEY, -1);
		rSHandler = new RemoteStorageHandler();
		Log.e(TAG, status + "");
	}
	
	public Integer handle(){
		Log.i(TAG, "handle:");
		printState();
		if (isDownloadIntent()){
			handleDownloadIntent();
		} else if (isUploadIntent()){
			handleUploadIntent();
		} else {
			handleUserLaunchIntent(homeActivity);
		}
		return getReturnResult();
	}

	private Integer getReturnResult() {
		if ( !isBackgroundIntent() ){
			return RESULT_RUN_IN_FOREGROUND;
		} else {
			if (homeActivity.isForeground){
				return RESULT_RUN_IN_FOREGROUND;
			} else {
				return RESULT_RUN_IN_BACKGROUND;
			}
		}
	}

	//------------
	// Convenience
	//------------
	private Boolean isUploadIntent() {
		if (transferType == null)
			return false;
		return  transferType.equals(FileTransferService.IntentFields.TRANSFER_TYPE_UPLOAD);
	}

	private Boolean isDownloadIntent() {
		if (transferType == null)
			return false;
		return transferType.equals(FileTransferService.IntentFields.TRANSFER_TYPE_DOWNLOAD);
	}
	
	private Boolean isBackgroundIntent() {
		return isUploadIntent() || isDownloadIntent();
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

	private void printState(){
		//Convenience.printRunningTaskInfo(homeActivity);
		Log.i(TAG,"isForeground=" + homeActivity.isForeground.toString());
		Log.e(TAG, "is Background intent=" + isBackgroundIntent().toString());
		Log.i(TAG,"screenIsOff=" + screenIsOff().toString());
		Log.i(TAG,"screenIsLocked=" + screenIsLocked().toString());
		Log.i(TAG,"numActivities=" + Convenience.numActivitiesInOurTask(homeActivity));
	}
	

	//--------------------------
	// Handle user launch intent 
	//--------------------------
	public static void handleUserLaunchIntent(HomeActivity homeActivity) {
		Log.i(STAG, "handleUserLaunchIntent");
		FileUploadService.restartTransfersPendingRetry(homeActivity);
		FileDownloadService.restartTransfersPendingRetry(homeActivity);
		NotificationAlertManager.cancelNativeAlerts(homeActivity);
		(new Poller(homeActivity)).pollAll();
	}
	
	//---------------------
	// Handle upload intent 
	//---------------------
	private void handleUploadIntent() {
		Log.i(TAG, "handleUploadIntent");
		friend.updateStatus(intent);
		if (status == Friend.OutgoingVideoStatus.UPLOADED){
            // Set remote videoIdKV	
			rSHandler.addRemoteOutgoingVideoId(friend, videoId);
			
			// Send outgoing notification
			NotificationHandler.sendForVideoReceived(friend, videoId);
		}
	}
	

	//-------------------------
	// Handle Download Intent
	//-------------------------
	private synchronized void handleDownloadIntent(){
		Log.i(TAG, "handleDownloadIntent");
		
		if (VideoIdUtils.isOlderThanOldestIncomingVideo(friend, videoId)){
			Log.w(TAG, "handleDownloadIntent: Ignoring download intent for video id that is older than the current incoming video.");
			rSHandler.deleteRemoteVideoIdAndFile(friend, videoId);
			return;
		}
		
		if (friend.hasIncomingVideoId(videoId) && status == Video.IncomingVideoStatus.NEW){
			Log.w(TAG, "handleDownloadIntent: Ignoring download intent for video id that that is currently in process.");
			return;
		}
		
		if (status == Video.IncomingVideoStatus.NEW){
			// Create the video
			friend.createIncomingVideo(homeActivity, videoId);	
			// Download only if we did not have this videoId before this intent.
			friend.downloadVideo(intent);
		}
		
		friend.updateStatus(intent);

		if (status == Video.IncomingVideoStatus.DOWNLOADED){
			friend.createThumb(videoId);

			if (!VideoPlayer.isPlaying(friend.getId()))
				friend.deleteAllViewedVideos();
			
			if (!homeActivity.isForeground || screenIsLockedOrOff()){
				NotificationAlertManager.alert(homeActivity, friend, videoId);
			} else {
				if (!VideoPlayer.isPlaying(friend.getId())){
					homeActivity.getVideoPlayerForFriend(friend).refreshThumb();
					playNotificationTone();
				}
			}
			
			rSHandler.deleteRemoteVideoIdAndFile(friend, videoId);
			// Update RemoteStorage status as downloaded.
			rSHandler.setRemoteIncomingVideoStatus(friend, videoId, RemoteStorageHandler.StatusEnum.DOWNLOADED);
			
			// Send outgoing notification
			NotificationHandler.sendForVideoStatusUpdate(friend, videoId, NotificationHandler.StatusEnum.DOWNLOADED);
		}
	}
	
	
	private void playNotificationTone(){
		Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		Ringtone r = RingtoneManager.getRingtone(homeActivity.getApplicationContext(), notification);
		r.play();
	}
	

}
