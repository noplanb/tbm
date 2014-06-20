package com.noplanbees.tbm;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class FriendFactory extends ActiveModelFactory{
	private final String TAG = getClass().getSimpleName();
	
	public static FriendFactory instance = null;

	public static FriendFactory getFactoryInstance(){
		if ( instance == null ) 
			instance = new FriendFactory();
		return instance;
	}

	@Override
	protected Friend makeInstance() {
		Friend i = new Friend();
		i.init();
		instances.add(i);
		return i;	}

	public static Friend getFriendFromFrame(View v){
		Integer viewId = v.getId();
		return (Friend) getFactoryInstance().findWhere(Friend.Attributes.FRAME_ID, viewId.toString());
	}

	public Friend getFriendFromIntent(Intent intent) {
		Friend f = null;
		Bundle extras = intent.getExtras();
		Log.i(TAG, "getFriendFromIntent");
		Convenience.printBundle(extras);
		if (extras != null){
			if ( extras.get("friendId") != null ){
				f = (Friend) find(extras.getString("friendId"));
			} else if ( extras.get("receiverId") != null ){
				f = (Friend) find(extras.getString("receiverId"));
			} else if ( extras.get("from_id") != null ){
				f = (Friend) find(extras.getString("from_id"));
			} else if ( extras.get("to_id") != null ){
				f = (Friend) find(extras.getString("to_id"));
			} else if ( extras.get("id") != null ){
				f = (Friend) find(extras.getString("id"));
			}		
		}
		return f;
	}
	
	public ArrayList<Friend> all(){
		ArrayList<Friend> r = new ArrayList<Friend>();
		for (ActiveModel a : instances){
			r.add((Friend) a); 
		}
		return r;
	}

}
