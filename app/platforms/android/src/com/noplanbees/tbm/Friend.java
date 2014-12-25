package com.noplanbees.tbm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.io.FileUtils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.util.Log;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import com.noplanbees.tbm.network.FileDownloadService;
import com.noplanbees.tbm.network.FileUploadService;
import com.noplanbees.tbm.network.FileTransferService;

public class Friend extends ActiveModel{

public static interface VideoStatusChangedCallback{
	public void onVideoStatusChanged(Friend friend);
}
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

	public static class Attributes{
		public static final String ID  = "id";
		public static final String MKEY  = "mkey";
		public static final String FIRST_NAME  = "firstName";
		public static final String LAST_NAME  = "lastName";
		public static final String OUTGOING_VIDEO_ID = "outgoingVideoId";
		public static final String OUTGOING_VIDEO_STATUS  = "outgoingVideoStatus";
		public static final String UPLOAD_RETRY_COUNT  = "uploadRetryCount";
		public static final String LAST_VIDEO_STATUS_EVENT_TYPE = "lastVideoStatusEventType";
		public static final String TIME_OF_LAST_ACTION = "timeOfLastAction";
		public static final String MOBILE_NUMBER = "mobileNumber";
		public static final String HAS_APP = "hasApp";
	}

	public static class VideoStatusEventType{
		public static final Integer OUTGOING = 0;
		public static final Integer INCOMING = 1;
	}

	@Override
	public String[] attributeList() {
		final String[] a = {	
				Attributes.ID, 
				Attributes.MKEY,
				Attributes.FIRST_NAME, 
				Attributes.LAST_NAME, 
				Attributes.OUTGOING_VIDEO_ID,
				Attributes.OUTGOING_VIDEO_STATUS,
				Attributes.UPLOAD_RETRY_COUNT,
				Attributes.LAST_VIDEO_STATUS_EVENT_TYPE,
				Attributes.TIME_OF_LAST_ACTION,
				Attributes.MOBILE_NUMBER,
				Attributes.HAS_APP};
		return a;
	}

	@Override
	public void init(Context context) {
		super.init(context);
		setOutgoingVideoStatus(OutgoingVideoStatus.NONE);
		set(Attributes.LAST_VIDEO_STATUS_EVENT_TYPE, VideoStatusEventType.INCOMING.toString());
		setUploadRetryCount(0);
	}

	//-------
	// HasApp
	//-------
	public boolean hasApp() {
		if (get(Attributes.HAS_APP).equals("true"))
			return true;
		else
			return false;
	}

	public void setHasApp(){
		set(Attributes.HAS_APP, "true");
	}
	
	//------------
	// PhoneNumber
	//------------
	public PhoneNumber getPhoneNumber() {
		PhoneNumberUtil pu = PhoneNumberUtil.getInstance();
		PhoneNumber pn = null;
		try {
			pn = pu.parse(get(Attributes.MOBILE_NUMBER), UserFactory.current_user().getRegion());
		} catch (NumberParseException e) {
			Log.e(TAG, "ERROR: Could not get phone nubmer object for friends phone this should never happen.");
		}
		return pn;
	}

	//----------------
	// Incoming Videos
	//----------------
	public ArrayList<Video>incomingVideos(){
		VideoFactory vf = VideoFactory.getFactoryInstance();
		return vf.allWithFriendId(getId());
	}

	public ArrayList<Video>sortedIncomingVideos(){
		ArrayList<Video> vids = incomingVideos();
		Collections.sort(vids, new Video.VideoTimestampComparator());
		return vids;
	}

	public Video oldestIncomingVideo(){
		ArrayList<Video> vids = sortedIncomingVideos();
		if (vids.isEmpty()){
			return null;
		} 
		return sortedIncomingVideos().get(0);
	}

	public Video newestIncomingVideo(){
		ArrayList <Video> vids = sortedIncomingVideos();
		Video v = null;
		if (!vids.isEmpty())
			v = vids.get(vids.size() -1);
		return v;
	}

	public Boolean hasIncomingVideoId(String videoId){
		for (Video v : incomingVideos()){
			if (v.getId().equals(videoId))
				return true;
		}
		return false;
	}

