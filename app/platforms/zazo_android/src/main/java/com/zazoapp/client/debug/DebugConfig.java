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

    public static final boolean DEBUG_LOG = false;

    private static final DebugConfig INSTANCE = new DebugConfig();

    public static final int DEFAULT_MIN_ROOM_SPACE_RESTRICTION = 30;

    private static class DeploymentType{
        public static final int DEVELOPMENT = 1;
        public static final int PRODUCTION = 2;
    }

    public interface DebugConfigChangesCallback {
        void onChange();
    }

    public enum Bool {
        SEND_SMS(true),
        USE_CUSTOM_SERVER(false),
        USE_REAR_CAMERA(false),
        SEND_BROKEN_VIDEO(false),
        DISABLE_GCM_NOTIFICATIONS(false),
        ENABLE_ALL_FEATURES(false),
        FEATURE_OPTIONS_OPENED(false),
        FORCE_CONFIRMATION_SMS(false),
        FORCE_CONFIRMATION_CALL(false),
        SEND_INCORRECT_FILE_SIZE(false),
        ALLOW_RESEND(false);

        private boolean value;

        Bool(boolean defaultValue) {
            this.value = defaultValue;
        }

        public boolean get() {
            return value;
        }
    }

    public enum Int {
        MODE(DeploymentType.PRODUCTION),
        MIN_ROOM_SPACE_RESTRICTION(DEFAULT_MIN_ROOM_SPACE_RESTRICTION);
        private int value;

        Int(int defaultValue) {
            this.value = defaultValue;
        }

        public int get() {
            return value;
        }
    }

    public enum Str {
        CUSTOM_HOST(""),
        CUSTOM_URI("");
        private String value;

        Str(String defaultValue) {
            this.value = defaultValue;
        }

        public String get() {
            return value;
        }
    }

    private Set<DebugConfigChangesCallback> callbacks = new HashSet<>();
    private PreferencesHelper prefs;

    private DebugConfig() {
    }

    public static void init(Context context) {
        INSTANCE.prefs = new PreferencesHelper(context, DEBUG_SETTINGS);
        INSTANCE.loadPreferences();
    }

    private static boolean isInited() {
        return INSTANCE.prefs != null;
    }

    private void loadPreferences() {
        for (Bool pref : Bool.values()) {
            pref.value = prefs.getBoolean(pref.name().toLowerCase(), pref.value);
        }
        for (Int pref : Int.values()) {
            pref.value = prefs.getInt(pref.name().toLowerCase(), pref.value);
        }
        for (Str pref : Str.values()) {
            pref.value = prefs.getString(pref.name().toLowerCase(), pref.value);
        }
    }

    public static DebugConfig getInstance() {
        return INSTANCE;
    }

    public static boolean isDebugEnabled() {
        return Int.MODE.get() == DeploymentType.DEVELOPMENT;
    }

    public static void set(Bool pref, boolean value) {
        if (isInited()) {
            pref.value = value;
            INSTANCE.prefs.putBoolean(pref.name().toLowerCase(), value);
            INSTANCE.notifyChanges();
        } else {
            throw new IllegalStateException("Settings wasn't inited");
        }
    }

    public static void set(Int pref, int value) {
        if (isInited()) {
            pref.value = value;
            INSTANCE.prefs.putInt(pref.name().toLowerCase(), value);
            INSTANCE.notifyChanges();
        } else {
            throw new IllegalStateException("Settings wasn't inited");
        }
    }

    public static void set(Str pref, String value) {
        if (isInited()) {
            pref.value = value;
            INSTANCE.prefs.putString(pref.name().toLowerCase(), value);
            INSTANCE.notifyChanges();
        } else {
            throw new IllegalStateException("Settings wasn't inited");
        }
    }

    public static void enableDebug(boolean enable) {
        set(Int.MODE, enable ? DeploymentType.DEVELOPMENT : DeploymentType.PRODUCTION);
    }

    public static void setMinRoomSpace(int value) {
        if (value < DEFAULT_MIN_ROOM_SPACE_RESTRICTION) {
            value = DEFAULT_MIN_ROOM_SPACE_RESTRICTION;
        }
        set(Int.MIN_ROOM_SPACE_RESTRICTION, value);
    }

    public void savePrefs() {
        for (Bool pref : Bool.values()) {
            prefs.putBoolean(pref.name().toLowerCase(), pref.value);
        }
        for (Int pref : Int.values()) {
            prefs.putInt(pref.name().toLowerCase(), pref.value);
        }
        for (Str pref : Str.values()) {
            prefs.putString(pref.name().toLowerCase(), pref.value);
        }
    }

    public static void reloadPrefs() {
        INSTANCE.loadPreferences();
    }

    public static void addCallback(DebugConfigChangesCallback callback) {
        INSTANCE.callbacks.add(callback);
    }

    public static void removeCallback(DebugConfigChangesCallback callback) {
        INSTANCE.callbacks.remove(callback);
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
