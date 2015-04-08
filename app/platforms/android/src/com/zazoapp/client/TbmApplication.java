package com.zazoapp.client;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.dispatch.TbmTracker;
import com.zazoapp.client.model.ActiveModelsHandler;
import com.zazoapp.client.ui.helpers.UnexpectedTerminationHelper;

public class TbmApplication extends Application {

	protected static final String TAG = "TbmApplication";

	private static TbmApplication application;

    private UnexpectedTerminationHelper unexpectedTerminationHelper = new UnexpectedTerminationHelper();

	private int foreground;

	public static TbmApplication getInstance(){
		return application;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
        Log.d(TAG, "onCreate");
        application = this;
        unexpectedTerminationHelper.init();
        DebugConfig.getInstance(this);
        loadDataModel();

        Dispatch.registerTracker(this, new TbmTracker());
        Dispatch.dispatchStored();
        startService(new Intent(this, DataHolderService.class));

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
			@Override
			public void onActivityStopped(Activity activity) {
				setForeground(false);
			}
			
			@Override
			public void onActivityStarted(Activity activity) {
				setForeground(true);				
			}
			@Override
			public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
			@Override
			public void onActivityResumed(Activity activity) {}
			@Override
			public void onActivityPaused(Activity activity) {
                // TODO temporary until we fix storing model on its update
                ActiveModelsHandler model = ActiveModelsHandler.getInstance(TbmApplication.this);
                if (model != null) {
                    model.saveAll();
                }
            }
			@Override
			public void onActivityDestroyed(Activity activity) {}
			@Override
			public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
		});
	}

    private void setForeground(boolean isForeground){
		if(isForeground)
			foreground++;
		else
			foreground--;
	}
	
	public boolean isForeground(){
		return foreground>0;
	}

    public void addTerminationCallback(UnexpectedTerminationHelper.TerminationCallback callback) {
        unexpectedTerminationHelper.addTerminationCallback(callback);
    }

    private void loadDataModel() {
        ActiveModelsHandler models = ActiveModelsHandler.getInstance(this);
        models.ensureAll();
        GridManager.getInstance().initGrid(this);
    }

    public static String getVersion() {
        String version = "x.x.x";
        if (application != null) {
            try {
                version = application.getPackageManager().getPackageInfo(application.getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return version;
    }

    public static String getVersionNumber() {
        int version = 1;
        if (application != null) {
            try {
                version = application.getPackageManager().getPackageInfo(application.getPackageName(), 0).versionCode;
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return String.valueOf(version);
    }
}
