package com.noplanbees.tbm;

import java.io.File;
import java.util.regex.Pattern;

import android.os.Environment;
import android.util.Log;

public class Config {

	public final static String serverUri = "http://192.168.1.82:3000";
	private static File videoDir = null;

	public static File getVideoDir() {
		if (videoDir !=null) 
			return videoDir;

		String TAG = "getOutputMediaDir";
		File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),"tbm");
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				Log.e(TAG, "Failed to create storage directory.");
				return null;
			}
		}
		videoDir = dir;		
		return videoDir;
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
}
