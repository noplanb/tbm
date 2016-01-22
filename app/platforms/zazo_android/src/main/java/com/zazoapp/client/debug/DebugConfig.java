package com.zazoapp.client.debug;

import android.content.Context;
import com.zazoapp.client.core.PreferencesHelper;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
* Created by skamenkovych@codeminders.com on 2/20/2015.
*/
public class DebugConfig {
    public static final String DEBUG_SETTINGS = "zazo_debug";
    /*int*/ private static final String KEY_MODE = "mode";
    /*bol*/ private static final String KEY_SEND_SMS = "send_sms";
    /*str*/ private static final String KEY_CUSTOM_HOST = "custom_host";
    /*str*/ private static final String KEY_CUSTOM_URI = "custom_uri";
    /*bol*/ private static final String KEY_USE_CUSTOM_SERVER = "use_custom_server";
    /*bol*/ private static final String KEY_USE_REAR_CAMERA = "use_rear_camera";
    /*bol*/ private static final String KEY_SEND_BROKEN_VIDEO = "send_broken_video";
    /*bol*/ private static final String KEY_DISABLE_GCM_NOTIFICATIONS = "disable_gcm_notifications";
    /*bol*/ private static final String KEY_ENABLE_ALL_FEATURES = "enable_all_features";
    /*bol*/ private static final String KEY_FEATURE_OPTIONS_OPENED = "feature_options_opened";
    /*bol*/ private static final String KEY_FORCE_CONFIRMATION_SMS = "force_confirmation_sms";
    /*bol*/ private static final String KEY_FORCE_CONFIRMATION_CALL = "force_confirmation_call";
    /*int*/ private static final String KEY_MIN_ROOM_SPACE_RESTRICTION = "min_room_space_restriction";
    /*bol*/ private static final String KEY_SEND_INCORRECT_FILE_SIZE = "send_incorrect_file_size";
    /*bol*/ private static final String KEY_ALLOW_RESEND = "allow_resend";

    public static final boolean DEBUG_LOG = false;

    private static volatile DebugConfig instance;

    public static final int DEFAULT_MIN_ROOM_SPACE_RESTRICTION = 30;

    private static class DeploymentType{
        public static final int DEVELOPMENT = 1;
        public static final int PRODUCTION = 2;
    }

    public interface DebugConfigChangesCallback {
        void onChange();
    }

    private Set<DebugConfigChangesCallback> callbacks = new HashSet<>();
    private Context context;
    private PreferencesHelper prefs;
    private int mode;
    private boolean shouldSendSms;
    private String customServerHost;
    private String customServerUri;
    private boolean useCustomServer;
    private boolean useRearCamera;
    private boolean sendBrokenVideo;
    private boolean disableGcmNotifications;
    private boolean enableAllFeatures;
    private boolean featureOptionsOpened;
    private boolean forceConfirmationSms;
    private boolean forceConfirmationCall;
    private int minRoomSpace;
    private boolean sendIncorrectFileSize;
    private boolean allowResend;

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
        prefs = new PreferencesHelper(context, DEBUG_SETTINGS);
        mode = prefs.getInt(KEY_MODE, DeploymentType.PRODUCTION);
        shouldSendSms = prefs.getBoolean(KEY_SEND_SMS, true);
        customServerHost = prefs.getString(KEY_CUSTOM_HOST, "");
        customServerUri = prefs.getString(KEY_CUSTOM_URI, "");
        useCustomServer = prefs.getBoolean(KEY_USE_CUSTOM_SERVER, false);
        useRearCamera = prefs.getBoolean(KEY_USE_REAR_CAMERA, false);
        sendBrokenVideo = prefs.getBoolean(KEY_SEND_BROKEN_VIDEO, false);
        disableGcmNotifications = prefs.getBoolean(KEY_DISABLE_GCM_NOTIFICATIONS, false);
        enableAllFeatures = prefs.getBoolean(KEY_ENABLE_ALL_FEATURES, false);
        featureOptionsOpened = prefs.getBoolean(KEY_FEATURE_OPTIONS_OPENED, false);
        forceConfirmationSms = prefs.getBoolean(KEY_FORCE_CONFIRMATION_SMS, false);
        forceConfirmationCall = prefs.getBoolean(KEY_FORCE_CONFIRMATION_CALL, false);
        minRoomSpace = prefs.getInt(KEY_MIN_ROOM_SPACE_RESTRICTION, DEFAULT_MIN_ROOM_SPACE_RESTRICTION);
        sendIncorrectFileSize = prefs.getBoolean(KEY_SEND_INCORRECT_FILE_SIZE, false);
        allowResend = prefs.getBoolean(KEY_ALLOW_RESEND, false);
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

