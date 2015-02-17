package com.noplanbees.tbm.bench;

import com.google.gson.internal.LinkedTreeMap;
import com.noplanbees.tbm.model.Contact;

public class BenchObject {

    public static BenchObject benchObjectWithContact(Contact contact, LinkedTreeMap<String, String> mobileNumber) {
        LinkedTreeMap<String, String> boParams = new LinkedTreeMap<String, String>();
        boParams.put(Keys.DISPLAY_NAME, contact.getDisplayName());
        boParams.put(Keys.FIRST_NAME, contact.getFirstName());
        boParams.put(Keys.LAST_NAME, contact.getLastName());
        boParams.put(Keys.MOBILE_NUMBER, mobileNumber.get(Contact.PhoneNumberKeys.E164));
        return new BenchObject(boParams);
    }

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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(displayName).append(", ").append(mobileNumber);
        return builder.toString();
    }
}
