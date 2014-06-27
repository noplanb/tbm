package com.noplanbees.tbm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.VideoView;

import com.google.gson.internal.LinkedTreeMap;

interface VideoStatusChangedCallback{
	public void onVideoStatusChanged(Friend friend);
}

public class Friend extends ActiveModel{

	public static class OutgoingVideoStatus{
		public static final int NONE = 0;
		public static final int NEW = 1;
		public static final int QUEUED = 2;
		public static final int UPLOADING = 3;
		public static final int UPLOADED = 4;
		public static final int DOWNLOADED = 5;
		public static final int VIEWED = 6;
		public static final int FAILED_PERMANENTLY = 7;
	}

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
		public static final String VIEW_INDEX  = "viewIndex";
		public static final String FRAME_ID  = "frameId";
		public static final String VIEW_ID  = "viewId";
		public static final String THUMB_VIEW_ID  = "thumbViewId";
		public static final String NAME_TEXT_ID  = "nameTextId";
		public static final String FIRST_NAME  = "firstName";
		public static final String LAST_NAME  = "lastName";
		public static final String VIDEO_PATH  = "videoPath";
		public static final String OUTGOING_VIDEO_ID = "outgoingVideoId";
		public static final String LAST_OUTGOING_VIDEO_ID = "lastOutgoingVideoId";
		public static final String OUTGOING_VIDEO_STATUS  = "outgoingVideoStatus";
		public static final String INCOMING_VIDEO_ID = "incomingVideoId";
		public static final String INCOMING_VIDEO_STATUS  = "incomingVideoStatus";
		public static final String UPLOAD_RETRY_COUNT  = "uploadRetryCount";
		public static final String DOWNLOAD_RETRY_COUNT  = "downloadRetryCount";
		public static final String LAST_VIDEO_STATUS_EVENT_TYPE = "lastVideoStatusEventType";
	}

	public static class VideoStatusEventType{
		public static final Integer OUTGOING = 0;
		public static final Integer INCOMING = 1;
	}

	@Override
	public String[] attributeList() {
		final String[] a = {	Attributes.ID, 
				Attributes.VIEW_INDEX, 
				Attributes.FRAME_ID, 
				Attributes.VIEW_ID, 
				Attributes.THUMB_VIEW_ID,
				Attributes.NAME_TEXT_ID,
				Attributes.FIRST_NAME, 
				Attributes.LAST_NAME, 
				Attributes.VIDEO_PATH, 
				Attributes.OUTGOING_VIDEO_ID,
				Attributes.LAST_OUTGOING_VIDEO_ID,
				Attributes.OUTGOING_VIDEO_STATUS,
				Attributes.INCOMING_VIDEO_ID,
				Attributes.INCOMING_VIDEO_STATUS,
				Attributes.UPLOAD_RETRY_COUNT,
				Attributes.DOWNLOAD_RETRY_COUNT,
				Attributes.LAST_VIDEO_STATUS_EVENT_TYPE};
		return a;
	}
	
	

	@Override
	public void init(Context context) {
		super.init(context);
		setIncomingVideoStatus(IncomingVideoStatus.NONE);
		setOutgoingVideoStatus(OutgoingVideoStatus.NONE);
		set(Attributes.LAST_VIDEO_STATUS_EVENT_TYPE, VideoStatusEventType.INCOMING.toString());
		setDownloadRetryCount(0);
		setUploadRetryCount(0);
	}



	//-------------------------
	// Video and thumb
	//-------------------------
	public String videoFromPath(){
		return Config.homeDirPath(context) + "/vid_from_" + getId() + ".mp4";		
	}

	public File videoFromFile(){
		return new File(videoFromPath());
	}

	public String videoToPath(){
		return Config.homeDirPath(context) + "/vid_to_" + getId() + ".mp4";		
	}

	public File videoToFile(){
		return new File(videoToPath());
	}

	public String thumbPath(){
		return Config.homeDirPath(context) + "/thumb_from_" + getId() + ".mp4";		
	}

	public File thumbFile(){
		return new File(thumbPath());		
	}

	public Bitmap thumbBitmap(){
		return Convenience.bitmapWithFile(thumbFile());
	}

	@SuppressLint("NewApi")
	public Bitmap sqThumbBitmap(){
		Bitmap sq = null;
		Bitmap thumbBmp = thumbBitmap();
		if (thumbBmp != null)
			sq = ThumbnailUtils.extractThumbnail(thumbBmp, thumbBmp.getWidth(), thumbBmp.getWidth());
		Log.i(TAG, "sqThumbBitmap: size = " + ((Integer) sq.getByteCount()).toString());
		return sq;
	}

	public boolean thumbExists(){
		return thumbFile().exists();
	}
	
	public boolean incomingVideoNotViewed(){
		return getIncomingVideoStatus() == IncomingVideoStatus.DOWNLOADED;
	}
	
	public void setIncomingVideoViewed(){
		set(Attributes.INCOMING_VIDEO_STATUS, ((Integer) IncomingVideoStatus.VIEWED).toString());
	}
	
	//-------------
	// Create Thumb
	//-------------
	public synchronized void createThumb(){
		Log.i(TAG, "createThumb for friend=" + get(Attributes.FIRST_NAME));
		
		if( !videoFromFile().exists() || videoFromFile().length() == 0 ){
			Log.e(TAG, "createThumb: no video file found for friend=" + get(Attributes.FIRST_NAME));
			return;
		}
		
		String vidPath = videoFromPath();
		Bitmap thumb = ThumbnailUtils.createVideoThumbnail(vidPath, MediaStore.Images.Thumbnails.MINI_KIND);
		File thumbFile = thumbFile();
		try {
			FileOutputStream fos = FileUtils.openOutputStream(thumbFile);
			thumb.compress(Bitmap.CompressFormat.PNG, 100, fos);
		} catch (IOException e) {
			Log.e(TAG, "createThumb: IOException " + e.getMessage() + e.toString());
		}
	}
	
	//--------------------------
	// Video upload and download
	//--------------------------
	private void setOutGoingVideoId(){
		Log.i(TAG, "setOutGoingVideoId.");
		set(Attributes.OUTGOING_VIDEO_ID, VideoIdUtils.OutgoingVideoId(this, UserFactory.current_user()));
	}

	public void uploadVideo(){
		Log.i(TAG, "uploadVideo. For friend=" + get(Attributes.FIRST_NAME));

		setAndNotifyOutgoingVideoStatus(OutgoingVideoStatus.QUEUED);
		setOutGoingVideoId();

		Intent i = new Intent(context, FileUploadService.class);
		i.putExtra(FileTransferService.IntentFields.ID_KEY, getId());
		i.putExtra(FileTransferService.IntentFields.FILE_PATH_KEY, videoToPath());
		i.putExtra(FileTransferService.IntentFields.URL_KEY, Config.fileUploadUrl());
		Bundle params = new Bundle();
		params.putString("video_id", get(Attributes.OUTGOING_VIDEO_ID));
		i.putExtra(FileTransferService.IntentFields.PARAMS_KEY, params);
		i.addFlags(Intent.FLAG_FROM_BACKGROUND);
		i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS); 	// See doc/task_manager_bug.txt for the reason for this flag.
		context.startService(i);
	}
	
	public void downloadVideo(Intent intent){
		Log.i(TAG, "downloadVideo. For friend=" + get(Attributes.FIRST_NAME));
		
		String videoId = intent.getStringExtra(FileTransferService.IntentFields.VIDEO_ID_KEY);
		set(Attributes.INCOMING_VIDEO_ID, videoId);
		setAndNotifyIncomingVideoStatus(IncomingVideoStatus.QUEUED);
		
		Intent i = new Intent(context, FileDownloadService.class);
		i.putExtra(FileTransferService.IntentFields.ID_KEY, getId());
		i.putExtra(FileTransferService.IntentFields.FILE_PATH_KEY, videoFromPath());
		i.putExtra(FileTransferService.IntentFields.URL_KEY, Config.fileDownloadUrl());
		Bundle params = new Bundle();
		params.putString("video_id", videoId);
		i.putExtra(FileTransferService.IntentFields.PARAMS_KEY, params);
		i.addFlags(Intent.FLAG_FROM_BACKGROUND);
		i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS); 	// See doc/task_manager_bug.txt for the reason for this flag.
		context.startService(i);
	}

	//-------------------------
	// Display stuff
	//-------------------------
	public VideoView videoView(Activity activity){
		Integer id = Integer.parseInt(get(Attributes.VIEW_ID));
		return (VideoView) activity.findViewById(id);
	}

	public ImageView thumbView(Activity activity){
		Integer id = Integer.parseInt(get(Attributes.THUMB_VIEW_ID));
		return (ImageView) activity.findViewById(id);
	}

	//=============
	// Video Status 
	//=============

	//-----------------------------------------
	// Changes to status and local notification.
	//------------------------------------------

	// Notify Callbacks
	private ArrayList <VideoStatusChangedCallback> vStatusCallbackDelegates = new ArrayList <VideoStatusChangedCallback> ();

	public void addVideoStatusChangedCallbackDelegate (VideoStatusChangedCallback delegate){
		if (vStatusCallbackDelegates.contains(delegate))
			return;
		vStatusCallbackDelegates.add(delegate);
		Log.i(TAG, "addVideoStatusChangedCallbackDelegate for " + get(Attributes.FIRST_NAME) + " num=" + vStatusCallbackDelegates.size());
	}

	private void notifyStatusChanged(){
		for (VideoStatusChangedCallback delegate : vStatusCallbackDelegates){
			delegate.onVideoStatusChanged(this);
		}
	}

	// Outgoing video status
	private void setOutgoingVideoStatus(int status){
		set(Attributes.OUTGOING_VIDEO_STATUS, ((Integer) status).toString());
	}

	public int getOutgoingVideoStatus(){
		return Integer.parseInt(get(Attributes.OUTGOING_VIDEO_STATUS));
	}

	public void setAndNotifyOutgoingVideoStatus(int status){
		if (getOutgoingVideoStatus() != status){
			setOutgoingVideoStatus(status);
			if (status == OutgoingVideoStatus.NEW)
				setUploadRetryCount(0);
			setLastEventTypeOutgoing();
			notifyStatusChanged();
		}
	}

	// Upload retryCount
	private void setUploadRetryCount(int retryCount){
		set(Attributes.UPLOAD_RETRY_COUNT, ((Integer) retryCount).toString());
	}

	private int getUploadRetryCount(){
		return Integer.parseInt(get(Attributes.UPLOAD_RETRY_COUNT));
	}

	private void setAndNotifyUploadRetryCount(int retryCount){
		if (getUploadRetryCount() != retryCount){
			setUploadRetryCount(retryCount);
			setLastEventTypeOutgoing();
			notifyStatusChanged();
		}
	}

	// Incoming video status
	private void setIncomingVideoStatus(int status){
		set(Attributes.INCOMING_VIDEO_STATUS, ((Integer) status).toString());
	}

	public int getIncomingVideoStatus(){
		return Integer.parseInt(get(Attributes.INCOMING_VIDEO_STATUS));
	}

	public void setAndNotifyIncomingVideoViewed() {
		setAndNotifyIncomingVideoStatus(IncomingVideoStatus.VIEWED);
	}
	public void setAndNotifyIncomingVideoStatus(int status){
		if (getIncomingVideoStatus() != status){
			setIncomingVideoStatus(status);
			if (status == IncomingVideoStatus.NEW)
				setDownloadRetryCount(0);
			if (status == IncomingVideoStatus.VIEWED)
				notifyServerVideoViewed();
			setLastEventTypeIncoming();
			notifyStatusChanged();
		}
	}

	// Download retryCount
	private void setDownloadRetryCount(int retryCount){
		set(Attributes.DOWNLOAD_RETRY_COUNT, ((Integer) retryCount).toString());
	}

	private int getDownloadRetryCount(){
		return Integer.parseInt(get(Attributes.DOWNLOAD_RETRY_COUNT));
	}

	private void setAndNotifyDownloadRetryCount(int retryCount){
		if (getDownloadRetryCount() != retryCount){
			setDownloadRetryCount(retryCount);
			setLastEventTypeIncoming();
			notifyStatusChanged();
		}
	}

	// LastVideoStatusEventType
	private void setLastEventTypeOutgoing(){
		set(Attributes.LAST_VIDEO_STATUS_EVENT_TYPE, VideoStatusEventType.OUTGOING.toString());
	}

	private void setLastEventTypeIncoming(){
		set(Attributes.LAST_VIDEO_STATUS_EVENT_TYPE, VideoStatusEventType.INCOMING.toString());
	}

	private Integer getLastEventType(){
		return Integer.parseInt(get(Attributes.LAST_VIDEO_STATUS_EVENT_TYPE));
	}

	// Update with intent
	public void updateStatus(Intent intent){
		String transferType = intent.getStringExtra(FileTransferService.IntentFields.TRANSFER_TYPE_KEY);
		int status = intent.getIntExtra(FileTransferService.IntentFields.STATUS_KEY, -1);
		int retryCount = intent.getIntExtra(FileTransferService.IntentFields.RETRY_COUNT_KEY, 0);

		if (transferType.equals(FileTransferService.IntentFields.TRANSFER_TYPE_DOWNLOAD)){
			setAndNotifyIncomingVideoStatus(status);
			setAndNotifyDownloadRetryCount(retryCount);
		} else if (transferType.equals(FileTransferService.IntentFields.TRANSFER_TYPE_UPLOAD)){
			setAndNotifyOutgoingVideoStatus(status);
			setAndNotifyUploadRetryCount(retryCount);
		} else {
			Log.e(TAG, "ERROR: updateStatus: unknown TransferType passed in intent. This should never happen.");
			throw new RuntimeException();
		}
	}

	//-------------------------------
	// Server notification of changes
	//-------------------------------
	private void notifyServerVideoViewed() {
		Log.i(TAG, "notifyServerVideoViewed");
		LinkedTreeMap<String, String>params = new LinkedTreeMap<String, String>();
		params.put("from_id", getId());
		params.put("to_id", User.userId(context));
		new SGet("videos/update_viewed", params);
	}

	private class SGet extends Server{
		public SGet(String uri, LinkedTreeMap<String, String> params) {
			super(uri, params);
		}
		@Override
		public void callback(String response) {
			Log.i(TAG, "callback: " + response);
		}
	}


	//----------------------------
	// VideoStatus String for view
	//----------------------------
	public String getStatusString(){
		if (getLastEventType() == VideoStatusEventType.INCOMING){
			return incomingStatusStr();
		} else {
			return outgoingStatusString();
		}
	}
	
	private String outgoingStatusString(){
		int status = getOutgoingVideoStatus();
		int count = getUploadRetryCount();
		String sfn = shortFirstName();

		switch (status){
		case OutgoingVideoStatus.NEW: 
			return sfn + " n...";
		case OutgoingVideoStatus.QUEUED:
			return sfn + " q...";
		case OutgoingVideoStatus.UPLOADING:
			if (count > 0){
				return sfn + " r" + count + "...";
			} else {
				return sfn + " u...";
			}
		case OutgoingVideoStatus.UPLOADED:
			return sfn + " .s..";
		case OutgoingVideoStatus.DOWNLOADED:
			return sfn + " ..p.";
		case OutgoingVideoStatus.VIEWED:
			return sfn + " v!";
		case OutgoingVideoStatus.FAILED_PERMANENTLY:
			return sfn + " e!";
		} 
		return get(Attributes.FIRST_NAME);
	}

	private String incomingStatusStr() {
		int status = getIncomingVideoStatus();
		int count = getDownloadRetryCount();

		switch (status){
		case IncomingVideoStatus.NEW:
			return "Downloading n";
		case IncomingVideoStatus.QUEUED:
			return "Downloading q";
		case IncomingVideoStatus.DOWNLOADING:
			if (count > 0){
				return "Downloading r" + count; 
			} else {
				return "Downloading...";
			}
		case IncomingVideoStatus.DOWNLOADED:
			return get(Attributes.FIRST_NAME);
		case IncomingVideoStatus.VIEWED:
			return get(Attributes.FIRST_NAME);
		case IncomingVideoStatus.FAILED_PERMANENTLY:
			return "Downloading e!";
		}
		return get(Attributes.FIRST_NAME);
	}

	private String shortFirstName(){
		String fn = get(Attributes.FIRST_NAME);
		int shortLen = Math.min(7, fn.length());
		return fn.substring(0, shortLen);	
	}

}
