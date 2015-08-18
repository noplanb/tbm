package com.zazoapp.client.dispatch;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;

/**
 * Created by User on 1/12/2015.
 */
public class Dispatch {
    private static final String TAG = Dispatch.class.getSimpleName();

    private static boolean includeLogcat = true;
    private static ErrorTracker tracker;

    public static void setIncludeLogcat(boolean includeLogcat) {
        Dispatch.includeLogcat = includeLogcat;
    }

    public static void registerTracker(Context context, ErrorTracker tracker) {
        Dispatch.tracker = tracker;
        tracker.init(context);
    }

    public static void dispatch(String msg){
        dispatch(msg, false);
    }

    public static void dispatch(final String msg, boolean needToWait){
        ensureTracker(new Runnable() {
            @Override
            public void run() {
                tracker.setIncludeLogcat(includeLogcat);
                tracker.trackMessage(msg);
            }
        });
    }

    public static void dispatchUserInfo(final Context context) {
        ensureTracker(new Runnable() {
            @Override
            public void run() {
                tracker.setIncludeLogcat(includeLogcat);
                tracker.trackMessage(UserInfoCollector.collect(context), ErrorLevel.INFO);
            }
        });
    }

    public static void dispatchStored() {
        ensureTracker(new Runnable() {
            @Override
            public void run() {
                tracker.trackStored();
            }
        });
    }

    public static void dispatch(final Throwable th, String message) {
        Log.e(TAG, message);
        ensureTracker(new Runnable() {
            @Override
            public void run() {
                tracker.setIncludeLogcat(includeLogcat);
                tracker.trackThrowable(th, ErrorLevel.ERROR);
            }
        });
    }

    public static void dispatch(final JSONObject data, Throwable tr) {
        if (tr != null) {
            Log.e(TAG, "dispatch", tr);
        }
        ensureTracker(new Runnable() {
            @Override
            public void run() {
                tracker.setIncludeLogcat(includeLogcat);
                tracker.trackData(data);
            }
        });
    }

    private static void ensureTracker(Runnable runnable) {
        if (tracker == null) {
            Log.e(TAG, "Register tracker first");
            return;
        }
        runnable.run();
    }
}
