package com.zazoapp.client.utilities;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.util.Random;

public class StringUtils {

	public static String randomString(int length) {
		String r = "";
		Random rand = new Random();
		for (int i=0; i< length; i++){
			r += (char) azAZFromInt(rand.nextInt(52));
		}
		return r;
	}
	
	public static char azAZFromInt(int num){
	    int lowerStart = (int)'a';
	    int upperStart = (int)'A';
	    int numLetters = (int)'z' - (int)'a' + 1;
	    
	    int offset = num % numLetters;
	    int start;
	    
	    if (num / numLetters > 0){
	        start = upperStart;
	    } else {
	        start = lowerStart;
	    }
	    
	    return (char)(start + offset);
	}
	
	public static void stripSpaces(String s){
		s.replaceAll(" ", "");
	}
	
	@SuppressWarnings("unchecked")
	public static LinkedTreeMap<String, String> linkedTreeMapWithJson(String json){
		Gson g = new Gson();
		LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
		data = g.fromJson(json, data.getClass());
		return data;
	}
}
