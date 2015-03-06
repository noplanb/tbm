package com.zazoapp.client.utilities;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;

@SuppressLint("NewApi")
public final class AsyncTaskManager{

	
	static public <T> void executeAsyncTask(AsyncTask<T, ?, ?> task, T... params){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
		}else{
			task.execute(params);
		}
	}
}
