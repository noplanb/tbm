package com.zazoapp.client.debug;

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
    private static final String KEY_CUSTOM_HOST = "custom_host";
    private static final String KEY_CUSTOM_URI = "custom_uri";
    private static final String KEY_USE_CUSTOM_SERVER = "use_custom_server";
    private static final String KEY_USE_REAR_CAMERA = "use_rear_camera";
    private static final String KEY_SEND_BROKEN_VIDEO = "send_broken_video";

    public static final boolean DEBUG_LOG = false;

    private static volatile DebugConfig instance;

    private static class DeploymentType{
        public static final int DEVELOPMENT = 1;
        public static final int PRODUCTION = 2;
    }

    public interface DebugConfigChangesCallback {
        void onChange();
    }

    private Set<DebugConfigChangesCallback> callbacks = new HashSet<>();
    private Context context;
    private int mode;
    private boolean shouldSendSms;
    private String customServerHost;
    private String customServerUri;
    private boolean useCustomServer;
    private boolean useRearCamera;
    private boolean sendBrokenVideo;

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

    public static DebugConfig getInstance() {
        return instance;
    }

    private void init(Context context) {
        this.context = context;
        final SharedPreferences sp = context.getSharedPreferences(DEBUG_SETTINGS, Context.MODE_PRIVATE);
        mode = sp.getInt(KEY_MODE, DeploymentType.PRODUCTION);
        shouldSendSms = sp.getBoolean(KEY_SEND_SMS, true);
        customServerHost = sp.getString(KEY_CUSTOM_HOST, "");
        customServerUri = sp.getString(KEY_CUSTOM_URI, "");
        useCustomServer = sp.getBoolean(KEY_USE_CUSTOM_SERVER, false);
        useRearCamera = sp.getBoolean(KEY_USE_REAR_CAMERA, false);
        sendBrokenVideo = sp.getBoolean(KEY_SEND_BROKEN_VIDEO, false);
    }

    public boolean isDebugEnabled() {
        return mode == DeploymentType.DEVELOPMENT;
    }

    public boolean shouldSendSms() {
        return shouldSendSms;
    }

    public boolean shouldUseCustomServer() {
        return useCustomServer;
    }

    public String getCustomHost() {
        return customServerHost;
    }

    public String getCustomUri() {
        return customServerUri;
    }

    public boolean shouldUseRearCamera() {
        return useRearCamera;
    }

    public boolean shouldSendBrokenVideo() {
        return sendBrokenVideo;
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

    public void useCustomServer(boolean use) {
        useCustomServer = use;
        putBooleanPref(KEY_USE_CUSTOM_SERVER, use);
        notifyChanges();
    }

    public void setCustomServerHost(String host) {
        customServerHost = host;
        putStringPref(KEY_CUSTOM_HOST, host);
        notifyChanges();
    }

    public void setCustomServerUri(String uri) {
        customServerUri = uri;
        putStringPref(KEY_CUSTOM_URI, uri);
        notifyChanges();
    }

    public void useRearCamera(boolean use) {
        useRearCamera = use;
        putBooleanPref(KEY_USE_REAR_CAMERA, use);
        notifyChanges();
    }

    public void setSendBrokenVideo(boolean option) {
        sendBrokenVideo = option;
        putBooleanPref(KEY_SEND_BROKEN_VIDEO, option);
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

    private void putStringPref(String key, String value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(DEBUG_SETTINGS, Context.MODE_PRIVATE).edit();
        editor.putString(key, value);
        editor.commit();
    }

    public void savePrefs() {
        putIntPref(KEY_MODE, mode);
        putBooleanPref(KEY_SEND_SMS, shouldSendSms);
        putStringPref(KEY_CUSTOM_HOST, customServerHost);
        putStringPref(KEY_CUSTOM_URI, customServerUri);
        putBooleanPref(KEY_USE_CUSTOM_SERVER, useCustomServer);
        putBooleanPref(KEY_USE_REAR_CAMERA, useRearCamera);
        putBooleanPref(KEY_SEND_BROKEN_VIDEO, sendBrokenVideo);
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
