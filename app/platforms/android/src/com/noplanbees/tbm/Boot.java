package com.noplanbees.tbm;

import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class Boot {
	public static final String TAG = "Boot";
	
	public static boolean boot(Context context){
		Log.i(TAG, "boot");
		//--------------------------
		// Check registration
		//--------------------------
		if (!User.isRegistered(context)) {
			Log.i(TAG, "Not registered. Starting RegisterActivty");
			Intent i = new Intent(context, RegisterActivity.class);
			context.startActivity(i);
			Log.i(TAG, "Exiting boot");
			return false;
		}
		return true;
	}
	
	//--------------------------
	// Init GCM
	//--------------------------
	public static boolean initGCM(HomeActivity homeActivity){
		homeActivity.gcmHandler = new GcmHandler(homeActivity);
		if (homeActivity.gcmHandler.checkPlayServices()){
			homeActivity.gcmHandler.registerGcm();
		} else {
			Log.e(TAG, "No valid Google Play Services APK found.");
			return false;
		}
		return true;
	}

}
