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

import com.noplanbees.tbm.crash_dispatcher.Dispatch;
import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.GridElement;
import com.noplanbees.tbm.model.ActiveModelsHandler;
import com.noplanbees.tbm.utilities.Logger;

import java.util.ArrayList;


public class DataHolderService extends Service {
	private final String TAG = this.getClass().getSimpleName();
	
    private String mName;
    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;

	private ActiveModelsHandler activeModelsHandler;
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
			return DataHolderService.this.activeModelsHandler;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Logger.d(TAG, "onCreate");

        mUnexpectedTerminationHelper.init();
		
        HandlerThread thread = new HandlerThread("IntentService[" + mName + "]");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);


        activeModelsHandler = ActiveModelsHandler.getInstance(this);
		//--------------------------
        // Try to retrieve all models from local storage
		//--------------------------
		activeModelsHandler.ensureAll();

        GridManager.getInstance().initGrid(this);
		
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

    private void releaseResources() {
        activeModelsHandler.saveAll();
        unregisterReceiver(receiver);
        mUnexpectedTerminationHelper.finish();
    }

    private UnexpectedTerminationHelper mUnexpectedTerminationHelper = new UnexpectedTerminationHelper();

    private class UnexpectedTerminationHelper {
        private Thread mThread;
        private Thread.UncaughtExceptionHandler mOldUncaughtExceptionHandler = null;
        private Thread.UncaughtExceptionHandler mUncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
            // gets called on the same (main) thread
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Log.w("UnexpectedTerminationHelper", "uncaughtException", ex);
                releaseResources();
                Dispatch.dispatch("UnexpectedTerminationHelper: " + ex.getMessage(), true);
                if (mOldUncaughtExceptionHandler != null) {
                    // it displays the "force close" dialog
                    mOldUncaughtExceptionHandler.uncaughtException(thread, ex);
                }
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
        };

        void init() {
            mThread = Thread.currentThread();
            mOldUncaughtExceptionHandler = mThread.getUncaughtExceptionHandler();
            mThread.setUncaughtExceptionHandler(mUncaughtExceptionHandler);
        }

        void finish() {
            mThread.setUncaughtExceptionHandler(mOldUncaughtExceptionHandler);
            mOldUncaughtExceptionHandler = null;
            mThread = null;
        }
    }

}
