package com.zazoapp.client.ui.helpers;

import android.util.Log;
import com.zazoapp.client.dispatch.Dispatch;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by skamenkovych@codeminders.com on 2/9/2015.
 */
public class UnexpectedTerminationHelper {
    private static final String TAG =  UnexpectedTerminationHelper.class.getSimpleName();

    private Set<TerminationCallback> terminationCallbacks = new HashSet<>();
    private Thread.UncaughtExceptionHandler mOldUncaughtExceptionHandler = null;
    private Thread.UncaughtExceptionHandler mUncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
        // gets called on the same (main) thread
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            Log.w(TAG, "uncaughtException", ex);
            notifyCallbacks();
            final Thread.UncaughtExceptionHandler oldHandler = restoreSystemHandler();
            Dispatch.dispatch(TAG + ": " + ex.getMessage(), true);
            if (oldHandler != null) {
                // it displays the "force close" dialog
                oldHandler.uncaughtException(thread, ex);
            }
        }
    };

    public void init() {
        mOldUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(mUncaughtExceptionHandler);
    }

    private void notifyCallbacks() {
        for (TerminationCallback callback : terminationCallbacks) {
            callback.onTerminate();
        }
        terminationCallbacks.clear();
    }

    private Thread.UncaughtExceptionHandler restoreSystemHandler() {
        Thread.UncaughtExceptionHandler oldHandler = mOldUncaughtExceptionHandler;
        Thread.setDefaultUncaughtExceptionHandler(mOldUncaughtExceptionHandler);
        mOldUncaughtExceptionHandler = null;
        return oldHandler;
    }

    public void addTerminationCallback(TerminationCallback callback) {
        terminationCallbacks.add(callback);
    }

    public interface TerminationCallback {
        void onTerminate();
    }
}
