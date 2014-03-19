package com.noplanbees.tbm;

import com.google.gson.internal.LinkedTreeMap;

import android.util.Log;

public class ServerTest {
	private final String TAG = this.getClass().getSimpleName();
	
	public ServerTest(){
		
	}
	
	public void run(){
		Log.i(TAG, "run");
		LinkedTreeMap<String, String>params = new LinkedTreeMap<String, String>();
		params.put("key1", "value1");
		params.put("key2", "value2");
		
		// Get relative url
		new ServerTestU("/reg/server_test");
		
        // Get relative url params
		new ServerTestUP("reg/server_test", params);
		
		// Post absolute url params
		new ServerTestUPM("http://192.168.1.82:3000/reg/server_test", params, "POST");
	}

	private class ServerTestUPM extends Server{
		public ServerTestUPM(String uri, LinkedTreeMap<String, String> params, String method) {
			super(uri, params, method);
		}
		@Override
		public void callback(String response) {
			Log.i(TAG, "callback");
		}
	}
	
	private class ServerTestUP extends Server{
		public ServerTestUP(String uri, LinkedTreeMap<String, String> params) {
			super(uri, params);
		}
		@Override
		public void callback(String response) {
			Log.i(TAG, "callback");
		}
	}
	
	private class ServerTestU extends Server{
		public ServerTestU(String uri) {
			super(uri);
		}
		@Override
		public void callback(String response) {
			Log.i(TAG, "callback");

		}
	}
}
