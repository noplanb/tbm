package com.noplanbees.tbm;

import android.content.Intent;
import android.util.Log;

public class Boot {
	public static final String TAG = "Boot";
	
	public static boolean boot(HomeActivity homeActivity){
		Log.i(TAG, "boot");
		
		//--------------------------
		// Retrieve or create User model from local storage
		//--------------------------
		ActiveModelsHandler.ensureUser(homeActivity);

		
		//--------------------------
		// Check registration
		//--------------------------
		if (!User.isRegistered(homeActivity)) {
			Log.i(TAG, "Not registered. Starting RegisterActivty");
			Intent i = new Intent(homeActivity, RegisterActivity.class);
			homeActivity.startActivity(i);
			Log.i(TAG, "Exiting boot");
			return false;
		}
		
		//--------------------------
        // Try to retrieve all models from local storage
		//--------------------------
		ActiveModelsHandler.ensureAll(homeActivity);

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
