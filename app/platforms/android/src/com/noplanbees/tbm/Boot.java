package com.noplanbees.tbm;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

public class Boot {
	public static final String TAG = "Boot";
	
	public static boolean boot(HomeActivity homeActivity){
		Log.i(TAG, "boot");
		
		//--------------------------
		// Retrieve or create User model from local storage
		//--------------------------
		ActiveModelsHandler.ensureUser();

		
		//--------------------------
		// Check registration
		//--------------------------
		if (!User.isRegistered()) {
			Log.i(TAG, "Not registered. Starting RegisterActivty");
			Intent i = new Intent(homeActivity, RegisterActivity.class);
			homeActivity.startActivity(i);
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
