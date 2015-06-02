package com.zazoapp.client.model;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.Config;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.dispatch.Dispatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class ActiveModelFactory<T extends ActiveModel> {
    private static final String TAG = ActiveModelFactory.class.getSimpleName();
    private static final String ATTR_VERSION = "model_version";

    protected ArrayList<T> instances = new ArrayList<>();

    public abstract Class<T> getModelClass();

    private Set<ModelChangeCallback> callbacks = new HashSet<>();
    private boolean notifyCallbacks = true;

    private LinkedTreeMap<String, String> modelData = new LinkedTreeMap<>();

    //--------------------
    // Factory
    //--------------------
    public synchronized T makeInstance(Context context) {
        T i = null;
        try {
            i = getModelClass().newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        i.init(context);
        instances.add(i);
        notifyCallbacks();
        return i;
    }

    public synchronized void destroyAll(Context context) {
        instances.clear();
        File f = new File(getSaveFilePath(context));
        f.delete();
        notifyCallbacks();
    }

    //--------------------
    // Save and retrieve
    //--------------------
    // See app_lifecycle.text for why these are synchronized.
    public synchronized String save(Context context) {
        if (instances == null || instances.isEmpty())
            return "";

        ArrayList<LinkedTreeMap<String, String>> all = new ArrayList<>();

        // save model version
        LinkedTreeMap<String, String> version = new LinkedTreeMap<>();
        version.put(ATTR_VERSION, TbmApplication.getVersionNumber());
        all.add(version);

        for (T i : instances) {
            all.add(i.attributes);
        }
        Gson g = new Gson();
        String j = g.toJson(all);
        try {
            File f = new File(getSaveFilePath(context));
            if (f.exists())
                f.delete();
            FileOutputStream fos = new FileOutputStream(f, true);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write(j);
            osw.close();
            fos.close();
        } catch (IOException e) {
            Dispatch.dispatch("ERROR: This should never happen." + e.getMessage() + e.toString());
            throw new RuntimeException();
        }
        modelData.put(ATTR_VERSION, TbmApplication.getVersionNumber());
        return j;
    }

    public synchronized boolean retrieve(Context context) {
        instances.clear();
        String json = null;
        try {
            File f = new File(getSaveFilePath(context));
            FileInputStream fis = new FileInputStream(f);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            String s = "";
            StringBuilder sb = new StringBuilder();
            while ((s = br.readLine()) != null) {
                sb.append(s);
            }
            json = sb.toString();
            br.close();
            isr.close();
            fis.close();
        } catch (FileNotFoundException e) {
            Log.i(TAG, e.getMessage() + e.toString());
            return false;
        } catch (IOException e) {
            Dispatch.dispatch(e.getMessage() + e.toString());
            return false;
        }
        Log.i(TAG, "retrieve(): retrieved from file.");

        ArrayList<LinkedTreeMap<String, String>> all = new ArrayList<>();
        Gson g = new Gson();
        all = g.fromJson(json, all.getClass());

        if (all == null) {
            Log.i(TAG, "retrieve: got null for objects");
            return false;
        } else {
            Log.i(TAG, "class of attr: " + all.get(0).getClass().getSimpleName());
        }

        notifyCallbacks = false;
        for (LinkedTreeMap<String, String> ats : all) {
            if (ats.containsKey(ATTR_VERSION)) {
                modelData.put(ATTR_VERSION, ats.get(ATTR_VERSION));
            } else {
                T i = makeInstance(context);
                i.attributes.clear();
                i.attributes.putAll(ats);
            }
        }
        notifyCallbacks = true;
        notifyCallbacks();
        return true;
    }

    public String getSaveFilePath(Context context) {
        return Config.homeDirPath(context) + "/" + this.getClass().getSimpleName() + "_saved_instances.json";
    }

    public void deleteSaveFile(Context context) {
        File f = new File(getSaveFilePath(context));
        f.delete();
    }

    public ArrayList<T> all() {
        ArrayList<T> all;
        synchronized (this) {
            all = new ArrayList<>(instances);
        }
        return all;
    }

    public ArrayList<T> allWhere(String a, String v) {
        ArrayList<T> all = all();
        ArrayList<T> result = new ArrayList<>();
        for (T t : all) {
            if (t.get(a).equals(v)) {
                result.add(t);
            }
        }
        return result;
    }

    //--------------------
    // Finders
    //--------------------
    public synchronized boolean hasInstances() {
        return !instances.isEmpty();
    }

    public T findWhere(String a, String v) {
        ArrayList<T> all = all();
        for (T t : all) {
            if (t.get(a).equals(v)) {
                return t;
            }
        }
        return null;
    }

    public T find(String id) {
        if (id == null)
            return null;
        return findWhere("id", id);
    }

    public boolean existsWithId(String id) {
        return find(id) != null;
    }

    public synchronized void delete(String id) {
        Iterator<T> it = instances.iterator();
        while (it.hasNext()) {
            T model = it.next();
            if (model.getId().equals(id)) {
                it.remove();
                notifyCallbacks();
                break;
            }
        }
    }

    public synchronized int count() {
        return instances.size();
    }

    public final void addCallback(ModelChangeCallback callback) {
        callbacks.add(callback);
    }

    public final void removeCallback(ModelChangeCallback callback) {
        callbacks.remove(callback);
    }

    protected void notifyCallbacks() {
        if (notifyCallbacks) {
            for (ModelChangeCallback callback : callbacks) {
                callback.onModelChanged(this);
            }
        }
    }

    public int getModelVersion() {
        String version = modelData.get(ATTR_VERSION);
        return (version == null) ? 1 : Integer.parseInt(version);
    }

    public interface ModelChangeCallback {
        void onModelChanged(ActiveModelFactory<?> factory);
    }
}
