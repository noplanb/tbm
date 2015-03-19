package com.zazoapp.client;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.Video;
import com.zazoapp.client.network.FileTransferService;
import com.zazoapp.client.network.HttpRequest;
import com.zazoapp.client.notification.NotificationAlertManager;
import com.zazoapp.client.notification.NotificationHandler;
import com.zazoapp.client.utilities.Convenience;

public class IntentHandler {

	public class IntentParamKeys {
		public static final String FRIEND_ID = "friendId";
		public static final String ACTION = "action";
	}

	public class IntentActions {
		public static final String NONE = "none";
		public static final String PLAY_VIDEO = "playVideo";
		public static final String SMS_RESULT = "smsResult";
	}

	private final static String TAG = IntentHandler.class.getSimpleName();

	private Context context;
	private Intent intent;
	private Friend friend;
	private String transferType;
	private String videoId;
	private int status;
	private int retryCount;

	public IntentHandler(Context context, Intent i) {
		// Convenience.printBundle(i.getExtras());
		this.context = context;
		intent = i;
		friend = FriendFactory.getFactoryInstance().getFriendFromIntent(intent);
		transferType = intent.getStringExtra(FileTransferService.IntentFields.TRANSFER_TYPE_KEY);
		videoId = intent.getStringExtra(FileTransferService.IntentFields.VIDEO_ID_KEY);
		status = intent.getIntExtra(FileTransferService.IntentFields.STATUS_KEY, -1);
        retryCount = intent.getIntExtra(FileTransferService.IntentFields.RETRY_COUNT_KEY, 0);
		Log.i(TAG, "status:" + status + " retry:" + intent.getIntExtra(FileTransferService.IntentFields.RETRY_COUNT_KEY, 0));
	}

	public void handle() {
		Log.i(TAG, "handle:");
		// This should never happen except perhaps when debugging and notifications are coming in even though
		// the user is not registered.
		if (!User.isRegistered(context)){
			Log.i(TAG, "Got an intent but user was not registered. Not processing it.");
			return;
		}
		
		if (isDownloadIntent()) {
			handleDownloadIntent();
		} else if (isUploadIntent()) {
			handleUploadIntent();
		}
	}


	// ------------
	// Convenience
	// ------------
	private Boolean isUploadIntent() {
		if (transferType == null)
			return false;
		return transferType.equals(FileTransferService.IntentFields.TRANSFER_TYPE_UPLOAD);
	}

	private Boolean isDownloadIntent() {
		if (transferType == null)
			return false;
		return transferType.equals(FileTransferService.IntentFields.TRANSFER_TYPE_DOWNLOAD);
	}

    // ---------------------
	// Handle upload intent
	// ---------------------
	private void handleUploadIntent() {
		Log.i(TAG, "handleUploadIntent");
		updateStatus();
		if (status == Friend.OutgoingVideoStatus.UPLOADED) {
			// Set remote videoIdKV
			RemoteStorageHandler.addRemoteOutgoingVideoId(friend, videoId);

			// Send outgoing notification
			NotificationHandler.sendForVideoReceived(friend, videoId);
		}
	}

