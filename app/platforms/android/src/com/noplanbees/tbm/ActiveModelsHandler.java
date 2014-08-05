package com.noplanbees.tbm;

import android.content.Context;
import android.util.Log;

public class ActiveModelsHandler {
	public static String TAG = "ActiveModelsHandler";

	public static void ensureAll(Context context){
		ensureUser(context);
		ensureFriend(context);
		ensureVideo(context);
	}
	
	public static void saveAll(Context context){
		saveUser(context);
		saveFriend(context);
		saveVideo(context);
	}
	
	public static void retreiveAll(Context context){
		retrieveUser(context);
		retrieveFriend(context);
		retrieveVideo(context);
	}
	
	public static void destroyAll(Context context){
		UserFactory.getFactoryInstance().destroyAll(context);
		FriendFactory.getFactoryInstance().destroyAll(context);
		VideoFactory.getFactoryInstance().destroyAll(context);
	}
	
	public static UserFactory ensureUser(Context context){
		UserFactory uf = UserFactory.getFactoryInstance();
		if (uf.hasInstances()){
			Log.i(TAG, "User present in memory.");
		} else if (uf.retrieve(context)){
			Log.i(TAG, "Retrieved User from local storage.");
		} else {
			Log.i(TAG, "Config not retrievable from local storage. Creating an instance.");
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
			Log.e(TAG, "Friend not retrievable from local storage.");
			r = null;
		}
		return r;
	}
	
	public static VideoFactory ensureVideo(Context context){
		VideoFactory vf = VideoFactory.getFactoryInstance();
		VideoFactory r;
		if (vf.hasInstances()){
			Log.i(TAG, "Video present in memory");
	        r = vf;
		} else if (vf.retrieve(context)){
			Log.i(TAG, "Retrieved Video from local storage.");
			r = vf;
		} else {
			Log.e(TAG, "Video not retrievable from local storage");
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
	
	public static VideoFactory retrieveVideo(Context context){
		VideoFactory vf = VideoFactory.getFactoryInstance();
		vf.retrieve(context);
		return vf;
	}
	
	public static void saveUser(Context context){
		UserFactory uf = UserFactory.getFactoryInstance();
		if (uf.hasInstances()){
			Log.i(TAG, "Saving User to local storage");
			uf.save(context);
		} else {
			Log.i(TAG, "Not Saving User. No instances found");
		}
	}
	
	public static void saveFriend(Context context){
		FriendFactory ff = FriendFactory.getFactoryInstance();
		if (ff.hasInstances()){
			Log.i(TAG, "Saving Friend to local storage");
			ff.save(context);
		} else {
			Log.i(TAG, "Not Saving Friend. No instances found");
		}
	}
	
	public static void saveVideo(Context context){
		VideoFactory vf = VideoFactory.getFactoryInstance();
		if (vf.hasInstances()){
			Log.i(TAG, "Saving Video to local storage");
			vf.save(context);
		} else {
			Log.i(TAG, "Not saving Video. No instances found.");
		}
	}

}
