package com.noplanbees.tbm;

import android.app.Activity;
import android.content.Context;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;


public class GridElement extends ActiveModel {

	public static class Attributes{
		public static final String ID  = "id";
		public static final String FRIEND_ID = "friendId";
	}
	
	@Override
	public String[] attributeList() {
		final String[] a = {	
			Attributes.ID,
			Attributes.FRIEND_ID,
		};
		return a;
	}
	
	public Activity activity;
	public VideoView videoView;
	public ImageView thumbView;
	public TextView nameText; 
	public FrameLayout frame;
	public VideoPlayer videoPlayer;
	
	@Override
	public void init(Context context) {
		super.init(context);
		activity = (Activity) context;
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


}
