package com.noplanbees.tbm;

import android.util.Log;

import com.google.gson.internal.LinkedTreeMap;



public class ActiveModel {
	protected final String TAG = this.getClass().getSimpleName();

	public LinkedTreeMap<String, String> attributes = new LinkedTreeMap<String, String>();

	public void init(){
		for(String atr : attributeList()){
			attributes.put(atr, "");
		}
	}

	// Must be overridden in subclass.	
	public String[] attributeList(){
		return null;
	}

	// Must be overridden in subclass.	
	public static String castToSubclass(){
		return null;
	}
	
	

	//--------------------
	// Getters and setters
	//--------------------
	public ActiveModel set(String a, String v){
		ActiveModel result = this;
		if ( attributes.containsKey(a) ) {
			Log.i(TAG, "setting " + a + " : " + v);
			attributes.put(a, v);
		} else {
			result = null;
			Log.e(TAG, "ERROR: set: " + a + " is not an attr. This should neve happen");
			throw new RuntimeException();
		}
		return result;
	}

	public String get(String a){
		return attributes.get(a);
	}
	
	public String getId(){
		return attributes.get("id");
	}
}
