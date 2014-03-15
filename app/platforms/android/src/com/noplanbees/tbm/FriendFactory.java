package com.noplanbees.tbm;

public class FriendFactory extends ActiveModelFactory{

	@Override
	protected Friend makeInstance() {
		Friend i = new Friend();
		i.init();
		instances.add(i);
		return i;	}
}
