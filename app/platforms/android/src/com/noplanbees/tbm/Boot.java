package com.noplanbees.tbm;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

public class Boot {
	public static final String TAG = "Boot";
	
	public static boolean boot(Activity activity){
		//--------------------------
		// Retrieve or create User model from local storage
		//--------------------------
		ActiveModelsHandler.ensureUser();

		
		//--------------------------
		// Check registration
		//--------------------------
		if (!User.isRegistered()) {
			Log.i(TAG, "Not registered. Starting RegisterActivty");
			Intent i = new Intent(activity, RegisterActivity.class);
			activity.startActivity(i);
			Log.i(TAG, "Exiting boot");
			return false;
		}
		
		//--------------------------
        // Try to retrieve Friend model from local storage
		//--------------------------
		ActiveModelsHandler.ensureFriend();

		
		//--------------------------
		// Init GCM
		//--------------------------
		GcmHandler gcmHandler = new GcmHandler(activity);
		if (gcmHandler.checkPlayServices()){
			gcmHandler.registerGcm();
		} else {
			Log.e(TAG, "No valid Google Play Services APK found.");
			return false;
		}
		return true;
	}

}
