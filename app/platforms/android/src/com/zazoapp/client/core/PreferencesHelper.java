package com.zazoapp.client.core;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by skamenkovych@codeminders.com on 3/31/2015.
 */
public final class PreferencesHelper {
    private static final String NAME = "zazo_preferences";
    private SharedPreferences sp;
    public PreferencesHelper(Context context) {
        sp = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public String getString(String preference, String defValue) {
        if (preference == null || preference.isEmpty())
            throw new IllegalArgumentException("Preference name must not be empty");
        return sp.getString(preference, defValue);
    }

    public void putString(String preference, String value) {
        if (preference == null || preference.isEmpty())
            throw new IllegalArgumentException("Preference name must not be empty");
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(preference, value);
        editor.commit();
    }

    public boolean getBoolean(String preference, boolean defValue) {
        if (preference == null || preference.isEmpty())
            throw new IllegalArgumentException("Preference name must not be empty");
        return sp.getBoolean(preference, defValue);
    }

    public void putBoolean(String preference, boolean value) {
        if (preference == null || preference.isEmpty())
            throw new IllegalArgumentException("Preference name must not be empty");
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(preference, value);
        editor.commit();
    }
}
