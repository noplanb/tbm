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

public abstract class ActiveModelFactory {
	private static final String TAG = ActiveModelFactory.class.getSimpleName();

	public ArrayList<ActiveModel> instances = new ArrayList<ActiveModel>();

	//--------------------
	// Factory
	//--------------------
	abstract protected ActiveModel makeInstance(Context context);

	public void destroyAll(Context context){
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

		ArrayList <LinkedTreeMap<String, String>> all = new ArrayList<LinkedTreeMap<String, String>>();
		for (ActiveModel i : instances){
			all.add(i.attributes);
		}
		Gson g = new Gson();
		String j = g.toJson(all);
		try {
			File f = new File(getSaveFilePath(context));
			if (f.exists())
				f.delete();
			FileOutputStream fos = new FileOutputStream(f, true);
			fos = new FileOutputStream(f, true);
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
			ActiveModel i = makeInstance(context);
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



	//--------------------
	// Finders
	//--------------------
	public boolean hasInstances(){
		return !instances.isEmpty();
	}

    public ActiveModel findWhere(String a, String v) {
        Iterator<ActiveModel> it = instances.iterator();
        while (it.hasNext()) {
            ActiveModel model = it.next();
            if (model.get(a).equals(v)) {
                return model;
            }
        }
        return null;
    }

    public ArrayList<ActiveModel> findAllWhere(String a, String v) {
        ArrayList<ActiveModel> result = new ArrayList<ActiveModel>();
        Iterator<ActiveModel> it = instances.iterator();
        while (it.hasNext()) {
            ActiveModel model = it.next();
            if (model.get(a).equals(v)) {
                result.add(model);
            }
        }
        return result;
    }

	public ActiveModel find(String id){
		if (id == null)
			return null;
		return findWhere("id", id);
	}

	public boolean existsWithId(String id){
		if (find(id) == null)
			return false;
		else
			return true;
	}

    public void delete(String id) {
        Iterator<ActiveModel> it = instances.iterator();
        while (it.hasNext()) {
            ActiveModel model = it.next();
            if (model.getId().equals(id)) {
                it.remove();
                break;
            }
        }
    }

	public int count(){
		return instances.size();
	}

}
