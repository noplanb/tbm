package com.noplanbees.tbm;

import android.util.Log;

import com.google.gson.internal.LinkedTreeMap;

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
		new ServerTestU("/reg/echo");
		
        // Get relative url params
		new ServerTestUP("reg/echo", params);
		
		// Post absolute url params
		new ServerTestUPM("http://192.168.1.82:3000/reg/echo", params, "POST");
	}

	private class ServerTestUPM extends Server{
		public ServerTestUPM(String uri, LinkedTreeMap<String, String> params, String method) {
			super(uri, params, method);
		}
		@Override
		public void success(String response) {
			Log.i(TAG, "callback");
		}
		@Override
		public void error(String errorString) {
			// TODO Auto-generated method stub
			
		}
	}
	
	private class ServerTestUP extends Server{
		public ServerTestUP(String uri, LinkedTreeMap<String, String> params) {
			super(uri, params);
		}
		@Override
		public void success(String response) {
			Log.i(TAG, "callback");
		}
		@Override
		public void error(String errorString) {
			// TODO Auto-generated method stub
			
		}
	}
	
	private class ServerTestU extends Server{
		public ServerTestU(String uri) {
			super(uri);
		}
		@Override
		public void success(String response) {
			Log.i(TAG, "callback");

		}
		@Override
		public void error(String errorString) {
			// TODO Auto-generated method stub
			
		}
	}
}
