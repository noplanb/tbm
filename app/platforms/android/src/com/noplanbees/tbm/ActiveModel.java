package com.noplanbees.tbm;

import java.util.ArrayList;

import com.google.gson.Gson;

import android.os.Bundle;

public class ActiveModel {
	
	public static final String[] attributeList = {"at0", "at1"};
	private static ArrayList<ActiveModel> instances;
	
	public Bundle attributes;
	
	public ActiveModel(){
		instances.add(this);
		for(String atr : attributeList){
			attributes.putString(atr, null);
		}
	}
	
	
	//--------------------
	// Save and retrieve
	//--------------------
	public static String save(){
		ArrayList <Bundle> all = null;
		for (ActiveModel i : instances){
			all.add(i.attributes);
		}
		Gson gson = new Gson();
		return gson.toJson(all);
	}
	
	public static void retrieve(String j){
		Gson gson = new Gson();
		ArrayList <Bundle> all = gson.fromJson(j, ArrayList.class);
		for (Bundle b : all){
			ActiveModel am = new ActiveModel();
			am.attributes.putAll(b);
		}
	}
	
	//--------------------
	// Finders
	//--------------------
	public ActiveModel findWhere(String a, String v){
		ActiveModel found = null;
		for (ActiveModel i : ActiveModel.instances){
			if (i.getAttStr(a) == v){
				found = i;
				break;
			}
		}
		return found;
	}
	
	public ActiveModel findWhere(String a, Integer v){
		ActiveModel found = null;
		for (ActiveModel i : ActiveModel.instances){
			if (i.getAttrInt(a) == v){
				found = i;
				break;
			}
		}
		return found;
	}
	
	public ActiveModel find(Integer id){
		return findWhere("id", id);
	}
	

	//--------------------
	// Getters and setters
	//--------------------
	public ActiveModel setAttr(String a, String v){
		attributes.putString(a, v);
		return this;
	}
	
	public ActiveModel setAttr(String a, Integer i){
		attributes.putInt(a, i);
		return this;
	}

	public ActiveModel setAttr(String a, Boolean b){
		attributes.putBoolean(a, b);
		return this;
	}
	
	public String getAttStr(String a){
		return attributes.getString(a);
	}
	
	public Integer getAttrInt(String a){
		return attributes.getInt(a);
	}
	
	public boolean getAttrBool(String a){
		return attributes.getBoolean(a);
	}


}
