package com.zazoapp.client.network;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.Config;
import com.zazoapp.client.DispatcherService;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.Video;
import com.zazoapp.client.network.FileTransferService.IntentFields;
import org.apache.commons.io.FileUtils;

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

public class ServerFileTransferAgent implements IFileTransferAgent {
	private final String TAG = getClass().getSimpleName();
	
	private final String boundary =  "*****";

	private Context context;
	protected String id;
	protected String filePath;
	protected Bundle params;

	private Intent intent;

	private String filename;

	public ServerFileTransferAgent(Context context) {
		this.context = context;
	}
	
	@Override
	public void setInstanceVariables(Intent intent) {
		this.intent = intent;
        this.filePath = intent.getStringExtra(IntentFields.FILE_PATH_KEY);
        this.params = intent.getBundleExtra(IntentFields.PARAMS_KEY);
		this.filename = intent.getStringExtra(IntentFields.FILE_NAME_KEY);
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
			Dispatch.dispatch("MalformedURLException " + e.toString());
			return false;
		} catch (IOException e) {
			Dispatch.dispatch("IOException..." + e.toString());
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
		Log.i(TAG, "download videoId=" + intent.getStringExtra(IntentFields.VIDEO_ID_KEY) + " params=" + params.toString());
		File f = FileUtils.getFile(Config.downloadingFilePath(context));
		try {
			String urlWithParams = Config.fileDownloadUrl() + stringifyParams(params);
			URL url = new URL(urlWithParams);
			FileUtils.copyURLToFile(url, f, 60000, 60000);
		} catch (MalformedURLException e) {
			Dispatch.dispatch("download2: MalformedURLException: " + e.getMessage() + e.toString());
			return false;
		} catch (IOException e) {
			Dispatch.dispatch("download: IOException: e.tostring " + e.toString());
			if (e.getClass().equals(FileNotFoundException.class)){
				reportStatus(intent, Video.IncomingVideoStatus.FAILED_PERMANENTLY);
				return false;
			} else {
				return false;
			}
		}
        if (f.renameTo(FileUtils.getFile(filePath))) {
            Log.i(TAG, "download SUCCESS" + params.toString());
            reportStatus(intent, Video.IncomingVideoStatus.DOWNLOADED);
            return true;
        }
        Dispatch.dispatch("download: error renaming");
        return false;
    }

    @Override
	public boolean delete() {
		deleteRemoteFile(filename);
		return true;
	}
	
    public static void deleteRemoteFile(String filename){
        LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
        params.put("filename", filename);
		new DeleteRemote("videos/delete", params, "GET");
    }

    private static class DeleteRemote extends HttpRequest{
        public DeleteRemote (String uri, LinkedTreeMap<String, String> params, String method){
            super(uri, params, method);
        }
    }

	protected void reportStatus(Intent intent, int status){
		Log.i(TAG, "reportStatus");
		intent.setClass(context, DispatcherService.class);
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
