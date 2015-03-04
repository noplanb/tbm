package com.zazoapp.client.model;

import android.content.Context;

import java.util.ArrayList;

public class VideoFactory extends ActiveModelFactory {
	private final String TAG = getClass().getSimpleName();
	
	private static VideoFactory instance = null;

	public static VideoFactory getFactoryInstance(){
		if ( instance == null ) 
			instance = new VideoFactory();
		return instance;
	}

	@Override
	protected Video makeInstance(Context context) {
		Video i = new Video();
		i.init(context);
		instances.add(i);
		return i;	
	}
	
	public ArrayList<Video> all(){
		ArrayList<Video> r = new ArrayList<Video>();
		for (ActiveModel a : instances){
			r.add((Video) a); 
		}
		return r;
	}
	
	public ArrayList<Video> allWhere(String a, String v){
		ArrayList<Video> result = new ArrayList<Video>();
		for (ActiveModel i: instances) {
			if ( i.get(a).equals(v) ){
				result.add((Video) i);
			}
		}
		return result;
	}
	
	public ArrayList<Video> allWithFriendId(String FriendId){
		return allWhere(Video.Attributes.FRIEND_ID, FriendId);
	}
	
}
