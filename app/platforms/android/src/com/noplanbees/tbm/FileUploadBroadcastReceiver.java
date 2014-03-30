package com.noplanbees.tbm;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class FileUploadBroadcastReceiver extends WakefulBroadcastReceiver{
	private final String TAG = "FUBR" + this.getClass().getSimpleName();


	@Override
	public void onReceive(Context context, Intent intent) {
    	Log.i(TAG, "FileUploadBroadcastReceiver: onReceive() got a message. Forwarding to FileUploadService.");
        ComponentName comp = new ComponentName(context.getPackageName(), FileUploadService.class.getName());
        // Start the service, keeping the device awake while it is launching.
        startWakefulService(context, (intent.setComponent(comp)));
	}
}
