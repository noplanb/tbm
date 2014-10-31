package com.noplanbees.tbm;

import java.io.File;
import java.util.regex.Pattern;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class Config {

	public final static String serverUri = "http://192.168.1.82:3000";
//	public final static String serverUri = "http:s//www.threebyme.com";
	
	public final static String appName = "Zazo";
	
	private static File publicDir = null;

	public static File getPublicDir() {
		if (publicDir !=null) 
			return publicDir;

		String TAG = "getOutputMediaDir";
		File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),"tbm");
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				Log.e(TAG, "ERROR: This should never happen. getHomeDir: Failed to create storage directory.");
				throw new RuntimeException();
			}
		}
		publicDir = dir;		
		return publicDir;
	}
	
	public static String homeDirPath(Context context){
//		return getPublicDir().getAbsolutePath();
		return context.getFilesDir().getAbsolutePath();
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
	
	public static String fileDownloadUrl() {
		return fullUrl("/videos/get");
	}
	
	public static String fileUploadUrl(){
		return fullUrl("/videos/create");
	}

	public static String downloadingFilePath(Context context) {
		return homeDirPath(context) + "/last_download.mp4";
	}
	
	public static String recordingFilePath(Context context){
		return homeDirPath(context) + "/new.mp4";
	}
	
	public static File recordingFile(Context context){
		return new File(recordingFilePath(context));
	}

}
