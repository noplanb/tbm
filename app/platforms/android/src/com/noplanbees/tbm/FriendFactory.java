package com.noplanbees.tbm;

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
	
	
}
