package com.noplanbees.tbm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

public class FileDownload {
	private final static String TAG = "FileDownload";

	public static void download(String sUrl, String file_path){
		try {
			File f = FileUtils.getFile(file_path);
			URL url = new URL(sUrl);
			FileUtils.copyURLToFile(url, f, 60000, 60000);
		} catch (MalformedURLException e) {
			Log.e(TAG, "download2: MalformedURLException: " + e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, "download2: IOException: " + e.getMessage() + e.toString());
		}
	}

	public static class BgDownload extends AsyncTask<String, Void, Void>{
		@Override
		protected Void doInBackground(String... params) {
			download(params[0], params[1]);
			return null;
		}	
	}

	public static synchronized void downloadForFriend(Friend friend){
		String sUrl = Config.fullUrl("/videos/get?user_id=" + friend.getId() + "&receiver_id=" + UserFactory.current_user().getId());
		String dfp = Config.downloadFilePath();
		download(sUrl, dfp);
		File f = new File(dfp);
		String lfffp = friend.videoFromPath();
		f.renameTo(new File(lfffp));
		Log.i(TAG, "downloadFromFriendId: downloaded file for friend=" + friend.get("firstName") + " placed in " + lfffp);
		createThumb(friend);
	}

	public static synchronized void createThumb(Friend friend){
		Log.i(TAG, "createThumb for friend=" + friend.get("firstName"));
		
		if( !friend.videoFromFile().exists() || friend.videoFromFile().length() == 0 ){
			Log.e(TAG, "createThumb: no video file found for friend=" + friend.get("fristName"));
			return;
		}
		
		String vidPath = friend.videoFromPath();
		Bitmap thumb = ThumbnailUtils.createVideoThumbnail(vidPath, MediaStore.Images.Thumbnails.MINI_KIND);
		File thumbFile = friend.thumbFile();
		try {
			FileOutputStream fos = FileUtils.openOutputStream(thumbFile);
			thumb.compress(Bitmap.CompressFormat.PNG, 100, fos);
		} catch (IOException e) {
			Log.e(TAG, "createThumb: IOException " + e.getMessage());
		}
	}
	
	public static void bgDownloadForFriend(Friend friend){
		new BgDownloadForFriend().execute(friend);
	}
	
	public static class BgDownloadForFriend extends AsyncTask<Friend, Void, Void>{
		@Override
		protected Void doInBackground(Friend... params) {
			downloadForFriend(params[0]);
			return null;
		}
	}
}
