package com.zazoapp.client.network;

import android.content.Context;
import android.content.Intent;

import com.zazoapp.client.model.Friend;


public class FileUploadService extends FileTransferService {
	private final String TAG = getClass().getSimpleName();
	private static final String STAG = FileTransferService.class.getSimpleName();
	
	private final String boundary =  "*****";
	
	
	public static void restartTransfersPendingRetry(Context context) {
		Intent intent = new Intent(context, FileUploadService.class);
		intent.setAction(ACTION_INTERRUPT);
		context.startService(intent);
	}	
	
	public FileUploadService() {
		super("FileUploadService");
	}
	
	@Override
	protected boolean doTransfer(Intent intent)throws InterruptedException{
		intent.putExtra(IntentFields.TRANSFER_TYPE_KEY, IntentFields.TRANSFER_TYPE_UPLOAD);
		reportStatus(intent, Friend.OutgoingVideoStatus.UPLOADING);
		return fileTransferAgent.upload();
	}
	
	@Override
	protected void maxRetriesReached(Intent intent) throws InterruptedException{
		reportStatus(intent, Friend.OutgoingVideoStatus.FAILED_PERMANENTLY);
	}


}
