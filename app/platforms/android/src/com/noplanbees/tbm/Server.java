package com.noplanbees.tbm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import android.os.AsyncTask;
import android.util.Log;

public abstract class Server {
	private String TAG = this.getClass().getSimpleName();
    private String uri;

    public Server(String uri){
    	this.uri = uri;
    	new BgHttpGet().execute();
    }
    
    public abstract void callback(String response);

	public String httpGet(){
		String slash = "";
		if(uri.charAt(0) != '/')
			slash = "/";
		
		String sUrl;
		if (Pattern.compile("http:").matcher(uri).find()){
			sUrl = uri;
		} else {
			sUrl = Config.serverUri + slash + uri;
		}
		Log.i(TAG, "httpGet: url = " + sUrl);
		String result = "";
		try {
			URL url = new URL(sUrl);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();	
			BufferedReader br = new BufferedReader( new InputStreamReader(con.getInputStream()) );
			String l;
			while ( (l = br.readLine()) != null ){
				result += l;
			}
			br.close();
		} catch (MalformedURLException e) {
			Log.e(TAG, "get:" + e.getMessage());
			result = null;
		} catch (IOException e) {
			Log.e(TAG, "get:" + e.getMessage());
			result = null;
		}
		return result;
	}

	private class BgHttpGet extends AsyncTask<Void, Void, String>{
		@Override
		protected String doInBackground(Void... params) {
			return httpGet();
		}

		@Override
		protected void onPostExecute(String result) {
			callback(result);
		}
	}



}
