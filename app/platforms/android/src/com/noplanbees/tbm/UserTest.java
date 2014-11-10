package com.noplanbees.tbm;

import android.content.Context;
import android.util.Log;


public class UserTest {
	public final static String TAG = "ConfigTest";

	public static void run(Context context) {	
		UserFactory cf = UserFactory.getFactoryInstance();
		cf.destroyAll(context);
		User c0 = cf.makeInstance(context);
		User c1 = cf.makeInstance(context);
		Log.i(TAG, String.format("Number of config instances = %d. Should never be more than 1", cf.instances.size()));
		
		c0.set("first_name", "first");
		
		Log.i(TAG, "first name = " + cf.instances.get(0).get("first_name"));
		
		Log.i(TAG, "Saving...");
	    cf.save(context);
		
		Log.i(TAG, "Retrieving...");
		cf.retrieve(context);
		
		Log.i(TAG, "first name = " + cf.instances.get(0).get("first_name"));

		Log.i(TAG, "Done");
	}
}
