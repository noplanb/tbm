package com.noplanbees.tbm.network;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.noplanbees.tbm.DataHolderService;
import com.noplanbees.tbm.NonStopIntentService;
import com.noplanbees.tbm.model.User;
import com.noplanbees.tbm.model.UserFactory;
import com.noplanbees.tbm.network.aws.S3FileTransferAgent;

import java.util.Set;



public abstract class FileTransferService extends NonStopIntentService {
	private final String TAG = getClass().getSimpleName();
	private final static String STAG = FileTransferService.class.getSimpleName();
	private final static Integer MAX_RETRIES = 100;

	protected String id;
	
	protected IFileTransferAgent fileTransferAgent;

	public class IntentFields {
		public static final String ID_KEY = "id";		
		public final static String TRANSFER_TYPE_KEY = "transferType";
		public final static String RETRY_COUNT_KEY = "retryCount";
		public final static String FILE_PATH_KEY = "filePath";
		public final static String FILE_NAME_KEY = "filename";
		public final static String PARAMS_KEY = "params";
		public final static String STATUS_KEY = "status";
		public final static String VIDEO_ID_KEY = "videoIdKey";
		public final static String VIDEOIDS_REMOTE_KV_KEY = "videoIdsRemoteKVKey";

		// TODO: Andrey are these necessary. Let me know so that we can delete them. -- Sani
//        public final static String USER_MKEY = "user_mkey";
//        public final static String USER_AUTH = "user_auth";

        public final static String TRANSFER_TYPE_UPLOAD = "upload";
		public final static String TRANSFER_TYPE_DOWNLOAD = "download";
	}

	protected abstract void maxRetriesReached(Intent intent) throws InterruptedException;
	protected abstract boolean doTransfer(Intent intent) throws InterruptedException;

	
	public FileTransferService(String name) {
		super(name);
	}

    @Override
    public void onCreate() {
        super.onCreate();
        if(NetworkConfig.IS_AWS_USING){
            fileTransferAgent = new S3FileTransferAgent(this);
        }else{
            fileTransferAgent = new ServerFileTransferAgent(this);
        }
    }

    @Override
	protected void onHandleIntent(Intent intent, int startId) {
		if (intent.getAction() != null && intent.getAction().equals("INTERRUPT")){
			// Non stop intent service has already acted on the interrupt when it got it possibly out of order.
			// Our only job here is to stopSelf for this intent as it has come up in the queue so we are calling 
			// stop self for this intent in the same order that it came in.
			Log.i(TAG, "Calling stopSelf for an interrupt intent.");
			//stopSelf(startId);
			return;
		}
		
		try {
			Log.i(TAG, "onHandleIntent");
			//TODO: Andrey why did you add this code here. Please let me know --Sani
//            User user = UserFactory.getFactoryInstance().makeInstance(getApplicationContext());
//            user.set(User.Attributes.AUTH, intent.getStringExtra(IntentFields.USER_AUTH));
//            user.set(User.Attributes.MKEY, intent.getStringExtra(IntentFields.USER_MKEY));
			fileTransferAgent.setInstanceVariables(intent);
		} catch (InterruptedException e) {
			Log.i(TAG, "Interrupt caught for Restart retry outside of loop.");
			intent.putExtra(FileTransferService.IntentFields.RETRY_COUNT_KEY, 0);
		}

        try {
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
        } catch (IllegalStateException e) {
            Log.d(TAG, "catched");
        }
		stopSelf(startId);
	}

	protected void reportStatus(Intent intent, int status){
		Log.i(TAG, "reportStatus");
		intent.setClass(getApplicationContext(), DataHolderService.class);
		intent.putExtra(IntentFields.STATUS_KEY, status);
		getApplicationContext().startService(intent);
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