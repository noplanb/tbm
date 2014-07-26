package com.noplanbees.tbm;

import com.google.gson.internal.LinkedTreeMap;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

interface VersionHandlerInterface{
	public void compatibilityCheckCallback(String result);
}

public class VersionHandler {
	private final String TAG = getClass().getSimpleName();
	
	private Activity activity;
	private VersionHandlerInterface vciDelegate;
	
	public VersionHandler(Activity a){
		activity = a;
		vciDelegate = (VersionHandlerInterface) a;
	}
	
	public Integer versionCode(){
		return packageInfo().versionCode;
	}
	
	public String versionName(){
		return packageInfo().versionName;
	}
	
	public void checkVersionCompatibility(){
		LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
		params.put("device_platform", "android");
		params.put("version", versionCode() + "");
		new CheckVersionCompatibility("version/check_compatibility", params, "GET");
	}
	
	private class CheckVersionCompatibility extends Server{
		public CheckVersionCompatibility(String uri, LinkedTreeMap<String, String> params, String method) {
			super(uri, params, method);
		}
		@Override
		public void success(String response) {
			vciDelegate.compatibilityCheckCallback(response);
		}
		@Override
		public void error(String errorString) {	
			Log.e(TAG, "checkCompatibility: ERROR: " + errorString);
		}	
	}
	
	private PackageInfo packageInfo(){
		PackageInfo info = new  PackageInfo();
		try {
			info = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			Log.e(TAG, "version: ERROR: " + e.toString());
		}
		return info;
	}
}
