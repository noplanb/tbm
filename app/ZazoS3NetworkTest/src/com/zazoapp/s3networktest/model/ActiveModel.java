package com.zazoapp.s3networktest.model;

import android.content.Context;
import android.util.Log;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.s3networktest.dispatch.Dispatch;

import java.util.HashSet;
import java.util.Set;

public abstract class ActiveModel {
	protected static final String TAG = ActiveModel.class.getSimpleName();
    public static final String TRUE = "true";
    public static final String FALSE = "false";

    public LinkedTreeMap<String, String> attributes = new LinkedTreeMap<String, String>();

    private Context context;

    private Set<ModelChangeCallback> callbacks = new HashSet<>();

    private volatile boolean notifyOnChanged = true;

	public void init(Context context){
		this.context = context;
		for(String atr : attributeList()){
			attributes.put(atr, "");
		}
	}

	// Must be overridden in subclass.	
	public abstract String[] attributeList();

    //--------------------
    // Getters and setters
    //--------------------
    public ActiveModel set(String a, String v) {
        if (attributes.containsKey(a)) {
            Log.i(TAG, "setting " + a + " : " + v);
            String oldValue = attributes.put(a, v);
            if (!v.equals(oldValue)) {
                notifyCallbacks(true);
            }
        } else {
            Dispatch.dispatch("ERROR: set: " + a + " is not an attr. This should neve happen");
            throw new RuntimeException();
        }
        return this;
    }

	public String get(String a){
		return attributes.get(a);
	}
	
	public String getId(){
		return attributes.get("id");
	}

    public final void addCallback(ModelChangeCallback callback) {
        callbacks.add(callback);
    }

    public final void removeCallback(ModelChangeCallback callback) {
        callbacks.remove(callback);
    }

    protected void notifyCallbacks(boolean changed) {
        if (notifyOnChanged) {
            for (ModelChangeCallback callback : callbacks) {
                callback.onModelUpdated(changed);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.getClass().getSimpleName());
        for (String s : attributes.values()) {
            builder.append(s).append(", ");
        }
        return builder.toString();
    }

    protected Context getContext() {
        return context;
    }

    /**
     * May be used to conceal changes before using {@link #set(String, String)}.
     * Should be set back to <b>true</b> after.
     * @param notify set <b>true</b> to notify callbacks when model is changed, <b>false</b> to conceal changes
     */
    protected void notifyOnChanged(boolean notify) {
        notifyOnChanged = notify;
    }

    public interface ModelChangeCallback {
        void onModelUpdated(boolean changed);
    }
}
