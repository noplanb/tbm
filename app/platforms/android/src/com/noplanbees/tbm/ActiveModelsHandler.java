package com.noplanbees.tbm;

import android.util.Log;

public class ActiveModelsHandler {
	public static String TAG = "ActiveModelsHandler";

	public static void ensureAll(){
		ensureUser();
		ensureFriend();
	}
	
	public static void saveAll(){
		saveUser();
		saveFriend();
	}
	
	public static void ensureUser(){
		UserFactory uf = UserFactory.getFactoryInstance();
		if (uf.hasInstances()){
			Log.i(TAG, "User present in memory.");
		} else if (uf.retrieve()){
			Log.i(TAG, "Retrieved User from local storage.");
		} else {
			Log.i(TAG, "Config not retrievable from local storage. Creating Conf instance.");
			uf.makeInstance();
		}
	}
	
	public static void ensureFriend(){
		FriendFactory ff = FriendFactory.getFactoryInstance();
		if (ff.hasInstances()){
			Log.i(TAG, "Friend present in memory");
		} else if (ff.retrieve()){
			Log.i(TAG, "Retrieved Friend from local storage.");
		} else {
			Log.i(TAG, "Friend not retrievable from local storage.");
		}
	}
	
	public static void retrieveFriend(){
		FriendFactory ff = FriendFactory.getFactoryInstance();
        ff.retrieve();
	}
	
	public static void saveUser(){
		UserFactory uf = UserFactory.getFactoryInstance();
		if (uf.hasInstances()){
			Log.i(TAG, "Saving User to local storage");
			uf.save();
		} else {
			Log.i(TAG, "Not Saving User. No instance found");
		}
	}
	
	public static void saveFriend(){
		FriendFactory ff = FriendFactory.getFactoryInstance();
		if (ff.hasInstances()){
			Log.i(TAG, "Saving Friend to local storage");
			ff.save();
		} else {
			Log.i(TAG, "Not Saving Friend. No instance found");
		}
	}

}
