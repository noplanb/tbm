package com.noplanbees.tbm;

import android.content.Context;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

public class User extends ActiveModel{
    
	public static class Attributes{
		public static final String ID = "id";
		public static final String AUTH = "auth";
		public static final String MKEY = "mkey";
		public static final String FIRST_NAME = "firstName";
		public static final String LAST_NAME = "lastName";
		public static final String REGISTERED = "registered";
		public static final String MOBILE_NUMBER = "mobileNumber";
	}
	
	@Override
	public String[] attributeList() {
      final String[] a = {
    		  Attributes.ID, 
    		  Attributes.AUTH, 
    		  Attributes.MKEY,
    		  Attributes.FIRST_NAME,
    		  Attributes.LAST_NAME,
    		  Attributes.REGISTERED,
    		  Attributes.MOBILE_NUMBER};
      return a;
	}
	
	public static boolean isRegistered(Context context){
		UserFactory uf = ActiveModelsHandler.getInstance(context).ensureUser();
		return uf.hasInstances() && uf.instances.get(0).get(User.Attributes.REGISTERED).startsWith("t");
	}
	
	public static String userId(Context context){
		String id = null;
		UserFactory uf = ActiveModelsHandler.getInstance(context).ensureUser();
		if (uf.hasInstances()){
			id = uf.instances.get(0).getId();
		}
		return id;
	}
	
    public String getId(){
    	return get(User.Attributes.ID);
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
    
}
