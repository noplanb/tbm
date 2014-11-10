package com.noplanbees.tbm;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.internal.LinkedTreeMap;

public abstract class Server {
	private static String STAG = Server.class.getSimpleName();

	public static class ParamKeys{
		public static final String RESPONSE_STATUS = "status";
		public static final String ERROR_TITLE = "title";
		public static final String ERROR_MSG = "msg";
	}

	public static class StatusValues{
		public static final String STATUS_SUCCESS = "success";
		public static final String STATUS_FAILURE = "failure";
	}

	public static boolean isSuccess(String status){
		Log.i(STAG, "is isSuccess:" + status);
		if (status == null)
			return false;
		if (status.equals(StatusValues.STATUS_SUCCESS))
			return true;
		else
			return false;
	}

	public static boolean isFailure(String status){
		return !isSuccess(status);
	}

	private String TAG = this.getClass().getSimpleName();
	private String uri;
	private String method;
	private String sParams;

	public Server(String uri, LinkedTreeMap<String, String> params, String method){
		this.uri = uri;
		this.method = method;
		sParams = paramsToString(params);
		new BgHttpReq().execute();
	}

	public Server(String uri, LinkedTreeMap<String, String> params){
		this.uri = uri; 
		sParams = paramsToString(params);
		method = "GET";
		new BgHttpReq().execute();
	}
	
	public Server(String uri){
		this.uri = uri; 
		sParams = "";
		method = "GET";
		new BgHttpReq().execute();
	}

	public abstract void success(String response);
	public abstract void error(String errorString);

	public String paramsToString(LinkedTreeMap<String, String> params){
		String s = "";
		for (String k: params.keySet()){
			s += Uri.encode(k) + "=" + Uri.encode(params.get(k)) + "&";
		}
		if (s.length() > 0 && s.charAt(s.length()-1)=='&') {
			s = s.substring(0, s.length()-1);
		}
		return s;
	}

	private boolean isPost(){
		return method.startsWith("POST") || method.startsWith("post");
	}

	public String httpReq() throws IOException, MalformedURLException{
		String sUrl = Config.fullUrl(uri);
		if ( !isPost() && sParams.length() > 0)
			sUrl += ( "?" + sParams );

		Log.i(TAG, "httpReq " + method + " url=" + sUrl +"  params=" + sParams);
		String result = "";

		URL url = new URL(sUrl);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();	
		con.setDoInput(true);
		con.setInstanceFollowRedirects(false); 
		con.setUseCaches (false);
		con.setRequestMethod(method);
		con.setRequestProperty("Accept-Charset", "UTF-8");

		if ( isPost() ){
			con.setDoOutput(true);
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
			con.setRequestProperty("Content-Length", "" + Integer.toString(sParams.getBytes().length));
		} else {
			con.setRequestProperty("Content-Length", "0");
		}

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

		return result;
	}

	private class BgHttpReq extends AsyncTask<Void, Void, HashMap<String, String>>{
		@Override
		protected HashMap<String, String> doInBackground(Void... params) {
			HashMap<String, String> r = new HashMap<String, String>();
			try {
				r.put("success", httpReq());
			} catch (MalformedURLException e) {
				r.put("error", e.toString());
			} catch (IOException e) {
				r.put("error", e.toString());
			}
			return r;
		}

		@Override
		protected void onPostExecute(HashMap<String, String> result) {
			if ( result.get("success") != null){
				success(result.get("success"));
			} else {
				error(result.get("error"));
			}
		}

	}

}