	public void createIncomingVideo(Context context, String videoId){
		if (hasIncomingVideoId(videoId))
			return;
		VideoFactory vf = VideoFactory.getFactoryInstance();
		Video v = vf.makeInstance(context);
		v.set(Video.Attributes.FRIEND_ID, getId());
		v.set(Video.Attributes.ID, videoId);
	}

	public void deleteVideo(String videoId){
		// Delete videoFile
		videoFromFile(videoId).delete();
		// Delete thumbFile
		thumbFile(videoId).delete();
		// Delete video object
		VideoFactory vf = VideoFactory.getFactoryInstance();
		vf.delete(videoId);
	}

	public void deleteAllViewedVideos(){
		for (Video v : incomingVideos()){
			if (v.getIncomingVideoStatus() == Video.IncomingVideoStatus.VIEWED)
				deleteVideo(v.getId());
		}
	}


	//----------------
	// Video and thumb
	//----------------
	public String videoFromPath(String videoId){
		return Config.homeDirPath(context) + "/vid_from_" + getId() + "_" + videoId + ".mp4";		
	}

	public File videoFromFile(String videoId){
		return new File(videoFromPath(videoId));
	}

	public String firstPlayableVideoId(){
		String vid = null;
		for (Video v : sortedIncomingVideos()){
			if (videoFromFile(v.getId()).exists()){
				vid = v.getId();
				break;
			}
		}
		return vid;
	}

	public String nextPlayableVideoId(String videoId){
		Boolean found = false;
		for (Video v : sortedIncomingVideos()){
			if (found && videoFromFile(v.getId()).exists()){
				return v.getId();
			}
			if (v.getId().equals(videoId))
				found = true;
		}
		return null;
	}

	public String videoToPath(){
		return Config.homeDirPath(context) + "/vid_to_" + getId() + ".mp4";		
	}

	public File videoToFile(){
		return new File(videoToPath());
	}

	public String thumbPath(String videoId){
		return Config.homeDirPath(context) + "/thumb_from_" + getId() + "_" + videoId + ".mp4";		
	}

	public File thumbFile(String videoId){
		return new File(thumbPath(videoId));		
	}

	public File lastThumbFile(){
		File f = null;
		for (Video v: sortedIncomingVideos()){
			if (thumbFile(v.getId()).exists()){
				f = thumbFile(v.getId());
			}
		}
		return f;
	}

	public Bitmap thumbBitmap(String videoId){
		return Convenience.bitmapWithFile(thumbFile(videoId));
	}

	public Bitmap lastThumbBitmap(){
		Bitmap b = null;
		File f = lastThumbFile();
		if (f != null)
			b = Convenience.bitmapWithFile(f);
		return b; 
	}

	@SuppressLint("NewApi")
	public Bitmap sqThumbBitmap(String videoId){
		Bitmap sq = null;
		Bitmap thumbBmp = thumbBitmap(videoId);
		if (thumbBmp != null){
			sq = ThumbnailUtils.extractThumbnail(thumbBmp, thumbBmp.getWidth(), thumbBmp.getWidth());
			Log.i(TAG, "sqThumbBitmap: size = " + ((Integer) sq.getByteCount()).toString());
		}
		return sq;
	}

	public Boolean thumbExists(){
		Boolean r = false;
		for (Video v : incomingVideos()){
			if (thumbFile(v.getId()).exists()){
				r = true;
				break;
			}
		}
		return r;
	}

	public Boolean incomingVideoNotViewed(){
		// Return true if any of the incoming videos are status DOWNLOADED
		Boolean r = false;
		for (Video v : incomingVideos()){
			if (v.getIncomingVideoStatus() == Video.IncomingVideoStatus.DOWNLOADED){
				r = true;
				break;
			}
		}
		return r;
	}

	public int incomingVideoNotViewedCount(){
		// Return true if any of the incoming videos are status DOWNLOADED
		int i =0;
		for (Video v : incomingVideos()){
			if (v.getIncomingVideoStatus() == Video.IncomingVideoStatus.DOWNLOADED){
				i++;
			}
		}
		return i;
	}

