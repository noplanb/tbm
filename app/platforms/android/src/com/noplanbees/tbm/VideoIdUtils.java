package com.noplanbees.tbm;

import android.util.Log;

public class VideoIdUtils {
	private static final String STAG = VideoIdUtils.class.getSimpleName();
	
	public static String generateId() {
		return System.currentTimeMillis() + "";
	}
	
	public static Boolean isNewerThanLastIncomingVideo(Friend friend, String videoId){
		Log.e(STAG, "NotificationVideoId=" + videoId + " currentVideoId=" + incomingVideoTimeStamp(friend));
		return timeStampFromVideoId(videoId) > incomingVideoTimeStamp(friend);
	}
	
	public static Boolean isOlderThanLastIncomingVideo(Friend friend, String videoId){
		return timeStampFromVideoId(videoId) < incomingVideoTimeStamp(friend);
	}
	
	public static long incomingVideoTimeStamp(Friend friend){
		return timeStampFromVideoId(friend.get(Friend.Attributes.INCOMING_VIDEO_ID));
	}
	
	public static Long timeStampFromVideoId(String videoId){
		if (videoId == null || videoId.equalsIgnoreCase(""))
			return 0L;
	
		return Long.parseLong(videoId);
	}

	public static String newerVideoId(String vid1, String vid2) {
		if (timeStampFromVideoId(vid1) > timeStampFromVideoId(vid2)){
			Log.e(STAG, "vid1=" + vid1 + " vid2=" + vid2 + " newer=" + vid1);
			return vid1;
		} else {
			Log.e(STAG, "vid1=" + vid1 + " vid2=" + vid2 + " newer=" + vid2);
			return vid2;
		}
	}
 
}
