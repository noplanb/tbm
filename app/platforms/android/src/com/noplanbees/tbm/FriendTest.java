package com.noplanbees.tbm;

import java.util.ArrayList;
import java.util.HashMap;

import android.util.Log;

import com.google.gson.Gson;


public class FriendTest {
	public final static String TAG = "FriendTest";

	public static void run() {		
		FriendFactory ff = FriendFactory.getFactoryInstance();
		ff.destroyAll();
		Friend f0 = ff.makeInstance();
		Friend f1 = ff.makeInstance();
		f0.set("id", "0");
		f1.set("id", "1");
		f0.set("firstName", "first0");
		f1.set("firstName", "first1");
		

		
		Log.i(TAG, "Testing find(): First name of f1 = " + ff.find("1").get("firstName"));
		
		Log.i(TAG, "Saving...");
	    ff.save();
		
		Log.i(TAG, "Retrieving...");
		ff.retrieve();
		
		Log.i(TAG, "First name of f1 = " + ff.find("1").get("firstName"));

		Log.i(TAG, "Done");

	}
}