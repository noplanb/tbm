package com.zazoapp.client.network.aws;

import android.content.Intent;
import android.os.Bundle;
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
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.model.IncomingMessage;
import com.zazoapp.client.model.OutgoingMessage;
import com.zazoapp.client.network.FileTransferService;
import com.zazoapp.client.network.FileTransferService.IntentFields;
import com.zazoapp.client.network.IFileTransferAgent;
import com.zazoapp.client.network.NetworkConfig;
import com.zazoapp.client.utilities.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class S3FileTransferAgent implements IFileTransferAgent {
	private static final String TAG = S3FileTransferAgent.class.getSimpleName();

    private String s3Bucket;

    private TransferManager tm;
	private Intent intent;
	private File file;
	private String filename;
	private FileTransferService fileTransferService;
	private boolean credentialsOk;

	public S3FileTransferAgent(FileTransferService fts) {
		fileTransferService = fts;
    }
	
	//------------------------
	// Instantiation and setup
	//------------------------
	@Override
	public void setInstanceVariables(Intent intent) throws InterruptedException {
		filename = intent.getStringExtra(IntentFields.FILE_NAME_KEY);
        String videoId = intent.getStringExtra(IntentFields.VIDEO_ID_KEY);
        Logger.i(TAG, "setInstanceVariables: " + filename + " id " + videoId);
		this.intent = intent;
		String filePath = intent.getStringExtra(IntentFields.FILE_PATH_KEY);
		this.file = new File(filePath);
		setupTransferManager();
	}
	
	private void setupTransferManager(){
        S3CredentialsStore s3CredStore = S3CredentialsStore.getInstance(fileTransferService);
        checkCredentialsOk();
        
        // Use them even if they are not ok. We will get an AmazonClientException which we will special case not retry.
        tm = new TransferManager(new BasicAWSCredentials(s3CredStore.getS3AccessKey(), s3CredStore.getS3SecretKey()));
        s3Bucket = s3CredStore.getS3Bucket();
        AmazonS3 client = tm.getAmazonS3Client();
        try {
            client.setRegion(Region.getRegion(Regions.valueOf(s3CredStore.getS3Region().toUpperCase().replace('-', '_'))));
        } catch (IllegalArgumentException e) {
            Dispatch.dispatch("S3FileTransferAgent: cant set region: " + e.toString());
        }
	}
	
	private void checkCredentialsOk(){
        if (!S3CredentialsStore.getInstance(fileTransferService).hasCredentials()){
        	Log.e(TAG, "Attempting an S3 file transfer but have no credentials. Getting them now by this transfer will fail.");
        	new S3CredentialsGetter(fileTransferService);
        	credentialsOk = false;
        	return;
        }
        credentialsOk = true;
	}

	//---------------------------
	// Upload download and delete
	//---------------------------
	@Override
	public boolean upload() throws InterruptedException {
		try {
            if (file.length() == 0) { // FIXME Temporary check for 0 size uploads
                Dispatch.dispatch(filename + " has 0 size");
            }
            PutObjectRequest _putObjectRequest = new PutObjectRequest(s3Bucket, filename, file);
            Bundle data1 = intent.getBundleExtra(IntentFields.METADATA); // FIXME TEST
            if (intent.hasExtra(IntentFields.METADATA)) {
                Bundle data = intent.getBundleExtra(IntentFields.METADATA);
                ObjectMetadata metadata = new ObjectMetadata();
                for (String s : data.keySet()) {
                    metadata.addUserMetadata(s, data.getString(s));
                }
                _putObjectRequest.setMetadata(metadata);
            }
            Logger.i(TAG, "upload() Before upload " + data1);
			Upload upload = tm.upload(_putObjectRequest);
			upload.waitForUploadResult();
            Logger.i(TAG, "upload() After upload " + data1);
		} catch (AmazonServiceException e) {
			handleServiceException(e);
			return notRetryableServiceException(e);
		} catch (AmazonClientException e) {
			handleClientException(e);
			return notRetryableClientException(e);
		}
        if (!DebugConfig.Bool.ALLOW_RESEND.get()) {
            file.delete(); // remove uploaded file
        }
		fileTransferService.reportStatus(intent, OutgoingMessage.Status.UPLOADED);
		return true;
	}

	@Override
	public boolean download() throws InterruptedException{
		Log.i(TAG, "Starting S3 download for filename: " + filename);
		try {
            GetObjectRequest _getObjectRequest = new GetObjectRequest(s3Bucket, filename);
            Logger.i(TAG, "download() Before download " + filename);
			Download download = tm.download(_getObjectRequest,	file);
			download.waitForCompletion();
            Logger.i(TAG, "download() After download " + filename);
		} catch (AmazonServiceException e) {
			handleServiceException(e);
			return notRetryableServiceException(e);
		} catch (AmazonClientException e) {
			handleClientException(e);
			return notRetryableClientException(e);
		} 
		fileTransferService.reportStatus(intent, IncomingMessage.Status.DOWNLOADED);
		return true;
	}
	

	@Override
	public boolean delete() throws InterruptedException{
		try {
            Logger.i(TAG, "delete() Before delete " + filename);
            DeleteObjectRequest _deleteObjectRequest = new DeleteObjectRequest(s3Bucket, filename);
			tm.getAmazonS3Client().deleteObject(_deleteObjectRequest);
            Logger.i(TAG, "delete() After delete " + filename);
		} catch (AmazonServiceException e) {
			handleServiceException(e);
			return notRetryableServiceException(e);
		} catch (AmazonClientException e) {
			handleClientException(e);
			return notRetryableClientException(e);
		}
		return true;
	}

    //------------
    // Convenience
    //------------
    private boolean isUpload() {
        return IntentFields.TRANSFER_TYPE_UPLOAD.equalsIgnoreCase(intent.getStringExtra(IntentFields.TRANSFER_TYPE_KEY));
    }

    private boolean isDownload() {
        return IntentFields.TRANSFER_TYPE_DOWNLOAD.equalsIgnoreCase(intent.getStringExtra(IntentFields.TRANSFER_TYPE_KEY));
    }

    private boolean isDelete() {
        return IntentFields.TRANSFER_TYPE_DELETE.equalsIgnoreCase(intent.getStringExtra(IntentFields.TRANSFER_TYPE_KEY));
    }

    //-------------------------
	// Client Exception helpers
	//-------------------------
	private void handleClientException(AmazonClientException e){
		logClientException(e);
		reportClientException(e);
	}

	private void logClientException(AmazonClientException e) {
        Logger.e(TAG, "ERROR in transfer type: " + intent.getStringExtra(IntentFields.TRANSFER_TYPE_KEY) + " AmazonClientException: " + e.toString());
        return;
	}

    private void reportClientException(AmazonClientException e) {
        if (notRetryableClientException(e)) {
            if (isDownload()) {
                fileTransferService.reportStatus(intent, IncomingMessage.Status.FAILED_PERMANENTLY);
            } else if (isUpload()) {
                fileTransferService.reportStatus(intent, OutgoingMessage.Status.FAILED_PERMANENTLY);
            }
            if (!isDelete()) {
                Dispatch.dispatch(
                        new AmazonClientException("Not Retryable Client Exception; Message=" + e.getMessage(), e),
                        "notRetryableClientException");
            }
        }
    }

    private boolean notRetryableClientException(AmazonClientException e){
		// The only client exception that is not retryable occurs if we have tried to access without credentials 
		// this returns a client error with message "Unable to calculate a request signature"
		return !credentialsOk;
	}
	
	//-------------------------
	// ServiceException helpers
	//-------------------------
	private void handleServiceException(AmazonServiceException e) {
		reportServiceException(e);
		dispatchServiceException(e);
		refreshCredentialsIfNecessary(e);
	}

    private void reportServiceException(AmazonServiceException e) {
        if (notRetryableServiceException(e)) {
            if (isDownload()) {
                fileTransferService.reportStatus(intent, IncomingMessage.Status.FAILED_PERMANENTLY);
            } else if (isUpload()) {
                fileTransferService.reportStatus(intent, OutgoingMessage.Status.FAILED_PERMANENTLY);
            }
            if (!isDelete()) {
                Log.e(TAG, e.toString());
                Dispatch.dispatch(
                        new AmazonServiceException("Not Retryable Service Exception; Code=" + e.getStatusCode()),
                        "notRetryableServiceException");
            }
        }
    }

    private void dispatchServiceException(AmazonServiceException e) {
        Logger.e(TAG, "dispatchServiceException " + e.getErrorType() + " " + e.getStatusCode(), e);
		switch(e.getErrorType()){
		case Client:
			if (e.getStatusCode() / 100 == 3)
				Dispatch.dispatch(serviceExceptionMessage(e));
			else
				Log.e(TAG, serviceExceptionMessage(e));
			break;
		case Service:
			//Log.e(TAG, serviceExceptionMessage(e));
            JSONObject connection = new JSONObject();
            try {
                connection.put("connection", NetworkConfig.getConnectionStatus(fileTransferService));
            } catch (JSONException e1) {
            }
            Dispatch.dispatch(e, null);
			break;
		case Unknown:
			Dispatch.dispatch(serviceExceptionMessage(e));
			break;
		}
	}
	
	private void refreshCredentialsIfNecessary(AmazonServiceException e){
		if (e.getErrorType() == ErrorType.Client || e.getErrorType() == ErrorType.Unknown)
	        new S3CredentialsGetter(fileTransferService);
	}
	
	private String serviceExceptionMessage(AmazonServiceException e){
		return "ERROR in transfer type: " + intent.getStringExtra(IntentFields.TRANSFER_TYPE_KEY) + 
			   " AmazonServiceException(" + e.getErrorType().getClass().getSimpleName() + ") " + 
			   e.getErrorMessage() + ": " + e.getErrorCode();
	}

    private boolean notRetryableServiceException(AmazonServiceException e) {
        switch (e.getErrorType()) {
            case Client:
                return true/*e.getStatusCode() != 403*/;
            case Service:
                return false;
            case Unknown:
                return true;
            default:
                return true;
        }
    }
}
