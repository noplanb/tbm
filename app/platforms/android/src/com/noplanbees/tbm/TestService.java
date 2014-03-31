package com.noplanbees.tbm;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class TestService extends NonStopIntentService{
	private final String TAG = this.getClass().getSimpleName();

	public TestService() {
		super("TestService");
		Log.i(TAG, "constructor");
	}

	@Override
	protected void onHandleIntent(Intent intent, int startId) {
		if (intent == null){
			Log.i(TAG, "got null intent");
		} else {
			Bundle extras = intent.getExtras();
			Log.i(TAG, "got intent " + extras.getInt("n"));
			for(int i=0; i<5; i++){
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				Log.i(TAG, "onHandleIntent n=" + extras.getInt("n") + " i=" + i);
			}
		}
		stopSelf(startId);
	}
}
