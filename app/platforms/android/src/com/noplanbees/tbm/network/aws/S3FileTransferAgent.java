package com.noplanbees.tbm.network.aws;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transfermanager.Download;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transfermanager.Upload;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.noplanbees.tbm.DataHolderService;
import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.Video;
import com.noplanbees.tbm.crash_dispatcher.Dispatch;
import com.noplanbees.tbm.network.FileTransferService.IntentFields;
import com.noplanbees.tbm.network.IFileTransferAgent;

import java.io.File;

public class S3FileTransferAgent implements IFileTransferAgent {
	private static final String TAG = S3FileTransferAgent.class.getSimpleName();

    private final String s3Bucket;

    private TransferManager tm;
	private Intent intent;
	private File file;
	private String filename;
	private Context context;

	public S3FileTransferAgent(Context context) {
		this.context = context;
        SharedPreferenceManager sharedPreferenceManager = SharedPreferenceManager.getSharedPreferenceManager(context);
        tm = new TransferManager(new BasicAWSCredentials(sharedPreferenceManager.getS3AccessKey(),
                sharedPreferenceManager.getS3SecretKey()));
        s3Bucket = sharedPreferenceManager.getS3Bucket();
        AmazonS3 client = tm.getAmazonS3Client();
        client.setRegion(Region.getRegion(Regions.valueOf(sharedPreferenceManager.getS3Region().toUpperCase()
                .replace('-', '_'))));
    }
	
	@Override
	public void setInstanceVariables(Intent intent) throws InterruptedException {
		this.filename = intent.getStringExtra(IntentFields.FILE_NAME_KEY);
		this.intent = intent;
		String filePath = intent.getStringExtra(IntentFields.FILE_PATH_KEY);
		this.file = new File(filePath);
	}

	@Override
	public boolean upload() {
        PutObjectRequest _putObjectRequest = new PutObjectRequest(s3Bucket, filename, file);
		try {
			Upload upload = tm.upload(_putObjectRequest);
			upload.waitForUploadResult();
		} catch (AmazonServiceException e) {
			checkServiceException(e);
			return false;
		} catch (AmazonClientException e) {
			return checkClientException(e);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		reportStatus(intent, Friend.OutgoingVideoStatus.UPLOADED);
		return true;
	}

	@Override
	public boolean download() throws IllegalStateException{
		GetObjectRequest _getObjectRequest = new GetObjectRequest(s3Bucket, filename);
		try {
			Download download = tm.download(_getObjectRequest,	file);
			download.waitForCompletion();
		} catch (AmazonServiceException e) {
			checkServiceException(e);
			return false;
		} catch (AmazonClientException e) {
			return checkClientException(e);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		reportStatus(intent, Video.IncomingVideoStatus.DOWNLOADED);
		return true;
	}

	private boolean checkClientException(AmazonClientException e) {
        //e.g
		//Dispatch.dispatch("AmazonClientException[ " + e.isRetryable() + "]: " + e.getLocalizedMessage());
        return true;//checkErrorCode(e.getStatusCode());
	}

	private void checkServiceException(AmazonServiceException e) {
		switch(e.getErrorType()){
		case Client:
			Dispatch.dispatch("AmazonServiceException(Client)[" + e.isRetryable() + "]: " + e.getErrorMessage() + ": " + e.getErrorCode());
			break;
		case Service:
			Dispatch.dispatch("AmazonServiceException(Service)[" + e.isRetryable() + "]: " + e.getErrorMessage() + ": " + e.getErrorCode());
		case Unknown:
			Dispatch.dispatch("AmazonServiceException(Unknown)[" + e.isRetryable() + "]: " + e.getErrorMessage() + ": " + e.getErrorCode());
			break;
		}
        checkErrorCode(e.getStatusCode());
	}

    private void checkErrorCode(int code){
        if((code>399 && code<500 && code!=404 || (code == 501) || code == 301)){
            reportStatus(intent, Video.IncomingVideoStatus.FAILED_PERMANENTLY);
            throw new IllegalStateException("S3 problem. Need to be reworked");
        }else if (code == 404){
            reportStatus(intent, Video.IncomingVideoStatus.FAILED_PERMANENTLY);
            throw new IllegalStateException("S3 problem. Need to be reworked");
        }else if((code>499 && code<600 && code!=501) || (code>299 && code<400 && code!=301)){
            return;
        }
    }
	
	protected void reportStatus(Intent intent, int status){
		Log.i(TAG, "reportStatus");
		intent.setClass(context, DataHolderService.class);
		intent.putExtra(IntentFields.STATUS_KEY, status);
		context.startService(intent);
	}

	@Override
	public boolean delete() {
		DeleteObjectRequest _deleteObjectRequest = new DeleteObjectRequest(s3Bucket, filename);
		try {
			tm.getAmazonS3Client().deleteObject(_deleteObjectRequest );
		} catch (AmazonServiceException e) {
			checkServiceException(e);
			return false;
		} catch (AmazonClientException e) {
			return checkClientException(e);
		}
		return true;
	}
}
