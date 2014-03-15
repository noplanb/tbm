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

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

public class ActiveModelFactory {
	private String TAG = this.getClass().getSimpleName();

	public ArrayList<ActiveModel> instances = new ArrayList<ActiveModel>();

	//--------------------
	// Factory
	//--------------------
	protected ActiveModel makeInstance(){
		ActiveModel i = new ActiveModel();
		i.init();
		instances.add(i);
		return i;
	}

	public void destroyAll(){
		instances.clear();
	}


	//--------------------
	// Save and retrieve
	//--------------------
	public String save(){
		if (instances == null || instances.isEmpty())
			return "";

		ArrayList <LinkedTreeMap<String, String>> all = new ArrayList<LinkedTreeMap<String, String>>();
		for (ActiveModel i : instances){
			all.add(i.attributes);
		}
		Gson g = new Gson();
		String j = g.toJson(all);
		try {
			File f = new File(Config.getVideoDir(), this.getClass().getSimpleName() + "_saved_instances.json");
			if (f.exists())
				f.delete();
			FileOutputStream fos = new FileOutputStream(f, true);
			fos = new FileOutputStream(f, true);
			OutputStreamWriter osw = new OutputStreamWriter(fos);
			osw.write(j);
			osw.close();
			fos.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
		return j;
	}

	public void retrieve(){
		instances.clear();
		String json = null;
		try {
			File f = new File(Config.getVideoDir(), this.getClass().getSimpleName() + "_saved_instances.json");
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
			Log.e(TAG, e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
		Log.i(TAG, "retrieve(): Got json from file = " + json);

		ArrayList<LinkedTreeMap<String, String>> all = new ArrayList<LinkedTreeMap<String, String>>();
		Gson g = new Gson();
		all = g.fromJson(json, all.getClass());

		Log.i(TAG, "class of attr: " + all.get(0).getClass().getSimpleName());
		
		for (LinkedTreeMap<String, String> ats : all){
			ActiveModel i = makeInstance();
			i.attributes.clear();
			i.attributes.putAll(ats);
		}

	}



	//--------------------
	// Finders
	//--------------------
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

	public ActiveModel find(String id){
		return findWhere("id", id);
	}
}
