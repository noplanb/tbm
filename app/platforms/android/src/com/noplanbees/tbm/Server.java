package com.noplanbees.tbm;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.internal.LinkedTreeMap;

public abstract class Server {
	private String TAG = this.getClass().getSimpleName();
	private String uri;
	private String method = "GET";
	private String sParams = "";

	public Server(String uri, LinkedTreeMap<String, String> params, String method){
		this.uri = uri;
		this.method = method;
		new BgHttpGet().execute();
	}

	public Server(String uri, LinkedTreeMap<String, String> params){
		this.uri = uri; 
		new BgHttpGet().execute();
	}

	public Server(String uri){
		this.uri = uri; 
		new BgHttpGet().execute();
	}

	public abstract void callback(String response);

	public static String paramsToString(LinkedTreeMap<String, String> params){
		String s = "?";
		for (String k: params.keySet()){
			s += Uri.encode(k) + "=" + Uri.encode(params.get(k));
		}
		return s;
	}

	private boolean isPost(){
		return method.startsWith("POST") || method.startsWith("post");
	}

	public String httpReq(){
		String sUrl = Config.fullUrl(uri);
		if ( !isPost() )
			sUrl += sParams;

		Log.i(TAG, "httpGet: url = " + sUrl);
		String result = "";
		try {
			URL url = new URL(sUrl);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();	
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setInstanceFollowRedirects(false); 
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
			con.setUseCaches (false);

			if ( isPost() ){
				con.setRequestProperty("Content-Length", "" + Integer.toString(sParams.getBytes().length));
			} else {
				con.setRequestProperty("Content-Length", "0");
			}
			con.setRequestMethod(method);

			if ( isPost() ){
				DataOutputStream wr = new DataOutputStream(con.getOutputStream ());
				wr.writeBytes(sParams);
				wr.flush();
				wr.close();
			}

			BufferedReader br = new BufferedReader( new InputStreamReader(con.getInputStream()) );
			String l;
			while ( (l = br.readLine()) != null ){
				result += l;
			}
			br.close();
			con.disconnect();
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
			return httpReq();
		}

		@Override
		protected void onPostExecute(String result) {
			callback(result);
		}

	}

}
