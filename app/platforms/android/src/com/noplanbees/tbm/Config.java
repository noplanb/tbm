package com.noplanbees.tbm;

import java.io.File;

import android.os.Environment;
import android.util.Log;

public class Config extends ActiveModel{
    
	public final static String serverUri = "http://192.168.1.82:3000";
	
	private static File videoDir = null;
	
	
	@Override
	public String[] attributeList() {
      final String[] a = {"id", "firstName", "lastName", "registered"};
      return a;
	}
	
	public static boolean isRegistered(){
		ConfigFactory cf = ConfigFactory.getFactoryInstance();
		return cf.instances.get(0) != null && cf.instances.get(0).get("registered") != null;
	}
	
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
