package com.noplanbees.tbm;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import com.noplanbees.tbm.FileTransferService.IntentFields;

import android.content.Intent;
import android.util.Log;

public class FileDownloadService extends FileTransferService {
	private final String TAG = getClass().getSimpleName();
	
	@Override
	protected Boolean doTransfer(Intent intent) {
		intent.putExtra(IntentFields.TRANSFER_TYPE_KEY, IntentFields.TRANSFER_TYPE_DOWNLOAD);
		reportStatus(intent, Friend.IncomingVideoStatus.DOWNLOADING);
		if (download()){
			reportStatus(intent, Friend.IncomingVideoStatus.DOWNLOADED);
			return true;
		} else {
			return false;
		}
	}
	
	private Boolean download(){
		File f = FileUtils.getFile(Config.downloadingFilePath(getApplicationContext()));
		try {
			URL url = new URL(urlWithParams);
			FileUtils.copyURLToFile(url, f, 60000, 60000);
		} catch (MalformedURLException e) {
			Log.e(TAG, "download2: MalformedURLException: " + e.getMessage() + e.toString());
			return false;
		} catch (IOException e) {
			Log.e(TAG, "download2: IOException: " + e.getMessage() + e.toString());
			return false;
		}
		f.renameTo(FileUtils.getFile(filePath));
		return true;
	}

}
