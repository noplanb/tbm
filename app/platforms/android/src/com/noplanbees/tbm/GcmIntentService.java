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
                String friendId = extras.getString("from_id");
                FriendFactory ff = FriendFactory.getFactoryInstance();
                Friend friend = (Friend) ff.find(friendId);
                friend.downloadVideo();
                ActiveModelsHandler.saveFriend();  
                sendHomeActivityIntent(friend);
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void sendHomeActivityIntent(Friend friend){
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

