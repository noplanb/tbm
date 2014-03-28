package com.noplanbees.tbm;

import android.view.View;
import android.widget.FrameLayout;

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

}
