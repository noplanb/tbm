package com.zazoapp.client.model;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.Config;
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
import java.util.Iterator;

public abstract class ActiveModelFactory<T extends ActiveModel> {
    private static final String TAG = ActiveModelFactory.class.getSimpleName();

    protected ArrayList<T> instances = new ArrayList<>();

    public abstract Class<T> getModelClass();

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
        return i;
    }

	public synchronized void destroyAll(Context context){
		instances.clear();
		File f = new File(getSaveFilePath(context));
		f.delete();
	}

	//--------------------
	// Save and retrieve
	//--------------------
	// See app_lifecycle.text for why these are synchronized.
	public synchronized String save(Context context){
		if (instances == null || instances.isEmpty())
			return "";

		ArrayList <LinkedTreeMap<String, String>> all = new ArrayList<>();
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
		} catch (FileNotFoundException e) {
			Dispatch.dispatch("ERROR: This should never happen." + e.getMessage() + e.toString());
			throw new RuntimeException();
		} catch (IOException e) {
            Dispatch.dispatch("ERROR: This shold never happen. " + e.getMessage() + e.toString());
			throw new RuntimeException();
		}
		return j;
	}

	public synchronized boolean retrieve(Context context){
		instances.clear();
		String json = null;
		try {
			File f = new File(getSaveFilePath(context));
			FileInputStream fis = new FileInputStream(f);
			InputStreamReader isr = new InputStreamReader(fis);
			BufferedReader br = new BufferedReader(isr);
			String s = "";
			StringBuilder sb = new StringBuilder();
			while ( (s = br.readLine()) != null){
				sb.append(s);
			}
			json = sb.toString();
			br.close();
			isr.close();
			fis.close();
		}
		catch (FileNotFoundException e) {
            Log.i(TAG, e.getMessage() + e.toString());
			return false;
		} catch (IOException e) {
            Dispatch.dispatch(e.getMessage() + e.toString());
			return false;
		}
		Log.i(TAG, "retrieve(): retrieved from file.");

		ArrayList<LinkedTreeMap<String, String>> all = new ArrayList<LinkedTreeMap<String, String>>();
		Gson g = new Gson();
		all = g.fromJson(json, all.getClass());

		if (all == null){
			Log.i(TAG, "retrieve: got null for objects");
			return false;
		} else {
			Log.i(TAG, "class of attr: " + all.get(0).getClass().getSimpleName());
		}

		for (LinkedTreeMap<String, String> ats : all){
			T i = makeInstance(context);
			i.attributes.clear();
			i.attributes.putAll(ats);
		}
		return true;
	}

	public String getSaveFilePath(Context context){
		return Config.homeDirPath(context) + "/" + this.getClass().getSimpleName() + "_saved_instances.json";
	}

	public void deleteSaveFile(Context context){
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
	public synchronized boolean hasInstances(){
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
                break;
            }
        }
    }

    public synchronized int count() {
        return instances.size();
    }

}
