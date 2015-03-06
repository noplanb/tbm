package com.zazoapp.client.model;

import android.content.Context;
import android.util.Log;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.dispatch.Dispatch;

import java.util.HashSet;
import java.util.Set;

public abstract class ActiveModel {
	protected static final String TAG = ActiveModel.class.getSimpleName();

	public LinkedTreeMap<String, String> attributes = new LinkedTreeMap<String, String>();

	protected Context context;

    private Set<ModelChangeCallback> callbacks = new HashSet<>();

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
	public ActiveModel set(String a, String v){
		ActiveModel result = this;
		if ( attributes.containsKey(a) ) {
			Log.i(TAG, "setting " + a + " : " + v);
			attributes.put(a, v);
		} else {
			result = null;
            Dispatch.dispatch("ERROR: set: " + a + " is not an attr. This should neve happen");
			throw new RuntimeException();
		}
		return result;
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

    protected void notifyCallbacks() {
        for (ModelChangeCallback callback : callbacks) {
            callback.onModelChanged();
        }
    }

    public interface ModelChangeCallback {
        void onModelChanged();
    }
}
