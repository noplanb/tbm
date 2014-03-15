package com.noplanbees.tbm;

import com.google.gson.internal.LinkedTreeMap;



public class ActiveModel {
	private final String TAG = this.getClass().getClass().getSimpleName();

	public LinkedTreeMap<String, String> attributes = new LinkedTreeMap<String, String>();

	public void init(){
		for(String atr : attributeList()){
			attributes.put(atr, null);
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
		attributes.put(a, v);
		return this;
	}

	public String get(String a){
		return attributes.get(a);
	}
}
