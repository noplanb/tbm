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
    private static final String KEY_SEND_SMS = "send_sms";
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
    private boolean shouldSendSms;

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
        shouldSendSms = sp.getBoolean(KEY_SEND_SMS, true);
    }

    public boolean isDebugEnabled() {
        return mode == DeploymentType.DEVELOPMENT;
    }

    public boolean shouldSendSms() {
        return shouldSendSms;
    }

    public void enableSendSms(boolean sendSms) {
        shouldSendSms = sendSms;
        putBooleanPref(KEY_SEND_SMS, sendSms);
        notifyChanges();
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

    private void putBooleanPref(String key, boolean value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(DEBUG_SETTINGS, Context.MODE_PRIVATE).edit();
        editor.putBoolean(key, value);
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
