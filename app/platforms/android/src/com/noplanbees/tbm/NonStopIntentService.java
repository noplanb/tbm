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
    public static final String ACTION_STOP = "STOP";
    public static final String ACTION_INTERRUPT = "ACTION_INTERRUPT";
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
    	
		if (intent.getAction() != null &&
                (intent.getAction().equals(ACTION_STOP) || intent.getAction().equals(ACTION_INTERRUPT))){
			// Let the interrupt take effect immediately to interrupt a transfer waiting on a long retry delay. 
			// But also add the interrupt intent to the queue so that when it comes up we can call stop self in turn
			// it is important that stopSelf is called in order for each intent that comes in because 
			// android tracks them for the purpose of START_REDELIVER_INTENT. Calling stop self out of order
			// can have unknown consequences.
			Log.i(TAG, "onStart: Got a request to ACTION_STOP");
	        mServiceLooper.getThread().interrupt();
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