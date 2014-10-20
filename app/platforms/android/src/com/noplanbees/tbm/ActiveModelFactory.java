package com.noplanbees.tbm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

public class ActiveModelFactory {
	private String TAG = this.getClass().getSimpleName();

	public ArrayList<ActiveModel> instances = new ArrayList<ActiveModel>();

	//--------------------
	// Factory
	//--------------------
	protected ActiveModel makeInstance(Context context){
		ActiveModel i = new ActiveModel();
		i.init(context);
		instances.add(i);
		return i;
	}

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
			Log.e(TAG, "ERROR: This should never happen." + e.getMessage() + e.toString());
			throw new RuntimeException();
		} catch (IOException e) {
			Log.e(TAG, "ERROR: This shold never happen. " + e.getMessage() + e.toString());
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
			Log.e(TAG, e.getMessage() + e.toString());
			return false;
		} catch (IOException e) {
			Log.e(TAG, e.getMessage() + e.toString());
			return false;
		}
		Log.i(TAG, "retrieve(): retrieved from file.");

		ArrayList<LinkedTreeMap<String, String>> all = new ArrayList<LinkedTreeMap<String, String>>();
		Gson g = new Gson();
		all = g.fromJson(json, all.getClass());

		Log.i(TAG, "class of attr: " + all.get(0).getClass().getSimpleName());

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


	public ActiveModel findWhere(String a, String v){
		ActiveModel found = null;
		for (ActiveModel i : instances){
			if (i.get(a).equals(v)){
				found = i;
				break;
			}
		}
		return found;
	}

	public ArrayList<ActiveModel> findAllWhere(String a, String v){
		ArrayList<ActiveModel> result = new ArrayList<ActiveModel>();
		for (ActiveModel i: instances) {
			if ( i.get(a).equals(v) ){
				result.add(i);
			}
		}
		return result;
	}

	public ActiveModel find(String id){
		return findWhere("id", id);
	}

	public boolean existsWithId(String id){
		if (find(id) == null)
			return false;
		else
			return true;
	}

	public void delete(String id){
		Integer found = null;
		Integer index = 0;
		for (ActiveModel am : instances){
			if (am.getId().equals(id)){
				found = index;
				break;
			}
			index ++;
		}
		if (found != null)
			instances.remove(found);	
	}

	public int count(){
		return instances.size();
	}

}
