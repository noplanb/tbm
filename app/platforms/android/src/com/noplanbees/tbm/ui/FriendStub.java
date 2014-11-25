package com.noplanbees.tbm.ui;

import android.graphics.Bitmap;


public class FriendStub{
	private String name;
	private Bitmap thumb;
	private String friendId;
	private boolean notViewed;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Bitmap getThumb() {
		return thumb;
	}
	public void setThumb(Bitmap imagePath) {
		this.thumb = imagePath;
	}
	public String getFriendId() {
		return friendId;
	}
	public void setFriendId(String friendId) {
		this.friendId = friendId;
	}
	public boolean isNotViewed() {
		return notViewed;
	}
	public void setNotViewed(boolean notViewed) {
		this.notViewed = notViewed;
	}
}