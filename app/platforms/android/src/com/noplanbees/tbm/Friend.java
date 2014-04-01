package com.noplanbees.tbm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.sax.StartElementListener;
import android.util.Log;
import android.widget.ImageView;
import android.widget.VideoView;

public class Friend extends ActiveModel{

	@Override
	public String[] attributeList() {
      final String[] a = {	"id", 
    		  			  	"viewIndex", 
    		  			  	"frameId", 
    		  			  	"viewId", 
    		  			  	"thumbViewId",
    		  			  	"nameTextId",
    		  			  	"firstName", 
    		  			  	"lastName", 
    		  			  	"videoPath", 
    		  			  	"videoNotViewed",
    		  			  	"sentVideoStatus",
    		  			  	"sentVideoRetryCount"};
      return a;
	}
	
	//-------------------------
	// Video and thumb
	//-------------------------
	public String videoFromPath(){
		return Config.homeDirPath() + "/vid_from_" + getId() + ".mp4";		
	}
	
	public File videoFromFile(){
		return new File(videoFromPath());
	}
	
	public String videoToPath(){
		return Config.homeDirPath() + "/vid_to_" + getId() + ".mp4";		
	}
	
	public File videoToFile(){
		return new File(videoToPath());
	}
	
	public String thumbPath(){
		return Config.homeDirPath() + "/thumb_from_" + getId() + ".mp4";		
	}
	
	public File thumbFile(){
		return new File(thumbPath());		
	}
	
	public Bitmap thumbBitmap(){
        Bitmap bmp = null;
		try {
	        File thumbFile = thumbFile();
	        FileInputStream fis;
			fis = FileUtils.openInputStream(thumbFile);
	        bmp = BitmapFactory.decodeStream(fis);
		} catch (IOException e) {
			Log.i(TAG, "sendNotification: IOException: " + e.getMessage());
		}
		return bmp;
	}
	
	public Bitmap sqThumbBitmap(){
		Bitmap sq = null;
		Bitmap thumbBmp = thumbBitmap();
		if (thumbBmp != null)
			sq = ThumbnailUtils.extractThumbnail(thumbBmp, thumbBmp.getWidth(), thumbBmp.getWidth());
		return sq;
	}
	
	public boolean thumbExists(){
		return thumbFile().exists();
	}
	
	//-------------------------
	// Video download
	//-------------------------
	public void downloadVideo(){
		FileDownload.downloadForFriend(this);
		new VideoStatusHandler().setVideoNotViewed(this);
	}
	
	public void bgDownloadVideo(){
		FileDownload.bgDownloadForFriend(this);
		new VideoStatusHandler().setVideoNotViewed(this);
	}
	
	//-------------------------
	// Video upload
	//-------------------------
	public void uploadVideo(HomeActivity activity){
		Log.i(TAG, "uploadVideo. For friend=" + get("firstName"));
		User user = UserFactory.current_user();
		String receiverId = get("id");

		Intent i = new Intent(activity, FileUploadService.class);
		i.putExtra(VideoStatusHandler.STATUS_KEY, VideoStatusHandler.NEW);
		i.putExtra(VideoStatusHandler.RETRY_COUNT_KEY, 0);
		i.putExtra("filePath", videoToPath());
		i.putExtra("userId", user.get("id"));
		i.putExtra("receiverId", receiverId);
        activity.startService(i);
	}
	
	//-------------------------
	// Display stuff
	//-------------------------
	public VideoView videoView(Activity activity){
		Integer id = Integer.parseInt(get("viewId"));
		return (VideoView) activity.findViewById(id);
	}
	
	public ImageView thumbView(Activity activity){
		Integer id = Integer.parseInt(get("thumbViewId"));
		return (ImageView) activity.findViewById(id);
	}
	

}
