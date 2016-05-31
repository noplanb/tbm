package com.zazoapp.client;

import android.content.Context;
import android.os.Environment;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.debug.DebugUtils;
import com.zazoapp.client.dispatch.Dispatch;

import java.io.File;
import java.util.regex.Pattern;

public class Config {
    private static final String TAG = Config.class.getSimpleName();

//    public static final String SERVER_HOST = "192.168.1.82";
//    public static final int SERVER_PORT = 3000;
//    public static final String SERVER_URI = "http://"+SERVER_HOST+":" + SERVER_PORT;
    public static final String APP_DOMAIN = "zazoapp.com";
    private static final String STAGE_HOST = "staging." + APP_DOMAIN;
    private static final String STAGE_URI = "http://" + STAGE_HOST;
    private static final String SERVER_HOST = "prod." + APP_DOMAIN;
    private static final String SERVER_URI = "http://" + SERVER_HOST;

    public static final String appName = "Zazo";
    public static final String legacyLandingPageUrl = APP_DOMAIN + "/l/";
    public static final String landingPageUrl = APP_DOMAIN + "/c/";

    private static File publicDir = null;

    public static int getMinVideoSize() {
        return 2000;
    }

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

    public static String fullUrl(String uri) {
        return fullUrl(getServerUri(), uri);
    }

    public static String fullUrl(String serverUri, String uri){

        String slash = "";
        if(uri.charAt(0) != '/')
            slash = "/";

        String url;
        if (Pattern.compile("http:").matcher(uri).find()){
            url = uri;
        } else if (Pattern.compile("https:").matcher(uri).find()){
            url = uri;
        } else {
            url = serverUri + slash + uri;
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
        File recordingFile = null;
        if (DebugConfig.Bool.SEND_BROKEN_VIDEO.get()) {
            recordingFile = DebugUtils.getBrokenVideoFile(context);
        }
        if (recordingFile == null) {
            recordingFile = new File(recordingFilePath(context));
        }
        return recordingFile;
    }

    public static String getServerHost() {
        if (DebugConfig.Bool.USE_CUSTOM_SERVER.get()) {
            if (DebugConfig.Str.CUSTOM_HOST.get().isEmpty()) {
                return STAGE_HOST;
            } else {
                return DebugConfig.Str.CUSTOM_HOST.get();
            }
        }
        return SERVER_HOST;
    }

    public static String getServerUri() {
        if (DebugConfig.Bool.USE_CUSTOM_SERVER.get()) {
            if (DebugConfig.Str.CUSTOM_HOST.get().isEmpty()) {
                return STAGE_URI;
            } else {
                return DebugConfig.Str.CUSTOM_URI.get();
            }
        }
        return SERVER_URI;
    }


}
