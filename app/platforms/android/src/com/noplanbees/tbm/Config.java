package com.noplanbees.tbm;

import java.io.File;

import android.os.Environment;
import android.util.Log;

public class Config {
    
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
	
}
