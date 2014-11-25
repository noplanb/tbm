package com.noplanbees.tbm;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.noplanbees.tbm.utilities.Logger;


public class DataHolderService extends Service {
	private final String TAG = this.getClass().getSimpleName();
	
    private String mName;
    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;

	private ActiveModelsHandler dataManager;
	private ShutdownReceiver receiver;


    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent)msg.obj, (int)msg.arg1);
        }
    }
	public DataHolderService() {
		super();
        mName = "DataHolderService";
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return new LocalBinder();
	}

	public class LocalBinder extends Binder {
		public ActiveModelsHandler getActiveModelsHandler() {
			return DataHolderService.this.dataManager;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Logger.d(TAG, "onCreate");
		
        HandlerThread thread = new HandlerThread("IntentService[" + mName + "]");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

		
		dataManager = ActiveModelsHandler.getInstance(this);
		//--------------------------
        // Try to retrieve all models from local storage
		//--------------------------
		dataManager.ensureAll();
		
		receiver = new ShutdownReceiver();
		IntentFilter filter = new IntentFilter("android.intent.action.ACTION_SHUTDOWN");
		registerReceiver(receiver, filter);
		
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

		if(intent!=null)
			onStart(intent, startId);
	    // If service is stopped when the queue still has intents that have not been handled.
	    // It will restart and redeliver the intents for which we did not send a stopSelf(startId).
	    return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Logger.d(TAG, "onDestroy");
		releaseResources();
        mServiceLooper.quit();
	}

	private void releaseResources() {
		dataManager.saveAll();
		unregisterReceiver(receiver);
	}

	protected void onHandleIntent(final Intent intent, int startId) {
		new IntentHandler(DataHolderService.this, intent).handle();
	}
	
	@Override
	public void onTaskRemoved(Intent rootIntent) {
		super.onTaskRemoved(rootIntent);
		Logger.d(TAG, "onTaskRemoved");
		releaseResources();
	}
	
	
	private class ShutdownReceiver extends BroadcastReceiver {

	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	Log.d("ShutdownReceiver", "onReceive");
	       releaseResources();
	    }

	}

}
