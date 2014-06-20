package com.noplanbees.tbm;

import java.util.Set;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


public abstract class FileTransferService extends NonStopIntentService {
	private final String TAG = getClass().getSimpleName();
	private final static String STAG = FileTransferService.class.getSimpleName();

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

	private static Thread sleepingThread = null;

	// GARF need to make the name different for each subclass.
	public FileTransferService() {
		super("FileTransferService");
	}

	public static void restartTransfersPendingRetry(){
		Log.i(STAG, "restartTransfersPendingRetry");
		// There is a very slight chance of a race condition here. We check sleepingTread on from a different process. Before it executes
		// the following line to interrupt the OS switches context to the service which was sleeping and wakes it. It exits the try catch block.
		// We then call interrupt and the service crashes. On the bright side it will be automatically restarted by the OS. And the damage 
		// may only be a message failed to send or sent twice.
		if (sleepingThread != null){
			sleepingThread.interrupt();
		} 
	}

	@Override
	protected void onHandleIntent(Intent intent, int startId) {
		Log.i(TAG, "onHandleIntent");
		Log.i(TAG, " ------ " + startId + " ------ ");
		filePath = intent.getStringExtra(IntentFields.FILE_PATH_KEY);
		url = intent.getStringExtra(IntentFields.URL_KEY);
		params = intent.getBundleExtra(IntentFields.PARAMS_KEY);
		urlWithParams = url + stringifyParams(params);
		if (!doTransfer(intent))
			retry(intent);
	}

	protected abstract Boolean doTransfer(Intent intent);

	protected void reportStatus(Intent intent, int status){
		intent.setClass(getApplicationContext(), HomeActivity.class);
		intent.putExtra(IntentFields.STATUS_KEY, status);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS); // See doc/task_manager_bug.txt for the reason for this flag.
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); // This is probably not necessary but on a test bed I needed it to make sure onNewIntent is called in the activity.
		getApplicationContext().startActivity(intent);
	}
	
	private void retry(Intent intent){
		int retryCount = intent.getIntExtra(IntentFields.RETRY_COUNT_KEY, 0);
		retryCount ++;
		intent.putExtra(IntentFields.RETRY_COUNT_KEY, retryCount);

		try {
			sleepingThread = Thread.currentThread();
			Thread.sleep(1000);
			sleepingThread = null;
		} catch (InterruptedException e) {
			Log.i(TAG, "Interrupted sleeping thread");
			intent.putExtra(IntentFields.RETRY_COUNT_KEY, 0);
		}
		
		// Retry until recursion causes a stack overflow.
		try{
			doTransfer(intent);
		} catch (StackOverflowError e) {
			// GARF do we need to do something in this case?
			Log.e(TAG, "retry. Reached maximum retries. File cannot be transferred.");
			return;
		}
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
