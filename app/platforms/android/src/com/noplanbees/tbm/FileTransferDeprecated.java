package com.noplanbees.tbm;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.os.AsyncTask;

public class FileTransferDeprecated {

	public static final int DOWN_LOAD = 0;
	public static final int UP_LOAD = 1;

	String filePath;
	String url;
	int direction;


	public FileTransferDeprecated (String url, String filePath, int direction){
		this.filePath = filePath;
		this.url = url;
		this.direction = direction;
	}
	
	public void aft(){
		new AsyncFT().execute();
	}

	public void upload() {
		URL url = null;
		try {
			url = new URL("http://www.android.com/");
		} catch (MalformedURLException e2) {
			e2.printStackTrace();
		}
		HttpURLConnection urlConnection = null;
		try {
			urlConnection = (HttpURLConnection) url.openConnection();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			InputStream in = new BufferedInputStream(urlConnection.getInputStream());
			System.out.print(readStream(in));
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			urlConnection.disconnect();
		}
	}

	private String readStream(InputStream is) {
		try {
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			int i = is.read();
			while(i != -1) {
				bo.write(i);
				i = is.read();
			}
			return bo.toString();
		} catch (IOException e) {
			return "";
		}
	}
	
	private class AsyncFT extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... params) {
			upload();
			return null;
		}

	}

}
