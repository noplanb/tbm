package com.noplanbees.tbm;

import android.util.Log;

public class ConfigTest {
	public final static String TAG = "ConfigTest";

	public static void run() {	
		ConfigFactory cf = ConfigFactory.getFactoryInstance();
		cf.destroyAll();
		Config c0 = cf.makeInstance();
		Config c1 = cf.makeInstance();
		Log.i(TAG, String.format("Number of config instances = %d. Should never be more than 1", cf.instances.size()));
		
		c0.set("first_name", "first");
		
		Log.i(TAG, "first name = " + cf.instances.get(0).get("first_name"));
		
		Log.i(TAG, "Saving...");
	    cf.save();
		
		Log.i(TAG, "Retrieving...");
		cf.retrieve();
		
		Log.i(TAG, "first name = " + cf.instances.get(0).get("first_name"));

		Log.i(TAG, "Done");
	}
}
