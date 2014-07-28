package com.noplanbees.tbm;

import android.content.Context;
import android.util.Log;

public class ActiveModelsHandler {
	public static String TAG = "ActiveModelsHandler";

	public static void ensureAll(Context context){
		ensureUser(context);
		ensureFriend(context);
	}
	
	public static void saveAll(Context context){
		saveUser(context);
		saveFriend(context);
	}
	
	public static void retreiveAll(Context context){
		retrieveUser(context);
		retrieveFriend(context);
	}
	
	public static void destroyAll(Context context){
		UserFactory.getFactoryInstance().destroyAll(context);
		FriendFactory.getFactoryInstance().destroyAll(context);
	}
	
	public static UserFactory ensureUser(Context context){
		UserFactory uf = UserFactory.getFactoryInstance();
		if (uf.hasInstances()){
			Log.i(TAG, "User present in memory.");
		} else if (uf.retrieve(context)){
			Log.i(TAG, "Retrieved User from local storage.");
		} else {
			Log.i(TAG, "Config not retrievable from local storage. Creating Conf instance.");
			uf.makeInstance(context);
		}
		return uf;
	}
	
	public static FriendFactory ensureFriend(Context context){
		FriendFactory ff = FriendFactory.getFactoryInstance();
		FriendFactory r;
		if (ff.hasInstances()){
			Log.i(TAG, "Friend present in memory");
			r = ff;
		} else if (ff.retrieve(context)){
			Log.i(TAG, "Retrieved Friend from local storage.");
			r = ff;
		} else {
			Log.i(TAG, "Friend not retrievable from local storage.");
			r = null;
		}
		return r;
	}
	
	public static FriendFactory retrieveFriend(Context context){
		FriendFactory ff = FriendFactory.getFactoryInstance();
        ff.retrieve(context);
        return ff;
	}
	
	public static UserFactory retrieveUser(Context context){
		UserFactory uf = UserFactory.getFactoryInstance();
        uf.retrieve(context);
        return uf;
	}
	
	public static void saveUser(Context context){
		UserFactory uf = UserFactory.getFactoryInstance();
		if (uf.hasInstances()){
			Log.i(TAG, "Saving User to local storage");
			uf.save(context);
		} else {
			Log.i(TAG, "Not Saving User. No instance found");
		}
	}
	
	public static void saveFriend(Context context){
		FriendFactory ff = FriendFactory.getFactoryInstance();
		if (ff.hasInstances()){
			Log.i(TAG, "Saving Friend to local storage");
			ff.save(context);
		} else {
			Log.i(TAG, "Not Saving Friend. No instance found");
		}
	}

}
