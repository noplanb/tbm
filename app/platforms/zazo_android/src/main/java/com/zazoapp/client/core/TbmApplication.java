package com.zazoapp.client.core;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.dispatch.RollbarTracker;
import com.zazoapp.client.dispatch.ZazoAnalytics;
import com.zazoapp.client.model.ActiveModelsHandler;
import com.zazoapp.client.model.GridManager;
import com.zazoapp.client.ui.LockScreenAlertActivity;
import com.zazoapp.client.ui.ZazoManagerProvider;
import com.zazoapp.client.ui.helpers.UnexpectedTerminationHelper;

public class TbmApplication extends Application {

    private static final String TAG = TbmApplication.class.getSimpleName();

    private static TbmApplication application;

    private ActiveModelsHandler activeModelsHandler;

    private UnexpectedTerminationHelper unexpectedTerminationHelper = new UnexpectedTerminationHelper();

    private int foreground;

    private ZazoManagerProvider managerProvider;

    public static TbmApplication getInstance() {
        return application;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        application = this;
        unexpectedTerminationHelper.init();
        DebugConfig.init(this);
        ZazoAnalytics.init(this);
        Settings.init(this);
        loadDataModel();

        Dispatch.registerTracker(this, new RollbarTracker());
        Dispatch.dispatchStored();

        initPrefs();
        IntentHandlerService.onApplicationStart();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStopped(Activity activity) {
                if (!(activity instanceof LockScreenAlertActivity)) {
                    setForeground(false);
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {
                if (!(activity instanceof LockScreenAlertActivity)) {
                    setForeground(true);
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
                activeModelsHandler.saveAll();
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }
        });
	}

    private void initPrefs() {
        PreferencesHelper prefs = new PreferencesHelper(this);
        prefs.putBoolean(VersionHandler.UPDATE_SESSION, true);
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
        activeModelsHandler = ActiveModelsHandler.getInstance(this);
        activeModelsHandler.ensureAll();
        GridManager.getInstance().initGrid(this);
        addTerminationCallback(activeModelsHandler);
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

    public ZazoManagerProvider getManagerProvider() {
        return managerProvider;
    }

    public void initManagerProvider(ZazoManagerProvider provider) {
        managerProvider = provider;
    }

    public static Context getContext() {
        return application;
    }
}
