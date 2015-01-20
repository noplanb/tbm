package com.noplanbees.tbm.gcm;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

/**
 * 
 * We declare this receiver in the manifest to handle RECEIVE intents. In this way when a message is directed at us
 * onRecieve() in this receiver is triggered and calls our intent service. The WakefulBroadcastReceiver class 
 * makes sure that the device is kept awake while the intentservice it calls handles the message.
 *
 */
public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {
	private final String TAG = "GCM " + this.getClass().getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
    	Log.i(TAG, "GcmBroadcastReceiver: onReceive() got a message. Forwarding to GcmIntentService.");
        // Explicitly specify that GcmIntentService will handle the intent.
        ComponentName comp = new ComponentName(context.getPackageName(), GcmIntentService.class.getName());
        // Start the service, keeping the device awake while it is launching.
        startWakefulService(context, (intent.setComponent(comp)));
    }
}
