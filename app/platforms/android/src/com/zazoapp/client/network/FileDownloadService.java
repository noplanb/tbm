package com.zazoapp.client.network;

import android.content.Context;
import android.content.Intent;

import com.zazoapp.client.model.Video;


public class FileDownloadService extends FileTransferService {
	private final static String TAG = FileDownloadService.class.getSimpleName();
	
	public static void restartTransfersPendingRetry(Context context) {
		Intent intent = new Intent(context, FileDownloadService.class);
		intent.setAction(ACTION_INTERRUPT);
		context.startService(intent);
	}	
	
	public FileDownloadService() {
		super("FileDownloadService");
	}
	
	@Override
	protected boolean doTransfer(Intent intent) throws InterruptedException{
		intent.putExtra(IntentFields.TRANSFER_TYPE_KEY, IntentFields.TRANSFER_TYPE_DOWNLOAD);
		reportStatus(intent, Video.IncomingVideoStatus.DOWNLOADING);
		return fileTransferAgent.download();
	}
	
	@Override
	protected void maxRetriesReached(Intent intent) throws InterruptedException{
		reportStatus(intent, Video.IncomingVideoStatus.FAILED_PERMANENTLY);
	}
}
