package com.noplanbees.tbm.network.aws;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.mobileconnectors.s3.transfermanager.Download;
import com.amazonaws.mobileconnectors.s3.transfermanager.PersistableTransfer;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transfermanager.Upload;
import com.amazonaws.mobileconnectors.s3.transfermanager.internal.S3ProgressListener;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.noplanbees.tbm.DataHolderService;
import com.noplanbees.tbm.FileTransferService.IntentFields;
import com.noplanbees.tbm.Friend;
import com.noplanbees.tbm.Video;
import com.noplanbees.tbm.network.IFileTransferAgent;

public class S3FileTransferAgent implements IFileTransferAgent {
	private static final String TAG = S3FileTransferAgent.class.getSimpleName();

	private TransferManager tm;
	private Intent intent;
	private File file;
	private String id;
	private Context context;

	public S3FileTransferAgent(Context context) {
		this.context = context;
		tm = new TransferManager(new BasicAWSCredentials(Config.ACCESS_KEY_ID, Config.SECRET_KEY));
	}
	
	@Override
	public void setInstanceVariables(Intent intent) throws InterruptedException {
		this.id = intent.getStringExtra(IntentFields.VIDEO_ID_KEY);
		this.intent = intent;
		String filePath = intent.getStringExtra(IntentFields.FILE_PATH_KEY);
		this.file = new File(filePath);
	}

	@Override
	public boolean upload() {
		Log.d(TAG, "upload " + Config.BUCKET_NAME + ", " + id);
		PutObjectRequest _putObjectRequest = new PutObjectRequest(Config.BUCKET_NAME, id, file);
		//_putObjectRequest.set
		//_putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead);
		Upload upload = tm.upload(_putObjectRequest, new S3ProgressListener() {
			
			@Override
			public void progressChanged(ProgressEvent arg0) {
				Log.d(TAG, "progressChanged: " + arg0.getEventCode() + " : " + arg0.getBytesTransferred());
			}
			
			@Override
			public void onPersistableTransfer(PersistableTransfer arg0) {
				Log.d(TAG, "onPersistableTransfer " + arg0);
			}
		});
		try {
			upload.waitForUploadResult();
		} catch (AmazonServiceException e) {
			e.printStackTrace();
			return false;
		} catch (AmazonClientException e) {
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		reportStatus(intent, Friend.OutgoingVideoStatus.UPLOADED);
		return true;
	}

	@Override
	public boolean download() {
		Log.d(TAG, "download " + Config.BUCKET_NAME + ", " + id);
		GetObjectRequest _getObjectRequest = new GetObjectRequest(Config.BUCKET_NAME, id);
		Download download = tm.download(_getObjectRequest,	file, new S3ProgressListener() {
			
			@Override
			public void progressChanged(ProgressEvent arg0) {
				Log.d(TAG, "progressChanged: " + arg0.getEventCode() + " : " + arg0.getBytesTransferred());
			}
			
			@Override
			public void onPersistableTransfer(PersistableTransfer arg0) {
				Log.d(TAG, "onPersistableTransfer " + arg0);
			}
		});
		
		try {
			download.waitForCompletion();
		} catch (AmazonServiceException e) {
			e.printStackTrace();
			return false;
		} catch (AmazonClientException e) {
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		
		reportStatus(intent, Video.IncomingVideoStatus.DOWNLOADED);
		return true;
	}
	
	protected void reportStatus(Intent intent, int status){
		Log.i(TAG, "reportStatus");
		intent.setClass(context, DataHolderService.class);
		intent.putExtra(IntentFields.STATUS_KEY, status);
		context.startService(intent);
	}

}
