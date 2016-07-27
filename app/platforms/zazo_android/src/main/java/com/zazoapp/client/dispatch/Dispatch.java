package com.zazoapp.client.dispatch;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import com.zazoapp.client.utilities.Convenience;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * Created by User on 1/12/2015.
 */
public class Dispatch {
    private static final String TAG = Dispatch.class.getSimpleName();

    private static boolean includeLogcat = true;
    private static ErrorTracker tracker;

    public static final String MESSAGE_TAG = "message_tag";

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
        if (message != null) {
            Log.e(TAG, message);
        }
        ensureTracker(new Runnable() {
            @Override
            public void run() {
                tracker.setIncludeLogcat(includeLogcat);
                tracker.trackThrowable(th, ErrorLevel.ERROR);
            }
        });
    }

    public static void dispatch(Intent intent, @Nullable String tag, @Nullable String message) {
        if (tag != null && message != null) {
            Log.i(tag, message);
        }
        JSONObject data = new JSONObject();
        try {
            data.put(MESSAGE_TAG, tag);
            data.put("message", message);
            if (intent != null) {
                data.put("intent", intent.toString());
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    for (String key : extras.keySet()) {
                        try {
                            data.put(key, extras.get(key));
                        } catch (JSONException e) {
                            data.put(key, "-not valid data-");
                        }
                    }
                }
            }
        } catch (JSONException e) {
            dispatch(e, "Couldn't send json data");
            // no need return, send data anyway
        }
        dispatch(data, null);
    }

    public static void dispatch(final JSONObject data, @Nullable Throwable tr) {
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

    public static void dispatchFileContent(File userFactoryFile, String s, Throwable tr) {
        if (tr != null) {
            Log.e(TAG, s, tr);
        } else if (s != null) {
            Log.d(TAG, s);
        }
        final JSONObject object = new JSONObject();
        try {
            object.put("body", s);
            object.put("filename", userFactoryFile.getName());
            object.put("length", userFactoryFile.length());
            object.put("lastModified", userFactoryFile.lastModified());
            object.put("fileContent", Convenience.getTextFromFile(userFactoryFile.getAbsolutePath()));
            ensureTracker(new Runnable() {
                @Override
                public void run() {
                    tracker.setIncludeLogcat(includeLogcat);
                    tracker.trackData(object);
                }
            });
        } catch (JSONException e) {
            Dispatch.dispatch(e, "Couldn't load logs. Dispatching...");
        }
    }
}