    public boolean isGcmNotificationsDisabled() {
        return disableGcmNotifications;
    }

    public boolean isAllFeaturesEnabled() {
        return enableAllFeatures;
    }

    public boolean isFeatureOptionsOpened() {
        return featureOptionsOpened;
    }

    public boolean shouldForceConfirmationSms() {
        return forceConfirmationSms;
    }

    public boolean shouldForceConfirmationCall() {
        return forceConfirmationCall;
    }

    public int getMinRoomSpace() {
        return minRoomSpace;
    }

    public boolean shouldSendIncorrectFileSize() {
        return sendIncorrectFileSize;
    }

    public boolean isResendAllowed() {
        return allowResend;
    }

    public void enableSendSms(boolean sendSms) {
        shouldSendSms = sendSms;
        prefs.putBoolean(KEY_SEND_SMS, sendSms);
        notifyChanges();
    }

    public void enableDebug(boolean enable) {
        mode = enable ? DeploymentType.DEVELOPMENT : DeploymentType.PRODUCTION;
        prefs.putInt(KEY_MODE, mode);
        notifyChanges();
    }

    public void useCustomServer(boolean use) {
        useCustomServer = use;
        prefs.putBoolean(KEY_USE_CUSTOM_SERVER, use);
        notifyChanges();
    }

    public void setCustomServerHost(String host) {
        customServerHost = host;
        prefs.putString(KEY_CUSTOM_HOST, host);
        notifyChanges();
    }

    public void setCustomServerUri(String uri) {
        customServerUri = uri;
        prefs.putString(KEY_CUSTOM_URI, uri);
        notifyChanges();
    }

    public void useRearCamera(boolean use) {
        useRearCamera = use;
        prefs.putBoolean(KEY_USE_REAR_CAMERA, use);
        notifyChanges();
    }

    public void setSendBrokenVideo(boolean option) {
        sendBrokenVideo = option;
        prefs.putBoolean(KEY_SEND_BROKEN_VIDEO, option);
        notifyChanges();
    }

    public void setDisableGcmNotifications(boolean disable) {
        disableGcmNotifications = disable;
        prefs.putBoolean(KEY_DISABLE_GCM_NOTIFICATIONS, disable);
        notifyChanges();
    }

    public void enableAllFeatures(boolean enable) {
        enableAllFeatures = enable;
        prefs.putBoolean(KEY_ENABLE_ALL_FEATURES, enable);
        notifyChanges();
    }

    public void openFeatureOptions(boolean open) {
        featureOptionsOpened = open;
        prefs.putBoolean(KEY_FEATURE_OPTIONS_OPENED, open);
        notifyChanges();
    }

    public void setForceConfirmationSms(boolean value) {
        forceConfirmationSms = value;
        prefs.putBoolean(KEY_FORCE_CONFIRMATION_SMS, value);
        notifyChanges();
    }

    public void setForceConfirmationCall(boolean value) {
        forceConfirmationCall = value;
        prefs.putBoolean(KEY_FORCE_CONFIRMATION_CALL, value);
        notifyChanges();
    }

