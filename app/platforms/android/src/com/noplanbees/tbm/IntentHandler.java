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
	private RemoteStorageSetter remoteStorageSetter;
	private int status;

	public IntentHandler(HomeActivity a, Intent i){
		// Convenience.printBundle(i.getExtras());
		homeActivity = a;
		intent = i;
		friend = FriendFactory.getFactoryInstance().getFriendFromIntent(intent);
		transferType = intent.getStringExtra(FileTransferService.IntentFields.TRANSFER_TYPE_KEY);
		videoId = intent.getStringExtra(FileTransferService.IntentFields.VIDEO_ID_KEY);
		status = intent.getIntExtra(FileTransferService.IntentFields.STATUS_KEY, -1);
		remoteStorageSetter = new RemoteStorageSetter();
		Log.e(TAG, status + "");
	}
	
	private class RemoteStorageSetter extends RemoteStorageHandler{
		public RemoteStorageSetter() {
			super();
		}
		@Override
		public void success(LinkedTreeMap<String, String>data) {			
		}
		@Override
		public void error(LinkedTreeMap<String, String>data) {			
		}
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
	}
	
	//---------------------
	// Handle upload intent 
	//---------------------
	private void handleUploadIntent() {
		Log.i(TAG, "handleUploadIntent");
		friend.updateStatus(intent);
		if (status == Friend.OutgoingVideoStatus.UPLOADED){
            // Set remote videoIdKV	
			remoteStorageSetter.setRemoteOutgoingVideoId(friend, friend.get(Friend.Attributes.OUTGOING_VIDEO_ID));
			
			// Send outgoing notification
			NotificationHandler.sendForVideoReceived(friend);
		}
	}
	

	//-------------------------
	// Handle Download Intent
	//-------------------------
	private void handleDownloadIntent(){
		Log.i(TAG, "handleDownloadIntent");
		
		if (VideoIdUtils.isOlderThanLastIncomingVideo(friend, videoId)){
			Log.e(TAG, "handleDownloadIntent: Ignoring intent for video id that is older than the current incoming video.");
			return;
		}
		
		friend.updateStatus(intent);
		if (status == Friend.IncomingVideoStatus.NEW){
			// Do not rely on VideoId we got notification as definitive as it may be stale check the remote store and use that if it is newer.
			new GetRemoteVideoId().getRemoteKV(RemoteStorageHandler.incomingVideoIdRemoteKVKey(friend));
		}
		
		if (status == Friend.IncomingVideoStatus.DOWNLOADED){
			friend.createThumb();
			if (!homeActivity.isForeground || screenIsLockedOrOff()){
				NotificationAlertManager.alert(homeActivity, friend);
			} else {
				homeActivity.getVideoPlayerForFriend(friend).refreshThumb();
				playNotificationTone();
			}
		}
		
		// Update RemoteStorage status as downloaded.
		remoteStorageSetter.setRemoteIncomingVideoStatus(friend, videoId, RemoteStorageHandler.STATUS_ENUM.DOWNLOADED);
		
		// Send outgoing notification
		NotificationHandler.sendForVideoStatusUpdate(friend, NotificationHandler.StatusEnum.DOWNLOADED);
	}
	
	private class GetRemoteVideoId extends RemoteStorageHandler{
		public GetRemoteVideoId() {
			super();
		}
		@Override
		public void success(LinkedTreeMap<String, String>data) {
			gotRemoteVideoId(data);
		}
		@Override
		public void error(LinkedTreeMap<String, String>data) {
			Log.e(TAG, "GetRemoteVideoId: Error failed to get remote videoId returning 0");
			data.put(RemoteStorageHandler.DataKeys.VIDEO_ID_KEY, "0");
			gotRemoteVideoId(data);
		}
	}
	
	private void gotRemoteVideoId(LinkedTreeMap<String, String> data) {
		String remoteVideoId = data.get(RemoteStorageHandler.DataKeys.VIDEO_ID_KEY);
		String newerVideoId = VideoIdUtils.newerVideoId(videoId, remoteVideoId);
		intent.putExtra(FileTransferService.IntentFields.VIDEO_ID_KEY, newerVideoId);
		Log.e(TAG, "calling download for videoId=" + intent.getStringExtra(FileTransferService.IntentFields.VIDEO_ID_KEY));
		friend.downloadVideo(intent);
	}
	
	private void playNotificationTone(){
		Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		Ringtone r = RingtoneManager.getRingtone(homeActivity.getApplicationContext(), notification);
		r.play();
	}
	

}
