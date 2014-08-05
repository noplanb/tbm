package com.noplanbees.tbm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class FileDownloadService extends FileTransferService {
	private final static String STAG = FileDownloadService.class.getSimpleName();
	private final String TAG = getClass().getSimpleName();
	
	public static void restartTransfersPendingRetry(Context context) {
		Intent intent = new Intent(context, FileDownloadService.class);
		intent.setAction("INTERRUPT");
		context.startService(intent);
	}	
	
	public FileDownloadService() {
		super("FileDownloadService");
	}
	
	@Override
	protected Boolean doTransfer(Intent intent) throws InterruptedException{
		intent.putExtra(IntentFields.TRANSFER_TYPE_KEY, IntentFields.TRANSFER_TYPE_DOWNLOAD);
		reportStatus(intent, Video.IncomingVideoStatus.DOWNLOADING);
		return download(intent);
	}
	
	private Boolean download(Intent intent) throws InterruptedException{
		Log.e(TAG, "download videoId=" + intent.getStringExtra(IntentFields.VIDEO_ID_KEY) + " params=" + params.toString());
		File f = FileUtils.getFile(Config.downloadingFilePath(getApplicationContext()));
		try {
			URL url = new URL(urlWithParams);
			FileUtils.copyURLToFile(url, f, 60000, 60000);
		} catch (MalformedURLException e) {
			Log.e(TAG, "download2: MalformedURLException: " + e.getMessage() + e.toString());
			return false;
		} catch (IOException e) {
			Log.e(TAG, "download: IOException: e.tostring " +  e.toString() );
			if (e.getClass().equals(FileNotFoundException.class)){
				reportStatus(intent, Video.IncomingVideoStatus.FAILED_PERMANENTLY);
				return true;
			} else {
				return false;
			}
		}
		f.renameTo(FileUtils.getFile(filePath));
		Log.e(TAG, "download SUCCESS" + params.toString());
		reportStatus(intent, Video.IncomingVideoStatus.DOWNLOADED);
		return true;
	}

	@Override
	protected void maxRetriesReached(Intent intent) throws InterruptedException{
		reportStatus(intent, Video.IncomingVideoStatus.FAILED_PERMANENTLY);
	}
	
}
