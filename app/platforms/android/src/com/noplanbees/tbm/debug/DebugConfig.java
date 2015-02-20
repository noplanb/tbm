package com.noplanbees.tbm.debug;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
* Created by skamenkovych@codeminders.com on 2/20/2015.
*/
public class DebugConfig {
    private static final String DEBUG_SETTINGS = "zazo_debug";
    private static final String KEY_MODE = "mode";
    public static final boolean DEBUG_LOG = true;

    private static volatile DebugConfig instance;

    private static class DeploymentType{
        public static int DEVELOPMENT = 1;
        public static int PRODUCTION = 2;
    }

    public interface DebugConfigChangesCallback {
        void onChange();
    }

    private Set<DebugConfigChangesCallback> callbacks = new HashSet<>();
    private Context context;
    private int mode;

    private DebugConfig() {
    }

    public static DebugConfig getInstance(Context context) {
        DebugConfig localInstance = instance;
        if (localInstance != null) {
            if (!context.getApplicationContext().equals(localInstance.context)) {
                localInstance = null;
            }
        }
        if (localInstance == null) {
            synchronized (DebugConfig.class) {
                localInstance = instance;
                if (localInstance == null) {
                    localInstance = new DebugConfig();
                    localInstance.init(context.getApplicationContext());
                    instance = localInstance;
                }
            }
        }
        return localInstance;
    }

    private void init(Context context) {
        this.context = context;
        final SharedPreferences sp = context.getSharedPreferences(DEBUG_SETTINGS, Context.MODE_PRIVATE);
        mode = sp.getInt(KEY_MODE, DeploymentType.PRODUCTION);
    }

    public boolean isDebugEnabled() {
        return mode == DeploymentType.DEVELOPMENT;
    }

    public void enableDebug(boolean enable) {
        mode = enable ? DeploymentType.DEVELOPMENT : DeploymentType.PRODUCTION;
        putIntPref(KEY_MODE, mode);
        notifyChanges();
    }

    private void putIntPref(String key, int value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(DEBUG_SETTINGS, Context.MODE_PRIVATE).edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public void savePrefs() {
        putIntPref(KEY_MODE, mode);
    }

    public void addCallback(DebugConfigChangesCallback callback) {
        callbacks.add(callback);
    }

    public void removeCallback(DebugConfigChangesCallback callback) {
        callbacks.remove(callback);
    }

    private void notifyChanges() {
        Iterator<DebugConfigChangesCallback> iterator = callbacks.iterator();
        while (iterator.hasNext()) {
            DebugConfigChangesCallback callback = iterator.next();
            if (callback != null) {
                callback.onChange();
            }
        }
    }
}
