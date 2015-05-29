package com.zazoapp.client.notification.gcm;


import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.zazoapp.client.Config;
import com.zazoapp.client.core.IntentHandlerService;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.IncomingVideo;
import com.zazoapp.client.network.FileTransferService;
import com.zazoapp.client.notification.NotificationHandler;

/**
 * This IntentService handles push notifications. It is called by
 * GcmBroadcastReciever.
 */
public class GcmIntentService extends IntentService {
	private final String TAG = "GCM " + this.getClass().getSimpleName();

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
                if (extras.containsKey(NotificationHandler.DataKeys.SERVER_HOST)) {
                    // filter messages addressed to another server
                    if (Config.getServerHost().equalsIgnoreCase(extras.getString(NotificationHandler.DataKeys.SERVER_HOST))) {
                        if (extras.getString(NotificationHandler.DataKeys.TYPE).equalsIgnoreCase(NotificationHandler.TypeEnum.VIDEO_RECEIVED)) {
                            handleVideoReceived(intent);
                        } else if (extras.getString(NotificationHandler.DataKeys.TYPE).equalsIgnoreCase(NotificationHandler.TypeEnum.VIDEO_STATUS_UPDATE)) {
                            handleVideoStatusUpdate(intent);
                        } else {
                            Dispatch.dispatch("onHandleIntent: ERROR: unknown intent type in notification payload.");
                        }
                    } else {
                        Log.i(TAG, "GCM: Message is addressed to another server.\n" + extras.toString());
                    }
                } else {
                    Dispatch.dispatch("GCM: No host in message.\n" + extras.toString());
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
		String videoId = intent.getStringExtra(NotificationHandler.DataKeys.VIDEO_ID);
		intent.putExtra(FileTransferService.IntentFields.TRANSFER_TYPE_KEY, FileTransferService.IntentFields.TRANSFER_TYPE_UPLOAD);
		
		// Normalize from notification naming convention to internal.
		intent.putExtra(FileTransferService.IntentFields.VIDEO_ID_KEY, videoId);
		if (status.equalsIgnoreCase(NotificationHandler.StatusEnum.DOWNLOADED)) {
			intent.putExtra(FileTransferService.IntentFields.STATUS_KEY, Friend.OutgoingVideoStatus.DOWNLOADED);
		} else if (status.equalsIgnoreCase(NotificationHandler.StatusEnum.VIEWED)) {
			intent.putExtra(FileTransferService.IntentFields.STATUS_KEY, Friend.OutgoingVideoStatus.VIEWED);
		} else {
			Dispatch.dispatch("handleVideoStatusUpdate: ERROR got unknow sent video status");
		}
		startDataHolderService(intent);
	}

	// --------
	// Handling video received
	// ---------
	private void handleVideoReceived(Intent intent) {
		Log.i(TAG, "handleVideoReceived:");
		// Normalize from notification naming convention to internal.
		intent.putExtra(FileTransferService.IntentFields.TRANSFER_TYPE_KEY, FileTransferService.IntentFields.TRANSFER_TYPE_DOWNLOAD);
		intent.putExtra(FileTransferService.IntentFields.STATUS_KEY, IncomingVideo.IncomingVideoStatus.NEW);
		intent.putExtra(FileTransferService.IntentFields.VIDEO_ID_KEY, intent.getStringExtra(NotificationHandler.DataKeys.VIDEO_ID)); 
		startDataHolderService(intent);
	}

	private void startDataHolderService(Intent intent) {
		intent.setClass(getApplicationContext(), IntentHandlerService.class);
		startService(intent);
	}

}
