package com.zazoapp.s3networktest;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import com.zazoapp.s3networktest.dispatch.Dispatch;

import java.io.File;
import java.util.regex.Pattern;

public class Config {
    private static final String TAG = Config.class.getSimpleName();

//    public static final String SERVER_HOST = "192.168.1.82";
//    public static final int SERVER_PORT = 3000;
//    public static final String SERVER_URI = "http://"+SERVER_HOST+":" + SERVER_PORT;

    private static final String STAGE_HOST = "staging.zazoapp.com";
    private static final String STAGE_URI = "http://" + STAGE_HOST;
    private static final String SERVER_HOST = "prod.zazoapp.com";
    private static final String SERVER_URI = "http://" + SERVER_HOST;

    public static final String appName = "Zazo";
    public static final String landingPageUrl = "zazoapp.com/l/";

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

    public static File recordingFile(Context context) {
        File recordingFile = getBrokenVideoFile(context);
        return recordingFile;
    }

    public static String getServerHost() {
        return STAGE_HOST;
    }

    public static String getServerUri() {
        return STAGE_URI;
    }

    private static final String BROKEN_VIDEO_NAME = "test_video_broken.mp4";

    public static File getBrokenVideoFile(Context context) {
        AssetManager am = context.getAssets();
        String path = Config.homeDirPath(context) + BROKEN_VIDEO_NAME;
        try {
            return Convenience.createFileFromAssets(am, path, BROKEN_VIDEO_NAME);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
