package com.noplanbees.tbm.network;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.noplanbees.tbm.Config;
import com.noplanbees.tbm.DataHolderService;
import com.noplanbees.tbm.Video;
import com.noplanbees.tbm.FileTransferService.IntentFields;
import com.noplanbees.tbm.Friend;

public class ServerFileTransferAgent implements IFileTransferAgent {
	private final String TAG = getClass().getSimpleName();
	
	private final String boundary =  "*****";

	private Context context;
	protected String id;
	protected String filePath;
	protected Bundle params;

	private Intent intent;
	
	public ServerFileTransferAgent(Context context) {
		this.context = context;
	}
	
	@Override
	public void setInstanceVariables(Intent intent) {
		this.intent = intent;
		filePath = intent.getStringExtra(IntentFields.FILE_PATH_KEY);
		params = intent.getBundleExtra(IntentFields.PARAMS_KEY);
	}
	
	@Override
	public boolean upload() {
		String urlWithParams = Config.fileUploadUrl() + stringifyParams(params);
		Log.i(TAG, "upload: " + urlWithParams);

		HttpURLConnection con = null;
		try {
			File f = new File(filePath);
			Log.i(TAG, String.format("File is %d bytes", f.length()));

			URL url = new URL(urlWithParams);
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
			Log.e(TAG, "MalformedURLException " + e.toString());
			return false;
		} catch (IOException e) {
			Log.e(TAG, "IOException..." + e.toString());
			if (e.getClass().equals(FileNotFoundException.class)){
				reportStatus(intent, Friend.OutgoingVideoStatus.FAILED_PERMANENTLY);
				return false;
			} else {
				return false;
			}
		} finally {
			con.disconnect();
		}
		reportStatus(intent, Friend.OutgoingVideoStatus.UPLOADED);
		return true;
	}

	@Override
	public boolean download() {
		Log.e(TAG, "download videoId=" + intent.getStringExtra(IntentFields.VIDEO_ID_KEY) + " params=" + params.toString());
		File f = FileUtils.getFile(Config.downloadingFilePath(context));
		try {
			String urlWithParams = Config.fileDownloadUrl() + stringifyParams(params);
			URL url = new URL(urlWithParams);
			FileUtils.copyURLToFile(url, f, 60000, 60000);
		} catch (MalformedURLException e) {
			Log.e(TAG, "download2: MalformedURLException: " + e.getMessage() + e.toString());
			return false;
		} catch (IOException e) {
			Log.e(TAG, "download: IOException: e.tostring " +  e.toString() );
			if (e.getClass().equals(FileNotFoundException.class)){
				reportStatus(intent, Video.IncomingVideoStatus.FAILED_PERMANENTLY);
				return false;
			} else {
				return false;
			}
		}
		f.renameTo(FileUtils.getFile(filePath));
		Log.e(TAG, "download SUCCESS" + params.toString());
		reportStatus(intent, Video.IncomingVideoStatus.DOWNLOADED);
		return true;
	}
	
	protected void reportStatus(Intent intent, int status){
		Log.i(TAG, "reportStatus");
		intent.setClass(context, DataHolderService.class);
		intent.putExtra(IntentFields.STATUS_KEY, status);
		context.startService(intent);
	}


	private String stringifyParams(Bundle params){
		Set<String> keys = params.keySet();
		if (keys.isEmpty())
			return "";

		String result = "?";
		for (String key : keys){
			if (!result.equals("?"))
				result += "&";
			result += (key + "=" + params.getString(key));
		}
		return result;
	}	
}
