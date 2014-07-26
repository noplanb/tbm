package com.noplanbees.tbm;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * This IntentService handles push notifications. It is called by
 * GcmBroadcastReciever.
 */
public class GcmIntentService extends IntentService {
	private final String TAG = "GCM " + this.getClass().getSimpleName();
	NotificationCompat.Builder builder;

	public GcmIntentService() {
		super("GcmIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		// The getMessageType() intent parameter must be the intent you received
		// in your BroadcastReceiver.
		String messageType = gcm.getMessageType(intent);

		if (!extras.isEmpty()) { // has effect of unparcelling Bundle
			/*
			 * Filter messages based on message type. Since it is likely that
			 * GCM will be extended in the future with new message types, just
			 * ignore any message types you're not interested in, or that you
			 * don't recognize.
			 */
			if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
				// Not used
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
				// Not used
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
				Log.i(TAG, "onHandleIntent: extras = " + extras.toString());
				if (extras.getString("type").equalsIgnoreCase("video_received")) {
					handleVideoReceived(intent);
				} else if (extras.getString("type").equalsIgnoreCase(
						"video_status_update")) {
					handleVideoStatusUpdate(intent);
				}
			}
		}
		// Release the wake lock provided by the WakefulBroadcastReceiver.
		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}

	// ---------
	// Handle video status update
	// ---------
	private void handleVideoStatusUpdate(Intent intent) {
		String status = intent.getStringExtra(NotificationHandler.DataKeys.STATUS);
		intent.putExtra(FileTransferService.IntentFields.TRANSFER_TYPE_KEY, FileTransferService.IntentFields.TRANSFER_TYPE_UPLOAD);
		
		// Normalize from notification naming convention to internal.
		if (status.equalsIgnoreCase(NotificationHandler.StatusEnum.DOWNLOADED)) {
			intent.putExtra(FileTransferService.IntentFields.STATUS_KEY, Friend.OutgoingVideoStatus.DOWNLOADED);
		} else if (status.equalsIgnoreCase(NotificationHandler.StatusEnum.VIEWED)) {
			intent.putExtra(FileTransferService.IntentFields.STATUS_KEY, Friend.OutgoingVideoStatus.VIEWED);
		} else {
			Log.e(TAG, "handleVideoStatusUpdate: ERROR got unknow sent video status");
		}
		startHomeActivity(intent);
	}

	// --------
	// Handling video received
	// ---------
	private void handleVideoReceived(Intent intent) {
		Log.i(TAG, "handleVideoReceived:");
		intent.putExtra(FileTransferService.IntentFields.TRANSFER_TYPE_KEY, FileTransferService.IntentFields.TRANSFER_TYPE_DOWNLOAD);
		intent.putExtra(FileTransferService.IntentFields.STATUS_KEY, Friend.IncomingVideoStatus.NEW);
		// Normalize from notification naming convention to internal.
		intent.putExtra(FileTransferService.IntentFields.VIDEO_ID_KEY, intent.getStringExtra(NotificationHandler.DataKeys.VIDEO_ID)); 
		startHomeActivity(intent);
	}

	private void startHomeActivity(Intent intent) {
		intent.setClass(getApplicationContext(), HomeActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_FROM_BACKGROUND);
		intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS); // See doc/task_manager_bug.txt for the reason for this flag.
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); // This is probably not necessary since HomeActivity is singleTask but on a test bed I needed it to make sure onNewIntent is called in the activity.
		startActivity(intent);
	}

}
