package com.zazoapp.client.utilities;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;

import java.util.Date;
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

    /**
     *
     * @param json JSON string
     * @return LinkedTreeMap with data or null if JSON string is broken
     */
    public static LinkedTreeMap<String, String> linkedTreeMapWithJson(String json) {
        Gson g = new Gson();
        LinkedTreeMap<String, String> data;
        try {
            data = g.fromJson(json, LinkedTreeMap.class);
        } catch (JsonSyntaxException e) {
            Log.d("ZazoJsonParser", e.getMessage());
            return null;
        }
        return data;
    }

    /**
     *
     * @param input date in format "yyyy-MM-ddTHH:mm:ss.sssZ"
     * @return Date object or null if couldn't parse
     */
    public static Date parseTime(String input) {
        try {
            return com.amazonaws.util.DateUtils.parseISO8601Date(input);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
