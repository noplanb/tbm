package com.zazoapp.client.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;

import com.google.gson.internal.LinkedTreeMap;

import java.util.List;
import java.util.Map;

public class Contact implements Parcelable{

    public static class ContactKeys{

        public static final String FIRST_NAME = "firstName";
        public static final String LAST_NAME = "lastName";
        public static final String DISPLAY_NAME = "displayName";
        public static final String ID = "id";
    }

	public static class PhoneNumberKeys{
        public static final String PHONE_TYPE = "phoneType";
        public static final String PHONE_TYPE_INT = "phoneTypeInt";
        public static final String PHONE_NUMBER = "phoneNumber";
        public static final String INTERNATIONAL = "international";
        public static final String NATIONAL = "national";
        public static final String E164 = "e164";
        public static final String COUNTRY_CODE = "countryCode";
    }

	public List<LinkedTreeMap<String,String>> phoneObjects;
    public Map<String, String> contact;

	public Contact(LinkedTreeMap<String, String> contact, List<LinkedTreeMap<String, String>> phoneObjects){
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

    public String getId() {
        return contact.get(ContactKeys.ID);
    }

	public String toString(){
		String c = "none";
		if (contact != null)
			c = contact.toString();
		String po = "none";
		if (phoneObjects != null)
			po = phoneObjects.toString();

		return "contact: " + c + "\n" + "phoneObjects: " + po;
	}

	public LinkedTreeMap<String, String> firstPhoneMarkedMobileForContact(){
		if (phoneObjects == null)
			return null;

		for(LinkedTreeMap<String, String> po : phoneObjects){
			if (po.get(PhoneNumberKeys.PHONE_TYPE_INT).equalsIgnoreCase(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE + ""));
				return po;
		}
		return null;
	}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(phoneObjects);
        dest.writeMap(contact);
    }
}
