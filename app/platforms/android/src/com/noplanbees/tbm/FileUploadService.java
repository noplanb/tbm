package com.noplanbees.tbm;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class FileUploadService extends IntentService {

	private final String SERVER_URL = "http://192.168.1.82:3000/upload";
	private final String TAG = this.getClass().getSimpleName();
	private final String attachmentName = "vid";
	private final String attachmentFileName = "vid.mp4";
	private final String crlf = "\r\n";
	private final String twoHyphens = "--";
	private final String boundary =  "*****";
	private final String quote =  "\"";

	public FileUploadService() {
		super("FileUploadService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {


		Bundle extras = intent.getExtras();
//		String filePath = (String) extras.get("filePath");
		String filePath = "/storage/sdcard0/Movies/tbm/last.mp4";
		String toId = (String) extras.get("toId");

		Log.i(TAG, "onHandleIntent: " + filePath + " " + toId);

		HttpURLConnection con = null;
		try {
			File f = new File(filePath);
			Log.i(TAG, String.format("File is %d bytes", f.length()));

			URL url = new URL(SERVER_URL + "?to_id=" + toId);
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
			out.writeBytes(twoHyphens + boundary + crlf);
			out.writeBytes("Content-Disposition: form-data; ");
//			out.writeBytes("Content-Type=video/mp4;");
			out.writeBytes("name=\"" + attachmentName + "\";filename=\"" + attachmentFileName + "\";");
			out.writeBytes(crlf + crlf);

            byte[] fileData = new byte[(int) f.length()];
            DataInputStream dis = new DataInputStream(new FileInputStream(f));
            dis.readFully(fileData);
            dis.close();
            out.write(fileData);
			Log.i(TAG, String.format("Wrote %d bytes", fileData.length));

//			FileReader fr = new FileReader(filePath);
//			int b;
//			int count = 0;
//			while ( (b=fr.read()) != -1 ){
//				out.write(b);
//				count ++;
//			}
//			Log.i(TAG, String.format("Wrote %d bytes", count));
			
//			out.writeBytes("some text");

			out.writeBytes(crlf);
			out.writeBytes(twoHyphens + boundary + twoHyphens + crlf);
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
			Log.e(TAG, "IOException " + e.getMessage());
		} finally {
			con.disconnect();
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
