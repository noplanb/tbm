package com.zazoapp.client.model;

import android.content.Context;

import com.zazoapp.client.multimedia.VideoIdUtils;

import java.util.Comparator;


public class Video extends ActiveModel {

    /**
     * Normal state machine: QUEUED -> NEW <-> DOWNLOADING -> DOWNLOADED -(onViewed)-> VIEWED
     */
	public static class IncomingVideoStatus{
		public static final int NONE = 0;
		public static final int NEW = 1;
		public static final int QUEUED = 2;
		public static final int DOWNLOADING = 3;
		public static final int DOWNLOADED = 4;
		public static final int VIEWED = 5;
		public static final int FAILED_PERMANENTLY = 6;
	}
	
	public static class Attributes{
		public static final String ID  = "id";
		public static final String FRIEND_ID = "friendId";
		public static final String STATUS = "status";
		public static final String DOWNLOAD_RETRY_COUNT = "downloadRetryCount";
	}
	
	@Override
	public String[] attributeList() {
		final String[] a = {	
			Attributes.ID,
			Attributes.FRIEND_ID,
			Attributes.STATUS,
			Attributes.DOWNLOAD_RETRY_COUNT
		};
		return a;
	}
	
	@Override
	public void init(Context context) {
		super.init(context);
		setIncomingVideoStatus(IncomingVideoStatus.NONE);
		setDownloadRetryCount(0);
	}
	
	public static class VideoTimestampComparator implements Comparator<Video> {
	    @Override
	    public int compare(Video o1, Video o2) {
	        return VideoIdUtils.timeStampFromVideoId(o1.getId()).compareTo(VideoIdUtils.timeStampFromVideoId(o2.getId()));
	    }
	}


	// Incoming video status
	public void setIncomingVideoStatus(int status){
		set(Attributes.STATUS, ((Integer) status).toString());
	}

	public int getIncomingVideoStatus(){
		return Integer.parseInt(get(Attributes.STATUS));
	}

	// Download retryCount
	public void setDownloadRetryCount(int retryCount){
		set(Attributes.DOWNLOAD_RETRY_COUNT, ((Integer) retryCount).toString());
	}

	public int getDownloadRetryCount(){
		return Integer.parseInt(get(Attributes.DOWNLOAD_RETRY_COUNT));
	}


}
