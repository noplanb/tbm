package com.zazoapp.client.utilities;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
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
    @Nullable public static LinkedTreeMap<String, String> linkedTreeMapWithJson(String json) {
        return fromJson(json, LinkedTreeMap.class);
    }

    @Nullable public static <T> T fromJson(String json, @NonNull Class<T> classOfT) {
        Gson g = new Gson();
        T t;
        try {
            t = g.fromJson(json, classOfT);
        } catch (JsonSyntaxException e) {
            Log.d("ZazoJsonParser", e.getMessage());
            return null;
        }
        return t;
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

    public static String getEventTime(String videoTimestamp) {
        if (TextUtils.isEmpty(videoTimestamp)) {
            return "";
        }
        try {
            long timestamp = Long.parseLong(videoTimestamp);
            Date date = new Date(timestamp);
            Calendar calendar = Calendar.getInstance();
            int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
            int currentMonth = calendar.get(Calendar.MONTH);
            int currentYear = calendar.get(Calendar.YEAR);
            calendar.clear();
            calendar.set(currentYear, currentMonth, currentDay);
            long dayStartTime = calendar.getTimeInMillis();
            long timeDifference = dayStartTime - timestamp;
            SimpleDateFormat dateFormat;
            if (timeDifference <= 0) {
                // Today
                dateFormat = (SimpleDateFormat) SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT);
            } else if (timeDifference <= 518400000) {
                // Less than a week ago
                dateFormat = new SimpleDateFormat("ccc " + ((SimpleDateFormat) SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT)).toPattern());
            } else if (Build.VERSION.SDK_INT >= 18) {
                dateFormat = new SimpleDateFormat();
                dateFormat.applyPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMd"));
            } else {
                dateFormat = (SimpleDateFormat) SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM);
            }
            return dateFormat.format(date);
        } catch (NumberFormatException e) {
            Log.e("Util", "Error to parse event time");
        }
        return "";
    }

    public static String getInitials(String a, String b) {
        StringBuilder initials = new StringBuilder();
        if (a != null && !a.isEmpty()) {
            initials.append(a.toUpperCase().charAt(0));
        }
        if (b != null && !b.isEmpty()) {
            initials.append(b.toUpperCase().charAt(0));
        }
        return initials.toString();
    }

    public static String getInitials(String a) {
        if (a != null && !a.isEmpty()) {
            String[] split = a.split(" ", 2);
            if (split.length == 2) {
                return getInitials(split[0], split[1]);
            } else {
                return getInitials(split[0], null);
            }
        } else {
            return "";
        }
    }

    public static CharSequence getFirstLetter(CharSequence sequence) {
        if (TextUtils.isEmpty(sequence)) {
            return "?";
        }
        if (sequence.charAt(0) <= '9') {
            return "#";
        }
        return String.valueOf(sequence.charAt(0)).toUpperCase();
    }
}
