package com.noplanbees.tbm;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Bundle;
import android.util.Log;

public class VideoIdUtils {
	private final static String STAG = VideoIdUtils.class.getSimpleName();
			
	public static String OutgoingVideoId(Friend friend, User user){
		String r = "";
		r += user.getId();
		r += "-";
		r += friend.getId();
		r += "-";
		r += user.get(User.Attributes.FIRST_NAME);
		r += "-";
		r += friend.get(Friend.Attributes.FIRST_NAME);
		r += "-";
		r += StringUtils.randomString(50);
		return r;
	}
	
	public static HashMap<String, String> senderAndReceiverIdsWithVideoId(String videoId){
		HashMap <String, String> r = new HashMap<String, String>();
		Pattern pattern = Pattern.compile("^(\\d+)-(\\d+)-");
		Matcher matcher = pattern.matcher(videoId);
		matcher.find();
		
		if (matcher.groupCount() != 2){
			System.out.println("senderAndReceiverIdsWithVideoId: ERROR: Did not get 2 matches when searching for and reciever ids from video id. This should never happen.");
			return null;
		}
		r.put("senderId", matcher.group(1));
    	r.put("receiverId", matcher.group(2));
		return r;
	}
}
