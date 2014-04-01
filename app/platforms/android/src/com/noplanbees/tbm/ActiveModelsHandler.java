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
	
	public static UserFactory ensureUser(){
		UserFactory uf = UserFactory.getFactoryInstance();
		if (uf.hasInstances()){
			Log.i(TAG, "User present in memory.");
		} else if (uf.retrieve()){
			Log.i(TAG, "Retrieved User from local storage.");
		} else {
			Log.i(TAG, "Config not retrievable from local storage. Creating Conf instance.");
			uf.makeInstance();
		}
		return uf;
	}
	
	public static FriendFactory ensureFriend(){
		FriendFactory ff = FriendFactory.getFactoryInstance();
		FriendFactory r;
		if (ff.hasInstances()){
			Log.i(TAG, "Friend present in memory");
			r = ff;
		} else if (ff.retrieve()){
			Log.i(TAG, "Retrieved Friend from local storage.");
			r = ff;
		} else {
			Log.i(TAG, "Friend not retrievable from local storage.");
			r = null;
		}
		return r;
	}
	
	public static FriendFactory retrieveFriend(){
		FriendFactory ff = FriendFactory.getFactoryInstance();
        ff.retrieve();
        return ff;
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