	public void setIncomingVideoViewed(String videoId){
		if (!hasIncomingVideoId(videoId)){
			Log.e(TAG, "setIncomingVideoViewed: ERROR: incoming video doesnt exist");
			return;
		}

		Video v = (Video) VideoFactory.getFactoryInstance().find(videoId);
		v.setIncomingVideoStatus(Video.IncomingVideoStatus.VIEWED);
	}

	//-------------
	// Create Thumb
	//-------------
	public synchronized void createThumb(String videoId){
		Log.i(TAG, "createThumb for friend=" + get(Attributes.FIRST_NAME));

		if( !videoFromFile(videoId).exists() || videoFromFile(videoId).length() == 0 ){
			Log.e(TAG, "createThumb: no video file found for friend=" + get(Attributes.FIRST_NAME));
			return;
		}

		String vidPath = videoFromPath(videoId);
		MediaMetadataRetriever r = new MediaMetadataRetriever();
		r.setDataSource(vidPath);
		// Get the last frame.
		Bitmap thumb = r.getFrameAtTime(240*1000000, MediaMetadataRetriever.OPTION_CLOSEST);
		File thumbFile = thumbFile(videoId);
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
		set(Attributes.OUTGOING_VIDEO_ID, VideoIdUtils.generateId());
	}

	public void uploadVideo(){
		Log.i(TAG, "uploadVideo. For friend=" + get(Attributes.FIRST_NAME));

		setAndNotifyOutgoingVideoStatus(OutgoingVideoStatus.QUEUED);
		setOutGoingVideoId();

		Intent i = new Intent(context, FileUploadService.class);
		i.putExtra(FileTransferService.IntentFields.ID_KEY, getId());
		i.putExtra(FileTransferService.IntentFields.FILE_PATH_KEY, videoToPath());
		i.putExtra(FileTransferService.IntentFields.VIDEO_ID_KEY, get(Attributes.OUTGOING_VIDEO_ID));
		Bundle params = new Bundle();
		params.putString("filename", RemoteStorageHandler.outgoingVideoRemoteFilename(this));
		i.putExtra(FileTransferService.IntentFields.PARAMS_KEY, params);
		context.startService(i);
	}

	public void downloadVideo(Intent intent){
		String videoId = intent.getStringExtra(FileTransferService.IntentFields.VIDEO_ID_KEY);
		Log.i(TAG, "downloadVideo. friend=" + get(Attributes.FIRST_NAME) + " videoId=" + videoId);

		setAndNotifyIncomingVideoStatus(videoId, Video.IncomingVideoStatus.QUEUED);

		Intent i = new Intent(context, FileDownloadService.class);
		i.putExtra(FileTransferService.IntentFields.ID_KEY, getId());
		i.putExtra(FileTransferService.IntentFields.VIDEO_ID_KEY, videoId);
		i.putExtra(FileTransferService.IntentFields.FILE_PATH_KEY, videoFromPath(videoId));
		Bundle params = new Bundle();
		params.putString("filename", RemoteStorageHandler.incomingVideoRemoteFilename(this, videoId));
		i.putExtra(FileTransferService.IntentFields.PARAMS_KEY, params);
		context.startService(i);
	}


	//=============
	// Video Status 
	//=============

