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
			Log.e(TAG, "download2: IOException: " + e.getMessage());
		}
	}

	public static class BgDownload extends AsyncTask<String, Void, Void>{
		@Override
		protected Void doInBackground(String... params) {
			download(params[0], params[1]);
			return null;
		}
	}

	public static synchronized void downloadFromFriendId(String friendId){
		String sUrl = Config.fullUrl("/videos/get?user_id=" + friendId + "&receiver_id=" + UserFactory.current_user().get("id"));
		String dfp = Config.downloadFilePath();
		download(sUrl, dfp);
		File f = new File(dfp);
		String lfffp = Config.videoPathForFriendId(friendId);
		f.renameTo(new File(lfffp));
		Log.i(TAG, "downloadFromFriendId: downloaded file for friend=" + friendId + " placed in " + lfffp);
		createThumb(friendId);
	}

	public static synchronized void createThumb(String friendId){
		Log.i(TAG, "createThumb for friend=" + friendId);
		String vidPath = Config.videoPathForFriendId(friendId);
		Bitmap thumb = ThumbnailUtils.createVideoThumbnail(vidPath, MediaStore.Images.Thumbnails.MINI_KIND);
		File thumbFile = Config.thumbFileForFriendId(friendId);
		try {
			FileOutputStream fos = FileUtils.openOutputStream(thumbFile);
			thumb.compress(Bitmap.CompressFormat.PNG, 100, fos);
		} catch (IOException e) {
			Log.e(TAG, "createThumb: IOException " + e.getMessage());
		}
	}

	public static class BgDownloadFromFriendId extends AsyncTask<String, Void, Void>{
		@Override
		protected Void doInBackground(String... params) {
			downloadFromFriendId(params[0]);
			return null;
		}
	}

}