    public void setMinRoomSpace(int value) {
        if (value < DEFAULT_MIN_ROOM_SPACE_RESTRICTION) {
            value = DEFAULT_MIN_ROOM_SPACE_RESTRICTION;
        }
        minRoomSpace = value;
        prefs.putInt(KEY_MIN_ROOM_SPACE_RESTRICTION, value);
        notifyChanges();
    }

    public void setSendIncorrectFileSize(boolean send) {
        sendIncorrectFileSize = send;
        prefs.putBoolean(KEY_SEND_INCORRECT_FILE_SIZE, send);
        notifyChanges();
    }

    public void allowResend(boolean allow) {
        allowResend = allow;
        prefs.putBoolean(KEY_ALLOW_RESEND, allow);
        notifyChanges();
    }

    public void savePrefs() {
        prefs.putInt(KEY_MODE, mode);
        prefs.putBoolean(KEY_SEND_SMS, shouldSendSms);
        prefs.putString(KEY_CUSTOM_HOST, customServerHost);
        prefs.putString(KEY_CUSTOM_URI, customServerUri);
        prefs.putBoolean(KEY_USE_CUSTOM_SERVER, useCustomServer);
        prefs.putBoolean(KEY_USE_REAR_CAMERA, useRearCamera);
        prefs.putBoolean(KEY_SEND_BROKEN_VIDEO, sendBrokenVideo);
        prefs.putBoolean(KEY_DISABLE_GCM_NOTIFICATIONS, disableGcmNotifications);
        prefs.putBoolean(KEY_ENABLE_ALL_FEATURES, enableAllFeatures);
        prefs.putBoolean(KEY_FEATURE_OPTIONS_OPENED, featureOptionsOpened);
        prefs.putBoolean(KEY_FORCE_CONFIRMATION_SMS, forceConfirmationSms);
        prefs.putBoolean(KEY_FORCE_CONFIRMATION_CALL, forceConfirmationCall);
        prefs.putInt(KEY_MIN_ROOM_SPACE_RESTRICTION, minRoomSpace);
        prefs.putBoolean(KEY_SEND_INCORRECT_FILE_SIZE, sendIncorrectFileSize);
        prefs.putBoolean(KEY_ALLOW_RESEND, allowResend);
    }

    public void reloadPrefs() {
        mode = prefs.getInt(KEY_MODE, DeploymentType.PRODUCTION);
        shouldSendSms = prefs.getBoolean(KEY_SEND_SMS, true);
        customServerHost = prefs.getString(KEY_CUSTOM_HOST, "");
        customServerUri = prefs.getString(KEY_CUSTOM_URI, "");
        useCustomServer = prefs.getBoolean(KEY_USE_CUSTOM_SERVER, false);
        useRearCamera = prefs.getBoolean(KEY_USE_REAR_CAMERA, false);
        sendBrokenVideo = prefs.getBoolean(KEY_SEND_BROKEN_VIDEO, false);
        disableGcmNotifications = prefs.getBoolean(KEY_DISABLE_GCM_NOTIFICATIONS, false);
        enableAllFeatures = prefs.getBoolean(KEY_ENABLE_ALL_FEATURES, false);
        featureOptionsOpened = prefs.getBoolean(KEY_FEATURE_OPTIONS_OPENED, false);
        forceConfirmationSms = prefs.getBoolean(KEY_FORCE_CONFIRMATION_SMS, false);
        forceConfirmationCall = prefs.getBoolean(KEY_FORCE_CONFIRMATION_CALL, false);
        minRoomSpace = prefs.getInt(KEY_MIN_ROOM_SPACE_RESTRICTION, DEFAULT_MIN_ROOM_SPACE_RESTRICTION);
        sendIncorrectFileSize = prefs.getBoolean(KEY_SEND_INCORRECT_FILE_SIZE, false);
        allowResend = prefs.getBoolean(KEY_ALLOW_RESEND, false);
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
