package com.noplanbees.tbm;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class FriendFactory extends ActiveModelFactory{

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
		return (Friend) FriendFactory.getFactoryInstance().findWhere("frameId", viewId.toString());
	}

	public Friend getFriendFromIntent(Intent intent) {
		Friend f = null;
		Bundle extras = intent.getExtras();
		if (extras != null){
			if ( extras.get("friendId") != null ){
				f = (Friend) find(extras.getString("friendId"));
			} else if ( extras.get("receiverId") != null ){
				f = (Friend) find(extras.getString("receiverId"));
			} else if ( extras.get("from_id") != null ){
				f = (Friend) find(extras.getString("from_id"));
			} else if ( extras.get("to_id") != null ){
				f = (Friend) find(extras.getString("to_id"));
			}		
		}
		return f;
	}

}
