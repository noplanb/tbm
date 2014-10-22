package com.noplanbees.tbm;

import java.util.ArrayList;

import com.google.gson.internal.LinkedTreeMap;

public class Contact {
	
	public class ContactKeys{
		public static final String FIRST_NAME = "firstName";
		public static final String LAST_NAME = "lastName";
		public static final String DISPLAY_NAME = "displayName";
	}
	
	public class PhoneNumberKeys{
		public static final String PHONE_TYPE = "phoneType";
		public static final String PHONE_NUMBER = "phoneNumber";
		public static final String INTERNATIONAL = "international";
		public static final String NATIONAL = "national";
		public static final String E164 = "e164";
	}

	public ArrayList<LinkedTreeMap<String,String>> phoneObjects;
	public LinkedTreeMap<String, String> contact;
	
	public Contact(LinkedTreeMap<String, String> contact, ArrayList<LinkedTreeMap<String, String>> phoneObjects){
		this.phoneObjects = phoneObjects;
		this.contact = contact;
	}
	
	public String getDisplayName(){
		return contact.get(ContactKeys.DISPLAY_NAME);
	}
	
	public String getFirstName(){
		return contact.get(ContactKeys.FIRST_NAME);
	}

	public String getLastName() {
		return contact.get(ContactKeys.LAST_NAME);
	}
	
	public String toString(){
		return "contact: " + contact.toString() + "\n" + "phoneObjects: " + phoneObjects.toString();
	}
}
