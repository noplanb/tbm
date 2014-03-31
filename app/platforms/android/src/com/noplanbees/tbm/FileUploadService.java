package com.noplanbees.tbm;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class FileUploadService extends NonStopIntentService {

	private final static int MAX_RETRIES = 100;
	private final String SERVER_URL = Config.fullUrl("/videos/create");

	private final String TAG = this.getClass().getSimpleName();
	private final String boundary =  "*****";

	public FileUploadService() {
		super("FileUploadService");
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy");
	}

	@Override
	protected void onHandleIntent(Intent intent, int startId) {
		if (intent == null){
			Log.i(TAG, "onHandleIntent: got null intent");
		} else {
			Log.i(TAG, "onHandleIntent: startId=" + startId);
			upload(intent, startId);
		}
	}

	private void upload(Intent intent, int startId){
		Bundle extras = intent.getExtras();
		String filePath = (String) extras.get("filePath");
		String receiverId = (String) extras.get("receiverId");
		String userId = (String) extras.get("userId");

		Log.i(TAG, "onHandleIntent: " + filePath + " " + extras.toString());

		HttpURLConnection con = null;
		try {
			File f = new File(filePath);
			Log.i(TAG, String.format("File is %d bytes", f.length()));

			URL url = new URL(SERVER_URL + "?receiver_id=" + receiverId + "&user_id=" + userId);
			con = (HttpURLConnection) url.openConnection();
			con.setDoOutput(true);
			con.setChunkedStreamingMode(0);
			con.setUseCaches(false);
			con.setDoOutput(true);

			con.setRequestMethod("POST");
			con.setRequestProperty("Connection", "Keep-Alive");
			con.setRequestProperty("Cache-Control", "no-cache");
			con.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

			DataOutputStream out = new DataOutputStream(con.getOutputStream());

			String preHeader = "--"+boundary+"\r\n";
			preHeader += "Content-Disposition: form-data; name=\"file\"; filename=\"vid.mp4\"\r\n";
			preHeader += "Content-Type: video/mp4\r\n";
			preHeader += "Content-Transfer-Encoding: binary\r\n";
			preHeader += "\r\n";
			out.writeBytes(preHeader);


			byte[] fileData = new byte[(int) f.length()];
			DataInputStream dis = new DataInputStream(new FileInputStream(f));
			dis.readFully(fileData);
			dis.close();
			out.write(fileData);
			Log.i(TAG, String.format("Wrote %d bytes", fileData.length));

			String postString = "\r\n--"+boundary+"--\r\n";
			out.writeBytes(postString);

			out.flush();
			out.close();

			BufferedReader in = new BufferedReader( new InputStreamReader(con.getInputStream()) );
			String l;
			while ( (l = in.readLine()) != null ){
				Log.i(TAG, l);
			}
			in.close();

		} catch (MalformedURLException e) {
			Log.e(TAG, "MalformedURLException " + e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, "IOException retrying..." + e.getMessage());
			retry(intent, startId);
		} finally {
			con.disconnect();
			stopSelf(startId);
		}
	}

	private void retry(Intent intent, int startId) {
		Bundle extras = intent.getExtras();
		Integer retryCount = (Integer) extras.get("retryCount");
		Long delay = (long) (1000 * (1 << retryCount));
		retryCount ++;

		if (retryCount < MAX_RETRIES){
			Log.i(TAG, "startId=" + startId + " retry " +  retryCount.toString() + String.format(" will retry in %d seconds", delay/1000));
			intent.putExtra("retryCount", retryCount);
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				Log.e(TAG, "retry interrupted: " + e.getMessage());
			}
			upload(intent, startId);
		} else {
			Log.e(TAG, "max retries reached for startId=" + startId);
			stopSelf(startId);
		}
	}

	private void get(Intent intent){
		Bundle extras = intent.getExtras();
		String filePath = (String) extras.get("filePath");
		String toId = (String) extras.get("toId");

		Log.i(TAG, "onHandleIntent: " + filePath + " " + toId);

		HttpURLConnection con = null;
		InputStream in = null;
		try {
			URL url = new URL(SERVER_URL + "?to_id=" + toId);
			con = (HttpURLConnection) url.openConnection();
			in = new BufferedInputStream(con.getInputStream());
			String resp = readStream(in);
			Log.i(TAG, "Got response - ");
			Log.i(TAG, resp);
			in.close();

		} catch (MalformedURLException e) {
			Log.e(TAG, "MalformedURLException " + e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, "IOException " + e.getMessage());
		} finally {
			con.disconnect();
		}
	}

	private String readStream(InputStream is) throws IOException {
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		int i = is.read();
		Log.i(TAG, String.format("readStream: First byte is %d", i));
		while(i != -1) {
			bo.write(i);
			i = is.read();
		}
		return bo.toString();
	}
}
