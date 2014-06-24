package com.noplanbees.tbm;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public abstract class NonStopIntentService extends Service {
	private final String TAG = this.getClass().getSimpleName();
    private String mName;
    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;

    public NonStopIntentService(String name) {
        super();
        mName = name;
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent)msg.obj, (int)msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    	Log.i(TAG, "onCreate");
        HandlerThread thread = new HandlerThread("IntentService[" + mName + "]");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public void onStart(Intent intent, int startId) {
    	Log.i(TAG, "onStart");
		
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
    }


	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	Log.i(TAG, "onStartCommand flags=" + flags + " startId=" + startId);
    	
    	if (intent == null){
			return START_STICKY;
    	}
    	
		if (intent.getAction() != null && intent.getAction().equals("INTERRUPT")){
			Log.i(TAG, "onStart: Got a request to INTERRUPT");
	        mServiceLooper.getThread().interrupt();
	        // I dont want to call stopSelf on this becuase it wont have time to act.  But I also dont want it redelivered if the service is stopeed.
			return START_STICKY;
		}
		
        onStart(intent, startId);
        // If service is stopped when the queue still has intents that have not been handled.
        // It will restart and redeliver the intents for which we did not send a stopSelf(startId).
        return START_REDELIVER_INTENT;
    }    

    @Override
    public void onDestroy() {
    	Log.i(TAG, "onDestroy");
        mServiceLooper.quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * This method is invoked on the worker thread with a request to process.
     * Only one Intent is processed at a time, but the processing happens on a
     * worker thread that runs independently from other application logic.
     * So, if this code takes a long time, it will hold up other requests to
     * the same IntentService, but it will not hold up anything else.
     * 
     *
     * @param intent The value passed to {@link
     *               android.content.Context#startService(Intent)}.
     * @param startId 
     */
    protected abstract void onHandleIntent(Intent intent, int startId);  

}