package com.noplanbees.tbm;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Intent;
import android.util.Log;

public class FileUploadService extends FileTransferService {
	private final String TAG = getClass().getSimpleName();
	
	private final String boundary =  "*****";
	
	@Override
	protected Boolean doTransfer(Intent intent) {	
		intent.putExtra(IntentFields.TRANSFER_TYPE_KEY, IntentFields.TRANSFER_TYPE_UPLOAD);
		reportStatus(intent, Friend.OutgoingVideoStatus.UPLOADING);
		
		if (upload()){
			reportStatus(intent, Friend.OutgoingVideoStatus.UPLOADED);
			return true;
		} else {
			return false;
		}
	}
	
	private Boolean upload(){
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
			
			// GARF: need to check status from server and in case of an an error which indicates the file is never uploadable we 
			// need return true and report FAILED_PERMANENTLY.
		} catch (MalformedURLException e) {
			Log.e(TAG, "MalformedURLException " + e.toString());
			return false;
		} catch (IOException e) {
			Log.e(TAG, "IOException retrying..." + e.toString());
			return false;
		} finally {
			con.disconnect();
		}
		return true;
	}

	
}
