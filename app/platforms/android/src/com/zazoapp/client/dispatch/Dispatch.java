package com.zazoapp.client.dispatch;

import android.content.Context;
import android.util.Log;

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

    public static void dispatch(String msg, boolean needToWait){
        Log.e(TAG, msg);
        if (tracker == null) {
            Log.e(TAG, "Register tracker first");
            return;
        }
        tracker.setIncludeLogcat(includeLogcat);
        tracker.trackMessage(msg);
    }

    public static void dispatchUserInfo(Context context) {
        if (tracker == null) {
            Log.e(TAG, "Register tracker first");
            return;
        }
        tracker.setIncludeLogcat(false);
        tracker.trackMessage(UserInfoCollector.collect(context));
    }

    public static void dispatchStored() {
        if (tracker == null) {
            Log.e(TAG, "Register tracker first");
            return;
        }
        tracker.trackStored();
    }
}
