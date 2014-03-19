package com.noplanbees.tbm;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

public class Boot {
	public static final String TAG = "Boot";
	
	public static boolean boot(Activity activity){
		//--------------------------
		// Retrieve or create Config model from local storage
		//--------------------------
		UserFactory cf = UserFactory.getFactoryInstance();
		if (cf.hasInstances()){
			Log.i(TAG, "Config present in memory.");
		} else if (cf.retrieve()){
			Log.i(TAG, "Retrieved Config from local storage.");
		} else {
			Log.i(TAG, "Config not retrievable from local storage. Creating Conf instance.");
			cf.makeInstance();
		}
		
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
		FriendFactory ff = FriendFactory.getFactoryInstance();
		if (ff.hasInstances()){
			Log.i(TAG, "Friend present in memory");
		} else if (ff.retrieve()){
			Log.i(TAG, "Retrieved Friend from local storage.");
		} else {
			Log.i(TAG, "Friend not retrievable from local storage.");
		}
		
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
