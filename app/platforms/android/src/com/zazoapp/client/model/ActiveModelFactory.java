package com.zazoapp.client.model;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.Config;
import com.zazoapp.client.utilities.Convenience;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class ActiveModelFactory<T extends ActiveModel> implements ActiveModel.ModelChangeCallback {
    private static final String TAG = ActiveModelFactory.class.getSimpleName();

    protected ArrayList<T> instances = new ArrayList<>();

    public abstract Class<T> getModelClass();

    private Set<ModelChangeCallback> callbacks = new HashSet<>();
    private volatile boolean notifyCallbacks = true;
    private Runnable saveTask;

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
        i.addCallback(this);
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
        long start = System.currentTimeMillis();
        ArrayList<LinkedTreeMap<String, String>> all = new ArrayList<>();
        for (T i : instances) {
            all.add(i.attributes);
        }
        Gson g = new Gson();
        String j = g.toJson(all);
        Convenience.saveJsonToFile(j, getSaveFilePath(context));
        long time = System.currentTimeMillis() - start;
        Log.i(TAG, String.format("Saved %ss (%d) for %d ms", getModelClass().getSimpleName(), instances.size(), time));
        return j;
    }

    public synchronized boolean retrieve(Context context) {
        instances.clear();
        String json = Convenience.getJsonFromFile(getSaveFilePath(context));
        if (json == null) {
            return false;
        }
        Log.i(TAG, "retrieve(): retrieved from file.");

        ArrayList<LinkedTreeMap<String, String>> all = null;
        Gson g = new Gson();
        try {
            all = g.fromJson(json, ArrayList.class);
        } catch (JsonSyntaxException e) {
        }

        if (all == null) {
            Log.i(TAG, "retrieve: got null for objects");
            return false;
        } else if (all.size() > 0) {
            Log.i(TAG, "class of attr: " + all.get(0).getClass().getSimpleName());
        }

        notifyCallbacks = false;
        replaceAttributes(context, all);
        notifyCallbacks = true;
        notifyCallbacks();
        return true;
    }

    protected void replaceAttributes(Context context, ArrayList<LinkedTreeMap<String, String>> all) {
        for (LinkedTreeMap<String, String> ats : all) {
            T i = makeInstance(context);
            i.attributes.clear();
            i.attributes.putAll(ats);
        }
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

    @Override
    public void onModelUpdated(boolean changed) {
        if (changed) {
            notifyCallbacks();
        }
    }

    public Runnable getSaveTask(Context context) {
        final Context c = context.getApplicationContext();
        if (saveTask == null) {
            saveTask = new Runnable() {
                @Override
                public void run() {
                    save(c);
                }
            };
        }
        return saveTask;
    }

    /**
     * May be used to conceal changes.
     * Should be set back to <b>true</b> after.
     * @param notify set <b>true</b> to notify callbacks when model is changed, <b>false</b> to conceal changes
     */
    protected void notifyOnChanged(boolean notify) {
        notifyCallbacks = notify;
    }

    public interface ModelChangeCallback {
        void onModelChanged(ActiveModelFactory<?> factory);
    }
}
