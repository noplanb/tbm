package com.noplanbees.tbm.network.aws;

import java.io.File;

import android.content.Intent;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonServiceException.ErrorType;
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
import com.noplanbees.tbm.dispatch.Dispatch;
import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.Video;
import com.noplanbees.tbm.network.FileTransferService;
import com.noplanbees.tbm.network.FileTransferService.IntentFields;
import com.noplanbees.tbm.network.IFileTransferAgent;

public class S3FileTransferAgent implements IFileTransferAgent {
	private static final String TAG = S3FileTransferAgent.class.getSimpleName();

    private final String s3Bucket;

    private TransferManager tm;
	private Intent intent;
	private File file;
	private String filename;
	private FileTransferService fileTransferService;

	public S3FileTransferAgent(FileTransferService fts) {
		fileTransferService = fts;
        SharedPreferenceManager sharedPreferenceManager = SharedPreferenceManager.getSharedPreferenceManager(fileTransferService);
        tm = new TransferManager(new BasicAWSCredentials(sharedPreferenceManager.getS3AccessKey(), sharedPreferenceManager.getS3SecretKey()));
        s3Bucket = sharedPreferenceManager.getS3Bucket();
        AmazonS3 client = tm.getAmazonS3Client();
        try {
            client.setRegion(Region.getRegion(Regions.valueOf(sharedPreferenceManager.getS3Region().toUpperCase().replace('-', '_'))));
        } catch (IllegalArgumentException e) {
            Dispatch.dispatch("S3FileTransferAgent: cant set region: " + e.toString());
        }
    }
	
	@Override
	public void setInstanceVariables(Intent intent) throws InterruptedException {
		this.filename = intent.getStringExtra(IntentFields.FILE_NAME_KEY);
		this.intent = intent;
		String filePath = intent.getStringExtra(IntentFields.FILE_PATH_KEY);
		this.file = new File(filePath);
	}

	@Override
	public boolean upload() throws InterruptedException {
		try {
            PutObjectRequest _putObjectRequest = new PutObjectRequest(s3Bucket, filename, file);
			Upload upload = tm.upload(_putObjectRequest);
			upload.waitForUploadResult();
		} catch (AmazonServiceException e) {
			handleServiceException(e);
			return notRetryableServiceException(e);
		} catch (AmazonClientException e) {
			logClientException(e);
			return false;
		}
		fileTransferService.reportStatus(intent, Friend.OutgoingVideoStatus.UPLOADED);
		return true;
	}

	@Override
	public boolean download() throws InterruptedException{
		Log.i(TAG, "Starting S3 download for filename: " + filename);
		try {
            GetObjectRequest _getObjectRequest = new GetObjectRequest(s3Bucket, filename);
			Download download = tm.download(_getObjectRequest,	file);
			download.waitForCompletion();
		} catch (AmazonServiceException e) {
			handleServiceException(e);
			return notRetryableServiceException(e);
		} catch (AmazonClientException e) {
			logClientException(e);
			return false;
		} 
		fileTransferService.reportStatus(intent, Video.IncomingVideoStatus.DOWNLOADED);
		return true;
	}
	

	@Override
	public boolean delete() throws InterruptedException{
		try {
            DeleteObjectRequest _deleteObjectRequest = new DeleteObjectRequest(s3Bucket, filename);
			tm.getAmazonS3Client().deleteObject(_deleteObjectRequest );
		} catch (AmazonServiceException e) {
			handleServiceException(e);
			return notRetryableServiceException(e);
		} catch (AmazonClientException e) {
			logClientException(e);
			return false;
		}
		return true;
	}
	
	//------------
	// Convenience
	//------------
	private boolean isUpload(){
		return intent.getStringExtra(IntentFields.TRANSFER_TYPE_KEY).equalsIgnoreCase(IntentFields.TRANSFER_TYPE_UPLOAD);
	}
	
	private boolean isDownload(){
		return intent.getStringExtra(IntentFields.TRANSFER_TYPE_KEY).equalsIgnoreCase(IntentFields.TRANSFER_TYPE_DOWNLOAD);
	}
	
	//-------------------------
	// Client Exception helpers
	//-------------------------
	private void logClientException(AmazonClientException e) {
		Log.e(TAG, "ERROR in transfer type: " + intent.getStringExtra(IntentFields.TRANSFER_TYPE_KEY) + "AmazonClientException: " + e.toString());
        return;
	}
	
	//-------------------------
	// ServiceException helpers
	//-------------------------
	private void handleServiceException(AmazonServiceException e) {
		reportServiceException(e);
		dispatchServiceException(e);
		refreshCredentialsIfNecessary(e);
	}

	private void reportServiceException(AmazonServiceException e){
		if (notRetryableServiceException(e)){
			if (isDownload()){
	            fileTransferService.reportStatus(intent, Video.IncomingVideoStatus.FAILED_PERMANENTLY);
			} else if (isUpload()) {
	            fileTransferService.reportStatus(intent, Friend.OutgoingVideoStatus.FAILED_PERMANENTLY);
			}
		}
	}
	
	private void dispatchServiceException(AmazonServiceException e) {
		switch(e.getErrorType()){
		case Client:
			if (e.getStatusCode() / 100 == 3)
				Dispatch.dispatch(serviceExceptionMessage(e));
			else
				Log.e(TAG, serviceExceptionMessage(e));
			break;
		case Service:
			Log.e(TAG, serviceExceptionMessage(e));
			break;
		case Unknown:
			Dispatch.dispatch(serviceExceptionMessage(e));
			break;
		}
	}
	
	private void refreshCredentialsIfNecessary(AmazonServiceException e){
		if (e.getErrorType() == ErrorType.Client || e.getErrorType() == ErrorType.Unknown){
	        new CredentialsGetter(fileTransferService, new CredentialsGetter.CredentialsGetterCallback() {
				@Override
				public void success() {}
				@Override
				public void failure() {}
	        });
		}
	}
	
	private String serviceExceptionMessage(AmazonServiceException e){
		return "ERROR in transfer type: " + intent.getStringExtra(IntentFields.TRANSFER_TYPE_KEY) + 
			   " AmazonServiceException(" + e.getErrorType().getClass().getSimpleName() + ") " + 
			   e.getErrorMessage() + ": " + e.getErrorCode();
	}

	private boolean notRetryableServiceException(AmazonServiceException e){
		switch(e.getErrorType()){
		case Client: return true;
		case Service: return false;
		case Unknown: return true;
		default: return true;
		}
	}
}
