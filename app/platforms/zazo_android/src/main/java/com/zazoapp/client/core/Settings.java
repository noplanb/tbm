package com.zazoapp.client.core;

import android.content.Context;
import android.support.annotation.StringRes;
import com.zazoapp.client.R;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by skamenkovych@codeminders.com on 1/22/2016.
 */
public class Settings {
    public static final String FILE_NAME = "zazo_user_settings";
    public enum Bool {
        ALLOW_DATA_IN_ROAMING(true, R.string.settings_allow_data_in_roaming, R.string.settings_allow_data_in_roaming_hint),
        ;

        private boolean value;
        private int labelId;
        private int hintId;

        Bool(boolean defaultValue, @StringRes int labelId, @StringRes int hintId) {
            value = defaultValue;
            this.labelId = labelId;
            this.hintId = hintId;
        }

        public boolean isSet() {
            return value;
        }

        public @StringRes int getLabel() {
            return labelId;
        }

        public @StringRes int getHint() {
            return hintId;
        }
    }

    public interface SettingChangesCallback {
        void onChange();
    }

    private static final Settings INSTANCE = new Settings();

    private PreferencesHelper prefs;
    private Set<SettingChangesCallback> callbacks = new HashSet<>();

    private Settings() {}

    private static boolean isInited() {
        return INSTANCE.prefs != null;
    }

    public static void init(Context context) {
        INSTANCE.prefs = new PreferencesHelper(context, FILE_NAME);
        INSTANCE.loadPreferences();
    }

    private void loadPreferences() {
        for (Bool pref : Bool.values()) {
            pref.value = prefs.getBoolean("bool_" + pref.name().toLowerCase(), pref.value);
        }
    }

    public static void set(Bool pref, boolean value) {
        if (isInited()) {
            pref.value = value;
            INSTANCE.prefs.putBoolean("bool_" + pref.name().toLowerCase(), value);
            INSTANCE.notifyChanges();
        } else {
            throw new IllegalStateException("Settings wasn't inited");
        }
    }

    private void notifyChanges() {
        Iterator<SettingChangesCallback> iterator = callbacks.iterator();
        while (iterator.hasNext()) {
            SettingChangesCallback callback = iterator.next();
            if (callback != null) {
                callback.onChange();
            }
        }
    }

    public void addCallback(SettingChangesCallback callback) {
        callbacks.add(callback);
    }

    public void removeCallback(SettingChangesCallback callback) {
        callbacks.remove(callback);
    }
}
