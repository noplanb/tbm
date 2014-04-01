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
		// Pull our models from storage so we can use them in handling the notification
		ActiveModelsHandler.ensureAll();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		// The getMessageType() intent parameter must be the intent you received
		// in your BroadcastReceiver.
		String messageType = gcm.getMessageType(intent);

		if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
			/*
			 * Filter messages based on message type. Since it is likely that GCM
			 * will be extended in the future with new message types, just ignore
			 * any message types you're not interested in, or that you don't
			 * recognize.
			 */
			if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
				//Not used
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
				//Not used
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
				Log.i(TAG, "onHandleIntent: extras = " + extras.toString());

				if ( extras.getString("type").equalsIgnoreCase("video_recieved") ){
					handleVideoReceived(intent);	
				} else if ( extras.getString("type").equalsIgnoreCase("video_status_update") ){
					handleVideoStatusUpdate(intent);
				}
			}
		}
		// Release the wake lock provided by the WakefulBroadcastReceiver.
		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}

	//---------
	// Handle video status update
	//---------
	private void handleVideoStatusUpdate(Intent intent) {
		String status = intent.getExtras().getString("status");
		if ( status.equalsIgnoreCase("downloaded")){
			intent.putExtra(VideoStatusHandler.STATUS_KEY, VideoStatusHandler.DOWNLOADED);
		} else if ( status.equalsIgnoreCase("viewed") ){
			intent.putExtra(VideoStatusHandler.STATUS_KEY, VideoStatusHandler.SENT_VIEWED);
		} else {
			Log.e(TAG, "handleVideoStatusUpdate: ERROR got unknow sent video status");
		}
		// VidoeStatusHandler is responsible for forwarding intent to homeActivity in this case.
		new VideoStatusHandler(getApplicationContext()).updateSentVideoStatus(intent);
	}
	
	//--------
	// Handling video received
	//---------
	private void handleVideoReceived(Intent intent) {
		Friend friend = ActiveModelsHandler.ensureFriend().getFriendFromIntent(intent);
		friend.downloadVideo();
		sendHomeActivityVideoRecievedIntent(friend);
	}

	private void sendHomeActivityVideoRecievedIntent(Friend friend){
		Log.i(TAG, "sendNotification: Sending intent to start home activity");
		Intent i = new Intent(this, HomeActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Bundle extras = new Bundle();
		extras.putInt(IntentHandler.INTENT_TYPE_KEY, IntentHandler.TYPE_VIDEO_RECEIVED);
		extras.putString("friendId", friend.getId());
		i.putExtras(extras);
		startActivity(i);
	}

}

