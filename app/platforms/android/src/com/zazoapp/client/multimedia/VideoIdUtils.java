package com.zazoapp.client.multimedia;

import android.util.Log;

import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.Video;


public class VideoIdUtils {
	private static final String TAG = VideoIdUtils.class.getSimpleName();
	
	public static String generateId() {
		return System.currentTimeMillis() + "";
	}
	
	public static Boolean isNewerThanOldestIncomingVideo(Friend friend, String videoId){
		Video oldest = friend.oldestIncomingVideo();
		return (oldest != null && timeStampFromVideoId(videoId) > timeStampFromVideoId(oldest.getId()));
	}
	
	public static Boolean isOlderThanOldestIncomingVideo(Friend friend, String videoId){
		Video oldest = friend.oldestIncomingVideo();
		return (oldest != null && timeStampFromVideoId(videoId) < timeStampFromVideoId(oldest.getId()));		
	}
	
	public static Long timeStampFromVideoId(String videoId){
		if (videoId == null || videoId.equalsIgnoreCase(""))
			return 0L;
	
		return Long.parseLong(videoId);
	}

	public static String newerVideoId(String vid1, String vid2) {
		if (timeStampFromVideoId(vid1) > timeStampFromVideoId(vid2)){
			Log.d(TAG, "vid1=" + vid1 + " vid2=" + vid2 + " newer=" + vid1);
			return vid1;
		} else {
			Log.d(TAG, "vid1=" + vid1 + " vid2=" + vid2 + " newer=" + vid2);
			return vid2;
		}
	}
 
}