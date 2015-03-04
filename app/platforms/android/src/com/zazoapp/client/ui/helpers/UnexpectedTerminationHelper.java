package com.zazoapp.client.ui.helpers;

import android.util.Log;
import com.zazoapp.client.multimedia.CameraManager;

/**
 * Created by skamenkovych@codeminders.com on 2/9/2015.
 */
public class UnexpectedTerminationHelper {
    private Thread mThread;
    private Thread.UncaughtExceptionHandler mOldUncaughtExceptionHandler = null;
    private Thread.UncaughtExceptionHandler mUncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
        // gets called on the same (main) thread
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            Log.w("UnexpectedTerminationHelper", "uncaughtException", ex);
            CameraManager.releaseCamera();
            if (mOldUncaughtExceptionHandler != null) {
                // it displays the "force close" dialog
                mOldUncaughtExceptionHandler.uncaughtException(thread, ex);
            }
        }
    };

    public void init() {
        mThread = Thread.currentThread();
        mOldUncaughtExceptionHandler = mThread.getUncaughtExceptionHandler();
        mThread.setUncaughtExceptionHandler(mUncaughtExceptionHandler);
    }

    public void finish() {
        mThread.setUncaughtExceptionHandler(mOldUncaughtExceptionHandler);
        mOldUncaughtExceptionHandler = null;
        mThread = null;
    }
}
