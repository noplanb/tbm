package com.noplanbees.tbm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.Log;

import com.google.gson.internal.LinkedTreeMap;
import com.noplanbees.tbm.crash_dispatcher.Dispatch;


// GARF: note that since the register activity is called in HomeActivity onCreate (boot) it is out of the loop of versionHandler which is
// currently setup in onResume in HomeActivity. If we have an obsolete version of registerActivity that prevents us from getting to onResume in the HomeActivity then 
// app will fail and user will not be notified of need to update. When register is finalized we need to be sure handle version there as well somehow.
// As well as for other activities in the app.
public class VersionHandler {
	private final String TAG = getClass().getSimpleName();
	
	private Activity activity;
	
	private static class ParamKeys{
		public static final String RESULT_KEY = "result";
	}
	
	public static class Responses{
		public static final String UPDATE_SCHEMA_REQUIRED = "update_schema_required";
		public static final String UPDATE_REQUIRED = "update_required";
		public static final String UPDATE_OPTIONAL = "update_optional";
		public static final String CURRENT = "current";
	}
	
	public static boolean updateSchemaRequired(String result) {
		return result.equalsIgnoreCase(VersionHandler.Responses.UPDATE_SCHEMA_REQUIRED);
	}

	public static boolean updateRequired(String result) {
		return result.equalsIgnoreCase(VersionHandler.Responses.UPDATE_REQUIRED);
	}

	public static boolean updateOptional(String result) {
		return result.equalsIgnoreCase(VersionHandler.Responses.UPDATE_OPTIONAL);
	}
	
	public static boolean current(String result) {
		return result.equalsIgnoreCase(VersionHandler.Responses.CURRENT);
	}
	
	// Not used
	public static void uninstallApp(Context context){
		Uri packageURI = Uri.parse("package:" + context.getPackageName());
		Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
		context.startActivity(uninstallIntent);
	}
	
	public VersionHandler(Activity a){
		activity = a;
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
			super(uri, params, false);
		}
		@Override
		public void success(String response) {
			String result = StringUtils.linkedTreeMapWithJson(response).get(VersionHandler.ParamKeys.RESULT_KEY);
		    handleCompatibilityResult(result);
		}
		@Override
		public void error(String errorString) {	
			Dispatch.dispatch("checkCompatibility: ERROR: " + errorString);
		}	
	}
	
	private PackageInfo packageInfo(){
		PackageInfo info = new  PackageInfo();
		try {
			info = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			Dispatch.dispatch("version: ERROR: " + e.toString());
		}
		return info;
	}
	
	//---
	// UI
	//---
	public void handleCompatibilityResult(String result) {
		Log.i(TAG, "compatibilityCheckCallback: " + result);
		if (VersionHandler.updateSchemaRequired(result)) {
			ActiveModelsHandler.getInstance(activity).destroyAll();
			showVersionHandlerDialog("Your " + Config.appName + " app is obsolete. Please update.", false);
		} else if (VersionHandler.updateRequired(result)) {
			showVersionHandlerDialog("Your " + Config.appName + " app is obsolete. Please update.", false);
		} else if (VersionHandler.updateOptional(result)) {
			showVersionHandlerDialog("Your " + Config.appName + " app is out of date. Please update.", true);
		} else if (!VersionHandler.current(result)){
			Dispatch.dispatch("Version compatibilityCheckCallback: Unknow result: " + result.toString());
		}
	}
	
	private void showVersionHandlerDialog(String message, Boolean negativeButton){
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle("Update Available")
		.setMessage(message)
		.setPositiveButton("Update", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				goToPlayStore();
				activity.finish();
			}
		});
		
		if (negativeButton){
			builder.setNegativeButton("Later", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
				}
			});
		}
		AlertDialog alertDialog = builder.create();
		alertDialog.setCanceledOnTouchOutside(false);
		alertDialog.show();
	}
	
	private void goToPlayStore(){
		try {
		    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + activity.getPackageName())));
		} catch (android.content.ActivityNotFoundException anfe) {
		    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + activity.getPackageName())));
		}
	}
}
