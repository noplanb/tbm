package com.zazoapp.client.model;

import android.content.Context;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class User extends ActiveModel{

    public static class Attributes {
        public static final String ID = "id";
        public static final String AUTH = "auth";
        public static final String MKEY = "mkey";
        public static final String FIRST_NAME = "firstName";
        public static final String LAST_NAME = "lastName";
        public static final String REGISTERED = "registered";
        public static final String MOBILE_NUMBER = "mobileNumber";
        public static final String INVITEE = "invitee";
    }

    @Override
    public List<String> attributeList() {
        final String[] a = {
                Attributes.ID,
                Attributes.AUTH,
                Attributes.MKEY,
                Attributes.FIRST_NAME,
                Attributes.LAST_NAME,
                Attributes.REGISTERED,
                Attributes.MOBILE_NUMBER,
                Attributes.INVITEE
        };
        return new ArrayList<>(Arrays.asList(a));
    }

    public static boolean isRegistered(Context context) {
        UserFactory uf = ActiveModelsHandler.getInstance(context).ensureUser();
        return uf.hasInstances() && uf.all().get(0).get(User.Attributes.REGISTERED).startsWith("t");
    }

    public static String userId(Context context) {
        String id = null;
        UserFactory uf = ActiveModelsHandler.getInstance(context).ensureUser();
        if (uf.hasInstances()) {
            id = uf.all().get(0).getId();
        }
        return id;
    }

    public String getId(){
    	return get(User.Attributes.ID);
    }

    @Override
    public boolean validate() {
        return true;
    }

    public String getFirstName(){
    	return get(User.Attributes.FIRST_NAME);
    }
    
    public String getLastName(){
    	return get(User.Attributes.LAST_NAME);
    }
	
    public String getFullName(){
    	return getFirstName() + " " + getLastName();
    }

    public String getPhoneNumber(PhoneNumberUtil.PhoneNumberFormat format) {
        Phonenumber.PhoneNumber phone = getPhoneNumberObj();
        return PhoneNumberUtil.getInstance().format(phone, format);
    }

    public boolean isInvitee() {
        return TRUE.equals(get(Attributes.INVITEE));
    }

    public void setInvitee(boolean invitee) {
        set(Attributes.INVITEE, invitee ? TRUE : FALSE);
    }

    public boolean inviteeIsNotSet() {
        return get(Attributes.INVITEE).isEmpty();
    }

    public PhoneNumber getPhoneNumberObj(){
    	PhoneNumber r = null;
    	try {
    		r = PhoneNumberUtil.getInstance().parse(get(Attributes.MOBILE_NUMBER), "US");
    	} catch (NumberParseException e) {
			//Dispatch.dispatch("ERROR: " + get(Attributes.MOBILE_NUMBER) + ":  NumberParseException was thrown: " + e.toString());
    	}
    	return r;
    }
    
    public Integer getCountryCode(){
    	if (getPhoneNumberObj() == null)
    		return 1; //USA = 1
    	return getPhoneNumberObj().getCountryCode();
    }
    
    public String getRegion(){
    	if (getCountryCode() == null)
    		return null;
    	return PhoneNumberUtil.getInstance().getRegionCodeForCountryCode(getCountryCode());
    }
    
    public String getAreaCode(){
    	PhoneNumber pn = getPhoneNumberObj();
    	if (pn == null)
    		return "";
    	
    	PhoneNumberUtil pu = PhoneNumberUtil.getInstance();    	
    	String nsn = pu.getNationalSignificantNumber(pn);
    	int length =    pu.getLengthOfNationalDestinationCode(pn);

    	if (length > 0) {
    	  return nsn.substring(0,  length);
    	} else {
    		return "";
    	}
    }

    public String getMkey() {
        return get(Attributes.MKEY);
    }
}
