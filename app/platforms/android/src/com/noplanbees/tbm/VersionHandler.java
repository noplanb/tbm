package com.noplanbees.tbm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.Log;

import com.google.gson.internal.LinkedTreeMap;

interface VersionHandlerInterface{
	public void compatibilityCheckCallback(String result);
}

// GARF: note that since the register activity is called in HomeActivity onCreate (boot) it is out of the loop of versionHandler which is
// currently setup in onResume in HomeActivity. If we have an obsolete version of registerActivity that prevents us from getting to onResume in the HomeActivity then 
// app will fail and user will not be notified of need to update. When register is finalized we need to be sure handle version there as well somehow.
// As well as for other activities in the app.
public class VersionHandler {
	private final String TAG = getClass().getSimpleName();
	
	private Activity activity;
	private VersionHandlerInterface vciDelegate;
	
	public static class Responses{
		public static final String UPDATE_SCHEMA_REQUIRED = "update_schema_required";
		public static final String UPDATE_REQUIRED = "update_required";
		public static final String UPDATE_OPTIONAL = "update_optional";
		public static final String CURRENT = "current";
	}
	
	public static boolean update_schema_required(String result) {
		return result.equalsIgnoreCase(VersionHandler.Responses.UPDATE_SCHEMA_REQUIRED);
	}

	public static boolean update_required(String result) {
		return result.equalsIgnoreCase(VersionHandler.Responses.UPDATE_REQUIRED);
	}

	public static boolean update_optional(String result) {
		return result.equalsIgnoreCase(VersionHandler.Responses.UPDATE_OPTIONAL);
	}
	
	public static boolean current(String result) {
		return result.equalsIgnoreCase(VersionHandler.Responses.CURRENT);
	}
	
	public static void goToPlayStore(Context context){
		try {
		    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + context.getPackageName())));
		} catch (android.content.ActivityNotFoundException anfe) {
		    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + context.getPackageName())));
		}
	}
	
	// Not used
	public static void uninstallApp(Context context){
		Uri packageURI = Uri.parse("package:" + context.getPackageName());
		Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
		context.startActivity(uninstallIntent);
	}
	
	public VersionHandler(Activity a){
		activity = a;
		vciDelegate = (VersionHandlerInterface) a;
		checkVersionCompatibility();
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
