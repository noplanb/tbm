package com.noplanbees.tbm;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;

import com.noplanbees.tbm.network.FriendGetter;
import com.noplanbees.tbm.network.FriendGetter.FriendGetterCallback;
import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.FriendFactory;
import com.noplanbees.tbm.model.Video;
import com.noplanbees.tbm.network.FileTransferService;
import com.noplanbees.tbm.notification.NotificationAlertManager;
import com.noplanbees.tbm.notification.NotificationHandler;

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

	private final String TAG = this.getClass().getSimpleName();
	private final static String STAG = IntentHandler.class.getSimpleName();

	private Context context;
	private Intent intent;
	private Friend friend;
	private String transferType;
	private String videoId;
	private RemoteStorageHandler rSHandler;
	private int status;

	public IntentHandler(Context context, Intent i) {
		// Convenience.printBundle(i.getExtras());
		this.context = context;
		intent = i;
		friend = FriendFactory.getFactoryInstance().getFriendFromIntent(intent);
		transferType = intent.getStringExtra(FileTransferService.IntentFields.TRANSFER_TYPE_KEY);
		videoId = intent.getStringExtra(FileTransferService.IntentFields.VIDEO_ID_KEY);
		status = intent.getIntExtra(FileTransferService.IntentFields.STATUS_KEY, -1);
		rSHandler = new RemoteStorageHandler();
		Log.i(TAG, status + "");
	}

	public Integer handle() {
		Log.i(TAG, "handle:");
		if (isDownloadIntent()) {
			handleDownloadIntent();
		} else if (isUploadIntent()) {
			handleUploadIntent();
		}
		return getReturnResult();
	}

	private Integer getReturnResult() {
		return null;
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

	private Boolean isBackgroundIntent() {
		return isUploadIntent() || isDownloadIntent();
	}

	private Boolean screenIsOff() {
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		return !pm.isScreenOn();
	}

	private Boolean screenIsLocked() {
		KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
		return (Boolean) km.inKeyguardRestrictedInputMode();
	}

	private Boolean screenIsLockedOrOff() {
		return screenIsLocked() || screenIsOff();
	}

    // ---------------------
	// Handle upload intent
	// ---------------------
	private void handleUploadIntent() {
		Log.i(TAG, "handleUploadIntent");
		friend.updateStatus(intent);
		if (status == Friend.OutgoingVideoStatus.UPLOADED) {
			// Set remote videoIdKV
			rSHandler.addRemoteOutgoingVideoId(friend, videoId);

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
			new FriendGetter(context, false, null);
			return;
		}else{
            friend.setLastActionTime(System.currentTimeMillis());
        }

        if (friend.hasIncomingVideoId(videoId) && status == Video.IncomingVideoStatus.NEW) {
			Log.w(TAG, "handleDownloadIntent: Ignoring download intent for video id that that is currently in process.");
			return;
		}

		if (status == Video.IncomingVideoStatus.NEW) {
            // If we have gotten a video from a friend then he has the app.
            friend.setHasApp();
			// Create the video
			friend.createIncomingVideo(context, videoId);
			// Download only if we did not have this videoId before this intent.
			friend.downloadVideo(intent.getStringExtra(FileTransferService.IntentFields.VIDEO_ID_KEY));
		}

		friend.updateStatus(intent);

		if (status == Video.IncomingVideoStatus.DOWNLOADED) {
			friend.createThumb(videoId);

			friend.deleteAllViewedVideos();

			if (!TbmApplication.getInstance().isForeground() || screenIsLockedOrOff()) {
				NotificationAlertManager.alert(context, friend, videoId);
			} else {
				playNotificationTone();
			}

			friend.deleteRemoteVideo(videoId);
			// Update RemoteStorage status as downloaded.
			rSHandler.setRemoteIncomingVideoStatus(friend, videoId, RemoteStorageHandler.StatusEnum.DOWNLOADED);

			// Send outgoing notification
			NotificationHandler.sendForVideoStatusUpdate(friend, videoId, NotificationHandler.StatusEnum.DOWNLOADED);
		}
	}

	private void playNotificationTone() {
		Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		Ringtone r = RingtoneManager.getRingtone(context.getApplicationContext(), notification);
		r.play();
	}

}
