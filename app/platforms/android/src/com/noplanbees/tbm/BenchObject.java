package com.noplanbees.tbm;

import com.google.gson.internal.LinkedTreeMap;

public class BenchObject {
	
	public static class Keys{
		public static final String FRIEND_ID = "friendId";
		public static final String FIRST_NAME = "firstName";
		public static final String LAST_NAME = "lastName";
		public static final String DISPLAY_NAME = "displayName";
		public static final String MOBILE_NUMBER = "mobileNumber";
	}
	
	public String friendId;
	public String firstName;
	public String lastName;
	public String displayName;
	public String mobileNumber;
	
	public BenchObject(LinkedTreeMap<String, String> params){
		friendId = params.get(Keys.FRIEND_ID);
		firstName = params.get(Keys.FIRST_NAME);
		lastName = params.get(Keys.LAST_NAME);
		displayName = params.get(Keys.DISPLAY_NAME);
		mobileNumber = params.get(Keys.MOBILE_NUMBER);
	}

}
