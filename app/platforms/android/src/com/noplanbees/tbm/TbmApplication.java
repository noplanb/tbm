package com.noplanbees.tbm;

import android.app.Application;
import android.content.Intent;

public class TbmApplication extends Application {

	private static TbmApplication application;
	
	private int foreground;
	
	public static TbmApplication getInstance(){
		return application;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		application = this;
		startService(new Intent(this, DataHolderService.class));
	}
	
	public void setForeground(boolean isForeground){
		if(isForeground)
			foreground++;
		else
			foreground--;
	}
	
	public boolean isForeground(){
		return foreground>0;
	}
}
