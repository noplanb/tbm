package com.noplanbees.tbm;

import android.util.Log;

public class ServerTest {
	private final String TAG = this.getClass().getSimpleName();
	
	public ServerTest(){
		
	}
	
	public void run(){
		Log.i(TAG, "run");
		new MyServer("http://www.google.com");
	}

	private class MyServer extends Server{

		public MyServer(String uri) {
			super(uri);
		}

		@Override
		public void callback(String response) {
			Log.i(TAG, "callback");
			Log.i(TAG, response);
		}
		
	}
}
