package com.noplanbees.tbm;

import java.io.File;
import java.util.regex.Pattern;

import android.os.Environment;
import android.util.Log;

public class Config {

	public final static String serverUri = "http://192.168.1.91:3000";
//	public final static String serverUri = "http://www.threebyme.com";
	private static File homeDir = null;

	public static File getHomeDir() {
		if (homeDir !=null) 
			return homeDir;

		String TAG = "getOutputMediaDir";
		File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),"tbm");
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				Log.e(TAG, "Failed to create storage directory.");
				return null;
			}
		}
		homeDir = dir;		
		return homeDir;
	}
	
	public static String homeDirPath(){
		return getHomeDir().getPath();
	}

	public static String fullUrl(String uri){

		String slash = "";
		if(uri.charAt(0) != '/')
			slash = "/";

		String url;
		if (Pattern.compile("http:").matcher(uri).find()){
			url = uri;
		} else {
			url = Config.serverUri + slash + uri;
		}
		return url;
	}

	public static String downloadFilePath() {
		return homeDirPath() + "/last_download.mp4";
	}
	
	public static String recordingFilePath(){
		return homeDirPath() + "/new.mp4";
	}
	
	public static File recordingFile(){
		return new File(recordingFilePath());
	}

}
