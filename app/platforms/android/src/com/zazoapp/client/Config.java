package com.zazoapp.client;

import android.content.Context;
import android.os.Environment;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.dispatch.Dispatch;

import java.io.File;
import java.util.regex.Pattern;

public class Config {
    private static final String TAG = Config.class.getSimpleName();

//	public final static String SERVER_HOST = "192.168.1.82";
//    public static final int SERVER_PORT = 3000;
//    public final static String SERVER_URI = "http://"+SERVER_HOST+":" + SERVER_PORT;

    //private final static String SERVER_HOST = "zazo.10.0.1.5.xip.io";
    //private final static String SERVER_HOST = "zazo-dev1-5.elasticbeanstalk.com";
    private final static String SERVER_HOST = "prod.zazoapp.com";
    private final static String SERVER_URI = "http://" + SERVER_HOST;

	public final static String appName = "Zazo";
    public static final String landingPageUrl = "http://www.zazoapp.com/l/";

    private static File publicDir = null;

    public static final boolean SHOW_RED_DOT = false;

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
			url = getServerUri() + slash + uri;
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

    public static String getServerHost() {
        final DebugConfig config = DebugConfig.getInstance();
        return (config != null && config.shouldUseCustomServer() && !config.getCustomHost().isEmpty()) ? config.getCustomHost() : SERVER_HOST;
    }

    public static String getServerUri() {
        final DebugConfig config = DebugConfig.getInstance();
        return (config != null && config.shouldUseCustomServer() && !config.getCustomHost().isEmpty()) ? config.getCustomUri() : SERVER_URI;
    }
}
