package com.zazoapp.client.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.network.aws.S3CredentialsStore;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by skamenkovych@codeminders.com on 3/31/2015.
 */
public final class PreferencesHelper {
    private static final String NAME = "zazo_preferences";
    private SharedPreferences sp;
    public PreferencesHelper(Context context) {
        sp = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public PreferencesHelper(Context context, String name) {
        sp = context.getSharedPreferences(name, Context.MODE_PRIVATE);
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

    public int getInt(String preference, int defValue) {
        if (preference == null || preference.isEmpty())
            throw new IllegalArgumentException("Preference name must not be empty");
        return sp.getInt(preference, defValue);
    }

    public void putInt(String preference, int value) {
        if (preference == null || preference.isEmpty())
            throw new IllegalArgumentException("Preference name must not be empty");
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(preference, value);
        editor.commit();
    }

    public void remove(String preference) {
        if (preference == null || preference.isEmpty())
            throw new IllegalArgumentException("Preference name must not be empty");
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(preference);
        editor.commit();
    }

    public Map<String, Pair<String, String>> getAll() {
        LinkedTreeMap<String, Pair<String, String>> prefs = new LinkedTreeMap<>();
        Map<String, ?> all = sp.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String type = entry.getValue().getClass().getSimpleName();
            prefs.put(entry.getKey(), Pair.create(type, entry.getValue().toString()));
        }
        return prefs;
    }

    public void putAll(Map<String, Pair<String, String>> all) {
        SharedPreferences.Editor editor = sp.edit();
        for (Map.Entry<String, Pair<String, String>> entry : all.entrySet()) {
            if (Boolean.class.getSimpleName().equals(entry.getValue().first)) {
                editor.putBoolean(entry.getKey(), Boolean.valueOf(entry.getValue().second));
            } else if (String.class.getSimpleName().equals(entry.getValue().first)) {
                editor.putString(entry.getKey(), String.valueOf(entry.getValue().second));
            } else if (Integer.class.getSimpleName().equals(entry.getValue().first)) {
                editor.putInt(entry.getKey(), Integer.valueOf(entry.getValue().second));
            }
        }
        editor.commit();
    }

    public static String toShortString(String name, PreferencesHelper helper) {
        Map<String, Pair<String, String>> map = helper.getAll();
        StringBuilder b = new StringBuilder(map.size() * 28);
        b.append(name).append(": ");
        if (map.isEmpty()) {
            b.append("{}");
        } else {
            b.append('{');
            Iterator<Map.Entry<String, Pair<String, String>>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Pair<String, String>> entry = it.next();
                String key = entry.getKey();
                b.append(key).append('=');
                if (key != null && (key.contains(S3CredentialsStore.SECRET_KEY) || key.contains(S3CredentialsStore.ACCESS_KEY_ID))) {
                    continue;
                }
                Pair<String, String> value = entry.getValue();
                b.append(value.second);
                if (it.hasNext()) {
                    b.append(", ");
                }
            }
            b.append('}');
        }
        return b.toString();
    }
}
