package com.noplanbees.tbm;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.Log;

import com.google.gson.internal.LinkedTreeMap;
import com.noplanbees.tbm.dispatch.Dispatch;
import com.noplanbees.tbm.model.ActiveModelsHandler;
import com.noplanbees.tbm.network.Server;
import com.noplanbees.tbm.utilities.StringUtils;


// GARF: note that since the register context is called in HomeActivity onCreate (boot) it is out of the loop of versionHandler which is
// currently setup in onResume in HomeActivity. If we have an obsolete version of registerActivity that prevents us from getting to onResume in the HomeActivity then 
// app will fail and user will not be notified of need to update. When register is finalized we need to be sure handle version there as well somehow.
// As well as for other activities in the app.
public class VersionHandler {
	private final String TAG = getClass().getSimpleName();

    public interface Callback{
        void showVersionHandlerDialog(String message, boolean negativeButton);
    }

	private Context context;

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
	
	public VersionHandler(Context context){
        if(!(context instanceof Callback))
            throw new IllegalStateException("Can't cast to VersionHandler.Callback");
		this.context = context;
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
	
	private class CheckVersionCompatibility extends Server {
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
			info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
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
			ActiveModelsHandler.getInstance(context).destroyAll();
			showVersionHandlerDialog("Your " + Config.appName + " app is obsolete. Please update.", false);
		} else if (VersionHandler.updateRequired(result)) {
			showVersionHandlerDialog("Your " + Config.appName + " app is obsolete. Please update.", false);
		} else if (VersionHandler.updateOptional(result)) {
			showVersionHandlerDialog("Your " + Config.appName + " app is out of date. Please update.", true);
		} else if (!VersionHandler.current(result)){
			Dispatch.dispatch("Version compatibilityCheckCallback: Unknow result: " + result.toString());
		}
	}
	
	private void showVersionHandlerDialog(String message, Boolean isNegativeButton){
        Callback callback = (Callback) context;
        callback.showVersionHandlerDialog(message, isNegativeButton);
	}

}