	// -------------------------
	// Handle Download Intent
	// -------------------------
	private synchronized void handleDownloadIntent() {
		Log.i(TAG, "handleDownloadIntent");

		// if (VideoIdUtils.isOlderThanOldestIncomingVideo(friend, videoId)){
		// Log.w(TAG,
		// "handleDownloadIntent: Ignoring download intent for video id that is older than the current incoming video.");
		// rSHandler.deleteRemoteVideoIdAndFile(friend, videoId);
		// return;
		// }

		// We may be getting a message from someone who is not a friend yet. Get
		// new friends and poll them all.
		if (friend == null) {
			Log.i(TAG, "Got Video from a user who is not currently a friend. Getting friends.");
			new SyncManager(context).getAndPollAllFriends();
			return;
		}
		
		friend.setLastActionTime(System.currentTimeMillis());
        friend.setHasApp();
        
        // Create and download the video if this was a videoReceived intent.
		if (status == Video.IncomingVideoStatus.NEW) {
		    
	        if (friend.hasIncomingVideoId(videoId)) {
	            Log.w(TAG, "handleDownloadIntent: Ignoring download intent for video id that that is currently in process.");
	            return;
	        }
	        
			friend.createIncomingVideo(context, videoId);
			friend.downloadVideo(videoId);
		}

		if (status == Video.IncomingVideoStatus.DOWNLOADED) {
			
			// Always delete the remote video even if the one we got is corrupted. Otherwise it may never be deleted
			deleteRemoteVideoAndKV();
			
			// Always set status for sender to downloaded and send status notification even if the video we got is not corrupted.
			RemoteStorageHandler.setRemoteIncomingVideoStatus(friend, videoId, RemoteStorageHandler.StatusEnum.DOWNLOADED);
			NotificationHandler.sendForVideoStatusUpdate(friend, videoId, NotificationHandler.StatusEnum.DOWNLOADED);
			
			if(!friend.createThumb(videoId)){
	           friend.setAndNotifyIncomingVideoStatus(videoId, Video.IncomingVideoStatus.FAILED_PERMANENTLY);
               return;
            }

			// TODO: create a new state for local videos called marked_for_remote_deletion.
			// do not show videos in that state during play
			// only after we have successfully deleted the remote_kv for the video do we 
			// actually delete the video object locally.
			friend.deleteAllViewedVideos();

			if (!TbmApplication.getInstance().isForeground() || Convenience.screenIsLockedOrOff(context)) {
				NotificationAlertManager.alert(context, friend, videoId);
			} else {
				// TODO: play the notification tone only if we are not currently playing or recording.
                NotificationAlertManager.playTone();
			}
		}
		
		if (status == Video.IncomingVideoStatus.FAILED_PERMANENTLY){
			Log.i(TAG, "deleteRemoteVideoAndKV for a video that failed permanently");
			deleteRemoteVideoAndKV(new HttpRequest.Callbacks() {
                @Override
                public void success(String response) {
                    friend.deleteVideo(videoId);
                }

                @Override
                public void error(String errorString) {}
            });
		}
		
		if (status == Video.IncomingVideoStatus.DOWNLOADING){
			// No need to do anything special in this case.
		}
		
		// Update the status and notify based on the intent if we have not exited for another reason above.
		// TODO: bring this method into this file
		updateStatus();
	}
	
	
	//--------
	// Helpers
	//--------
	private void deleteRemoteVideoAndKV(){
        deleteRemoteVideoAndKV(null);
	}

    private void deleteRemoteVideoAndKV(HttpRequest.Callbacks callbacks) {
        // Note it is ok if deleting the file fails as s3 will clean itself up after a few days.
        // Delete remote video.
        friend.deleteRemoteVideo(videoId);

        // Delete kv for video.
        RemoteStorageHandler.deleteRemoteIncomingVideoId(friend, videoId, callbacks);
    }

    public void updateStatus(){
        if (transferType.equals(FileTransferService.IntentFields.TRANSFER_TYPE_DOWNLOAD)){
            friend.setAndNotifyIncomingVideoStatus(videoId, status);
            friend.setAndNotifyDownloadRetryCount(videoId, retryCount);
        } else if (transferType.equals(FileTransferService.IntentFields.TRANSFER_TYPE_UPLOAD)){
            if ( videoId.equals(friend.getOutgoingVideoId()) ){
                friend.setAndNotifyOutgoingVideoStatus(status);
                friend.setAndNotifyUploadRetryCount(retryCount);
            }
        } else {
            Dispatch.dispatch("ERROR: updateStatus: unknown TransferType passed in intent. This should never happen.");
            throw new RuntimeException();
        }
    }

}
