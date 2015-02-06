package com.noplanbees.tbm.network;

import android.content.Context;
import android.content.Intent;

import com.noplanbees.tbm.RemoteStorageHandler;
import com.noplanbees.tbm.model.Video;


public class FileDeleteService extends FileTransferService {
	private final static String STAG = FileDeleteService.class.getSimpleName();
	private final String TAG = getClass().getSimpleName();
	
	public static void restartTransfersPendingRetry(Context context) {
		Intent intent = new Intent(context, FileDeleteService.class);
		intent.setAction("INTERRUPT");
		context.startService(intent);
	}	
	
	public FileDeleteService() {
		super("FileDeleteService");
	}
	
	@Override
	protected boolean doTransfer(Intent intent) throws InterruptedException{
		// Do not retry deletes always return true.
		fileTransferAgent.delete();
        return true;
    }
	
	@Override
	protected void maxRetriesReached(Intent intent) throws InterruptedException{
		reportStatus(intent, Video.IncomingVideoStatus.FAILED_PERMANENTLY);
	}
}
