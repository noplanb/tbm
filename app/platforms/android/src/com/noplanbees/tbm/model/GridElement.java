package com.noplanbees.tbm.model;

import android.content.Context;


public class GridElement extends ActiveModel {

	public static class Attributes{
		public static final String ID  = "id";
		public static final String FRIEND_ID = "friendId";
	}
	
	@Override
	public String[] attributeList() {
		final String[] a = {	
			Attributes.FRIEND_ID,
		};
		return a;
	}
	
	@Override
	public void init(Context context) {
		super.init(context);
		this.context = context;
	}
	
	public Friend friend(){
		String fid = attributes.get(GridElement.Attributes.FRIEND_ID);
		if (fid.equals(""))
			return null;
		
		return (Friend) FriendFactory.getFactoryInstance().find(fid);
	}
	
	public boolean hasFriend(){
		return !attributes.get(GridElement.Attributes.FRIEND_ID).equals("");
	}
	
	public void setFriend(Friend f){
		set(GridElement.Attributes.FRIEND_ID, f.getId());
	}
	
	public String getFriendId(){
		return attributes.get(GridElement.Attributes.FRIEND_ID);
	}


}
