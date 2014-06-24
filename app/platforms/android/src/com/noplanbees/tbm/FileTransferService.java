package com.noplanbees.tbm;

import java.util.ArrayList;
import java.util.Set;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import java.util.Set;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


public abstract class FileTransferService extends NonStopIntentService {
	private final String TAG = getClass().getSimpleName();
	private final static String STAG = FileTransferService.class.getSimpleName();
	private final static Integer MAX_RETRIES = 100;

	protected String id;
	protected String filePath;
	protected String url;
	protected String urlWithParams;
	protected Bundle params;

	public class IntentFields {
		public static final String ID_KEY = "id";		
		public final static String TRANSFER_TYPE_KEY = "transferType";
		public final static String RETRY_COUNT_KEY = "retryCount";
		public final static String FILE_PATH_KEY = "filePath";
		public final static String URL_KEY = "url";
		public final static String PARAMS_KEY = "params";
		public final static String STATUS_KEY = "status";
		public final static String VIDEO_ID_KEY = "videoIdKey";

		public final static String TRANSFER_TYPE_UPLOAD = "upload";
		public final static String TRANSFER_TYPE_DOWNLOAD = "download";
	}

	public FileTransferService(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent, int startId) {
		try {
			Log.i(TAG, "onHandleIntent");
			setInstanceVariables(intent);
		} catch (InterruptedException e) {
			Log.i(TAG, "Interrupt caught for Restart retry outside of loop.");
			intent.putExtra(FileTransferService.IntentFields.RETRY_COUNT_KEY, 0);
		}

		while(true){
			try {
				if (intent.getIntExtra(IntentFields.RETRY_COUNT_KEY, 0) > MAX_RETRIES){
					Log.i(TAG, "onHandleIntent: MAX_RETRIES reached: " + MAX_RETRIES);
					maxRetriesReached(intent);
					break;
				}
				if (doTransfer(intent))
					break;
				retrySleep(intent);
			} catch (InterruptedException e) {
				Log.i(TAG, "Interrupt caught for Restart retry inside loop.");
				intent.putExtra(FileTransferService.IntentFields.RETRY_COUNT_KEY, 0);
			}
		}
		stopSelf(startId);
	}

	private void setInstanceVariables(Intent intent) throws InterruptedException{
		filePath = intent.getStringExtra(IntentFields.FILE_PATH_KEY);
		url = intent.getStringExtra(IntentFields.URL_KEY);
		params = intent.getBundleExtra(IntentFields.PARAMS_KEY);
		urlWithParams = url + stringifyParams(params);
	}

	protected abstract void maxRetriesReached(Intent intent) throws InterruptedException;

	protected abstract Boolean doTransfer(Intent intent) throws InterruptedException;

	protected void reportStatus(Intent intent, int status){
		Log.i(TAG, "reportStatus");
		intent.setClass(getApplicationContext(), HomeActivity.class);
		intent.putExtra(IntentFields.STATUS_KEY, status);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS); // See doc/task_manager_bug.txt for the reason for this flag.
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); // This is probably not necessary but on a test bed I needed it to make sure onNewIntent is called in the activity.
		getApplicationContext().startActivity(intent);
	}

	private void retrySleep(Intent intent) throws InterruptedException{
		int retryCount = intent.getIntExtra(IntentFields.RETRY_COUNT_KEY, 0);
		retryCount ++;
		Log.i(TAG, "retry: " + retryCount);
		intent.putExtra(IntentFields.RETRY_COUNT_KEY, retryCount);

		long sleepTime = sleepTime(retryCount);
		Log.i(TAG, "Sleeping for: " + sleepTime + "ms");
		Thread.sleep(sleepTime);
	}

	private long sleepTime(int retryCount){
		if (retryCount <= 9){
			return 1000 * (1<<retryCount);
		}
		return 512000;
	}

	private String stringifyParams(Bundle params){
		Set<String> keys = params.keySet();
		if (keys.isEmpty())
			return "";

		String result = "?";
		for (String key : keys){
			if (!result.equals("?"))
				result += "&";
			result += (key + "=" + params.getString(key));
		}
		return result;
	}


}