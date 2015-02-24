package com.noplanbees.tbm;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.noplanbees.tbm.debug.DebugConfig;

public class TbmApplication extends Application {

	protected static final String TAG = "TbmApplication";

	private static TbmApplication application;
	
	private int foreground;
	
	public static TbmApplication getInstance(){
		return application;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate");
        DebugConfig.getInstance(this);
		application = this;
		
		
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
			public void onActivityPaused(Activity activity) {}
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
	
}