	//-----------------------------------------
	// Changes to status and local notification.
	//------------------------------------------


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
			FriendFactory.getFactoryInstance().notifyStatusChanged(this);
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
			FriendFactory.getFactoryInstance().notifyStatusChanged(this);
		}
	}

	// Incoming video status
	public void setAndNotifyIncomingVideoViewed(String videoId) {
		setAndNotifyIncomingVideoStatus(videoId, Video.IncomingVideoStatus.VIEWED);
	}

	public void setAndNotifyIncomingVideoStatus(String videoId, int status){
		setHasApp(); // If we have gotten a video from a friend then he has the app.
		Video v = (Video) VideoFactory.getFactoryInstance().find(videoId);
		if (v == null){
			Log.e(TAG, "setAndNotifyIncomingVideoStatus: ERROR: incoming video doesnt exist");
			return;
		}

		if (v.getIncomingVideoStatus() != status){
			v.setIncomingVideoStatus(status);
			if (status == Video.IncomingVideoStatus.VIEWED)
				notifyServerVideoViewed(videoId);

			// Only notify the UI of changes in status to the last incoming video.
			if (newestIncomingVideo().getId().equals(videoId)){
				setLastEventTypeIncoming();
				FriendFactory.getFactoryInstance().notifyStatusChanged(this);
			}
		}
	}

	// Download retryCount
	//	private void setDownloadRetryCount(int retryCount){
	//		set(Attributes.DOWNLOAD_RETRY_COUNT, ((Integer) retryCount).toString());
	//	}
	//
	//	private int getDownloadRetryCount(){
	//		return Integer.parseInt(get(Attributes.DOWNLOAD_RETRY_COUNT));
	//	}

	private void setAndNotifyDownloadRetryCount(String videoId, int retryCount){
		Video v = (Video) VideoFactory.getFactoryInstance().find(videoId);
		if (v == null){
			Log.e(TAG, "setAndNotifyIncomingVideoStatus: ERROR: incoming video doesnt exist");
			return;
		}

		if (v.getDownloadRetryCount() != retryCount){
			v.setDownloadRetryCount(retryCount);

			// Only notify the UI of changes in retry count of the last incoming video.
			if (newestIncomingVideo().getId().equals(videoId)){
				setLastEventTypeIncoming();
				FriendFactory.getFactoryInstance().notifyStatusChanged(this);
			}
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
		String videoId = intent.getStringExtra(FileTransferService.IntentFields.VIDEO_ID_KEY);

		if (transferType.equals(FileTransferService.IntentFields.TRANSFER_TYPE_DOWNLOAD)){
			setAndNotifyIncomingVideoStatus(videoId, status);
			setAndNotifyDownloadRetryCount(videoId, retryCount);
		} else if (transferType.equals(FileTransferService.IntentFields.TRANSFER_TYPE_UPLOAD)){
			if (videoId.equals(get(Attributes.OUTGOING_VIDEO_ID))){
				setAndNotifyOutgoingVideoStatus(status);
				setAndNotifyUploadRetryCount(retryCount);
			}
		} else {
			Log.e(TAG, "ERROR: updateStatus: unknown TransferType passed in intent. This should never happen.");
			throw new RuntimeException();
		}
	}

	//-------------------------------
	// Server notification of changes
	//-------------------------------
	private void notifyServerVideoViewed(String videoId) {
		Log.i(TAG, "notifyServerVideoViewed");
		// Update kv store
		// Send notification
		NotificationHandler.sendForVideoStatusUpdate(this, videoId, NotificationHandler.StatusEnum.VIEWED);
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
		Video v = newestIncomingVideo();
		if (v == null)
			return get(Attributes.FIRST_NAME);

		int status = v.getIncomingVideoStatus();
		int count = v.getDownloadRetryCount();

		switch (status){
		case Video.IncomingVideoStatus.NEW:
			return "Downloading n";
		case Video.IncomingVideoStatus.QUEUED:
			return "Downloading q";
		case Video.IncomingVideoStatus.DOWNLOADING:
			if (count > 0){
				return "Downloading r" + count; 
			} else {
				return "Downloading...";
			}
		case Video.IncomingVideoStatus.DOWNLOADED:
			return get(Attributes.FIRST_NAME);
		case Video.IncomingVideoStatus.VIEWED:
			return get(Attributes.FIRST_NAME);
		case Video.IncomingVideoStatus.FAILED_PERMANENTLY:
			return "Downloading e!";
		}
		return get(Attributes.FIRST_NAME);
	}

	private String shortFirstName(){
		String fn = get(Attributes.FIRST_NAME);
		int shortLen = Math.min(7, fn.length());
		return fn.substring(0, shortLen);	
	}
	
	public int getIncomingVideoStatus(){
		Video v = newestIncomingVideo();
		if (v == null)
			return -1;
		return v.getIncomingVideoStatus();
	}
	
	public String fullName() {
		return get(Attributes.FIRST_NAME) + " " + get(Attributes.LAST_NAME);
	}





}
