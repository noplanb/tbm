package com.noplanbees.tbm;

import android.content.Context;
import android.os.Environment;

import com.noplanbees.tbm.dispatch.Dispatch;

import java.io.File;
import java.util.regex.Pattern;

public class Config {
	String TAG = Config.class.getSimpleName();

	
//	public final static String SERVER_HOST = "192.168.1.82";
//    public static final int SERVER_PORT = 3000;
//    public final static String SERVER_URI = "http://"+SERVER_HOST+":" + SERVER_PORT;

  public final static String SERVER_HOST = "zazo-dev1-5.elasticbeanstalk.com";
  public final static String SERVER_URI = "http://"+SERVER_HOST;
	
	public final static String appName = "Zazo";

    private static File publicDir = null;

	public static File getPublicDir() {
		if (publicDir !=null) 
			return publicDir;

		File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),"tbm");
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				Dispatch.dispatch("ERROR: This should never happen. getHomeDir: Failed to create storage directory.");
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
			url = Config.SERVER_URI + slash + uri;
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
