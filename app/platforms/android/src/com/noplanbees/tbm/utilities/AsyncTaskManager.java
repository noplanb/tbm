package com.noplanbees.tbm.utilities;

import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;

@SuppressLint("NewApi")
public final class AsyncTaskManager{

	private static AsyncTaskManager asyncTaskExtensions;
	
	public static enum Pool{NAME_FETCHER,
		NAME_FETCHER_1,
		NAME_FETCHER_2,
		NAME_FETCHER_3,
		NAME_FETCHER_4,
		NAME_FETCHER_5,
		CONNECT, SEND_UNSENT, SYNC_TASK};
	
	private final static int MAX_NAME_FETCHER_COUNT = 5;
	private int currentNameFetchersCount;
	
	private Map<Pool, SerialExecutor> executors;
	private SerialExecutor currentExecutor;

	
	static public <T> void executeAsyncTask(AsyncTask<T, ?, ?> task, T... params){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
		}else{
			task.execute(params);
		}
	}
	
	static public <T> void executeAsyncTaskSerial(AsyncTask<T, ?, ?> task, T... params){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
			task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, params);
		}else{
			task.execute(params);
		}
	}
	
	
	public static AsyncTaskManager getInstance(){
		if(asyncTaskExtensions == null){
			asyncTaskExtensions = new AsyncTaskManager();
		}		
		return asyncTaskExtensions;
	}

	private AsyncTaskManager() {	
		if(executors == null)
			executors = new HashMap<AsyncTaskManager.Pool, SerialExecutor>();		
	}
	
	public void setCurrentExecutor(Pool pool) {
		if(pool == Pool.NAME_FETCHER){
			currentNameFetchersCount++;
			if(currentNameFetchersCount>MAX_NAME_FETCHER_COUNT){
				currentNameFetchersCount = 0;
			}
			switch (currentNameFetchersCount) {
			case 0:
				setupExecutor(Pool.NAME_FETCHER);
				break;
			case 1:
				setupExecutor(Pool.NAME_FETCHER_1);
				break;
			case 2:
				setupExecutor(Pool.NAME_FETCHER_2);
				break;
			case 3:
				setupExecutor(Pool.NAME_FETCHER_3);
				break;
			case 4:
				setupExecutor(Pool.NAME_FETCHER_4);
				break;
			case 5:
				setupExecutor(Pool.NAME_FETCHER_5);
				break;
			}
		}else{
			setupExecutor(pool);
		}
	}

	private void setupExecutor(Pool pool) {
		SerialExecutor serialExecutor = executors.get(pool);
		if(serialExecutor==null){
			serialExecutor = new SerialExecutor(pool.name());
			currentExecutor = serialExecutor;
			executors.put(pool, serialExecutor);
		}else
			currentExecutor = serialExecutor;
	}
	
	public <T> void executeCustom(AsyncTask<T, ?, ?> task, T... params){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
			task.executeOnExecutor(currentExecutor, params);
		}else{
			task.execute(params);
		}
	}



}
