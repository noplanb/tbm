package com.zazoapp.client.network.aws;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonServiceException.ErrorType;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferType;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.model.IncomingVideo;
import com.zazoapp.client.model.OutgoingVideo;
import com.zazoapp.client.network.FileTransferService;
import com.zazoapp.client.network.FileTransferService.IntentFields;
import com.zazoapp.client.network.IFileTransferAgent;
import com.zazoapp.client.network.NetworkConfig;
import com.zazoapp.client.utilities.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class S3FileTransferAgent implements IFileTransferAgent {
	private static final String TAG = S3FileTransferAgent.class.getSimpleName();

    private String s3Bucket;

	private Intent intent;
	private File file;
	private String filename;
	private FileTransferService fileTransferService;
    private int transferId;
    private TransferObserver transferObserver;

    private static AmazonS3Client sS3Client;
    private static TransferUtility sTransferUtility;

    private static final int RESULT_FAILED = -1;
    private static final int RESULT_NEED_RESTART = -2;

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
        transferId = intent.getIntExtra(IntentFields.TRANSFER_ID, -1);
        setupTransferManager();
	}

    @Override
    public void pause() throws InterruptedException {
        if (sTransferUtility != null) {
            //if (transferId != -1) {
            //    if (sTransferUtility.getTransferById(transferId).getState() != TransferState.PAUSED) {
            //        Logger.i(TAG, "pause " + transferId + " " + filename);
            //        sTransferUtility.pause(transferId);
            //    }
            //}
            List<TransferObserver> transfersInProgress = sTransferUtility.getTransfersWithTypeAndState(TransferType.ANY, TransferState.IN_PROGRESS);
            for (TransferObserver transferInProgress : transfersInProgress) {
                Logger.i(TAG, "pause " + transferInProgress.getId() + " " + new File(transferInProgress.getAbsoluteFilePath()).getName());
                sTransferUtility.pause(transferInProgress.getId());
            }
        }
    }

    private void setupTransferManager(){
        if (!checkCredentialsOk()) {
            return;
        }
        S3CredentialsStore s3CredStore = S3CredentialsStore.getInstance(fileTransferService);
        if (sTransferUtility == null) {
            if (sS3Client == null) {
                sS3Client = new AmazonS3Client(new BasicAWSCredentials(s3CredStore.getS3AccessKey(), s3CredStore.getS3SecretKey()));
                try {
                    sS3Client.setRegion(Region.getRegion(Regions.valueOf(s3CredStore.getS3Region().toUpperCase().replace('-', '_'))));
                } catch (IllegalArgumentException e) {
                    Dispatch.dispatch("S3FileTransferAgent: cant set region: " + e.toString());
                }
            }
            sTransferUtility = new TransferUtility(sS3Client, fileTransferService);
        }
        s3Bucket = s3CredStore.getS3Bucket();
        if (transferObserver == null && transferId != -1) {
            transferObserver = sTransferUtility.getTransferById(transferId);
            if (transferObserver.getState() == TransferState.IN_PROGRESS) {
                Logger.i(TAG, "setupTransferManager pause " + transferId);
                sTransferUtility.pause(transferId);
            }
        }
    }

    private static TransferObserver getTransfer(String filepath) {
        if (sTransferUtility == null) {
            return null;
        }
        List<TransferObserver> transfers = sTransferUtility.getTransfersWithType(TransferType.ANY);
        for (TransferObserver transfer : transfers) {
            if (filepath.equals(transfer.getAbsoluteFilePath())) {
                return transfer;
            }
        }
        return null;
    }

    private boolean checkCredentialsOk() {
        if (!S3CredentialsStore.getInstance(fileTransferService).hasCredentials()) {
            Log.e(TAG, "Attempting an S3 file transfer but have no credentials. Getting them now by this transfer will fail.");
            new S3CredentialsGetter(fileTransferService);
            return false;
        }
        return true;
    }

	//---------------------------
	// Upload download and delete
	//---------------------------
	@Override
	public boolean upload() throws InterruptedException {
        setupTransferManager();
        if (sTransferUtility == null) {
            return false;
        }
        Logger.i(TAG, "Starting S3 upload for filename: " + filename);
        if (transferObserver != null) {
            transferObserver.refresh();
            switch (transferObserver.getState()) {
                case COMPLETED:
                    return true;
                case FAILED:
                    break;
                default:
                    final CountDownLatch latch = new CountDownLatch(1);
                    final AtomicInteger transferResult = new AtomicInteger(0);
                    transferObserver.setTransferListener(new ZazoTransferListener(latch, transferResult));
                    Logger.i(TAG, "Resume upload");
                    sTransferUtility.resume(transferObserver.getId());
                    return waitForUploadResult(latch, transferResult);
            }
        }
        if (file.length() == 0) { // FIXME Temporary check for 0 size uploads
            Dispatch.dispatch(filename + " has 0 size");
        }
        Bundle data1 = intent.getBundleExtra(IntentFields.METADATA); // FIXME TEST
        ObjectMetadata metadata = new ObjectMetadata();
        if (intent.hasExtra(IntentFields.METADATA)) {
            Bundle data = intent.getBundleExtra(IntentFields.METADATA);
            for (String s : data.keySet()) {
                metadata.addUserMetadata(s, data.getString(s));
            }
        }
        Logger.i(TAG, "upload() Before upload " + data1);
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger transferResult = new AtomicInteger(0);
        TransferObserver observer = sTransferUtility.upload(s3Bucket, filename, file, metadata);
        observer.setTransferListener(new ZazoTransferListener(latch, transferResult));
        intent.putExtra(IntentFields.TRANSFER_ID, observer.getId());
        fileTransferService.reportStatus(intent, OutgoingVideo.Status.UPLOADING);
		return waitForUploadResult(latch, transferResult);
	}

    /**
     *
     * @param latch
     * @param transferResult
     * @return true if no need to restart
     * @throws InterruptedException
     */
    private boolean waitForUploadResult(CountDownLatch latch, AtomicInteger transferResult) throws InterruptedException {
        latch.await();
        Logger.i(TAG, "upload() After upload " + transferResult.get());
        if (transferResult.get() == 0) {
            if (!DebugConfig.getInstance().isResendAllowed()) {
                file.delete(); // remove uploaded file
            }
            fileTransferService.reportStatus(intent, OutgoingVideo.Status.UPLOADED);
        }
        return transferResult.get() == 0 || transferResult.get() == RESULT_FAILED;
    }

	@Override
	public boolean download() throws InterruptedException{
        setupTransferManager();
        if (sTransferUtility == null) {
            return false;
        }
        Logger.i(TAG, "Starting S3 download for filename: " + filename);
        if (transferObserver != null) {
            transferObserver.refresh();
            switch (transferObserver.getState()) {
                case COMPLETED:
                    return true;
                case FAILED:
                    break;
                default:
                    final CountDownLatch latch = new CountDownLatch(1);
                    final AtomicInteger transferResult = new AtomicInteger(0);
                    transferObserver.setTransferListener(new ZazoTransferListener(latch, transferResult));
                    Logger.i(TAG, "Resume download");
                    sTransferUtility.resume(transferObserver.getId());
                    return waitForDownloadResult(latch, transferResult);
            }
        }
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger transferResult = new AtomicInteger(0);
        TransferObserver observer = sTransferUtility.download(s3Bucket, filename, file);
        intent.putExtra(IntentFields.TRANSFER_ID, observer.getId());
        fileTransferService.reportStatus(intent, IncomingVideo.Status.DOWNLOADING);
        observer.setTransferListener(new ZazoTransferListener(latch, transferResult));
        return waitForDownloadResult(latch, transferResult);
    }

    /**
     *
     * @param latch
     * @param transferResult
     * @return true if no need to restart
     * @throws InterruptedException
     */
    private boolean waitForDownloadResult(CountDownLatch latch, AtomicInteger transferResult) throws InterruptedException {
        latch.await();
        Logger.i(TAG, "download() After download " + transferResult.get() + " " + filename);
        if (transferResult.get() == 0) {
            fileTransferService.reportStatus(intent, IncomingVideo.Status.DOWNLOADED);
        }
        return transferResult.get() == 0 || transferResult.get() == RESULT_FAILED;
    }

    @Override
    public boolean delete() throws InterruptedException {
        setupTransferManager();
        if (sS3Client == null) {
            return false;
        }
		try {
            Logger.i(TAG, "delete() Before delete " + filename);
            DeleteObjectRequest _deleteObjectRequest = new DeleteObjectRequest(s3Bucket, filename);
            sS3Client.deleteObject(_deleteObjectRequest);
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
                fileTransferService.reportStatus(intent, IncomingVideo.Status.FAILED_PERMANENTLY);
            } else if (isUpload()) {
                fileTransferService.reportStatus(intent, OutgoingVideo.Status.FAILED_PERMANENTLY);
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
		return false;
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
                fileTransferService.reportStatus(intent, IncomingVideo.Status.FAILED_PERMANENTLY);
            } else if (isUpload()) {
                fileTransferService.reportStatus(intent, OutgoingVideo.Status.FAILED_PERMANENTLY);
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

    class ZazoTransferListener implements TransferListener {
        private CountDownLatch latch;
        private AtomicInteger result;

        ZazoTransferListener(CountDownLatch latch, AtomicInteger result) {
            this.latch = latch;
            this.result = result;
        }

        @Override
        public void onStateChanged(int id, TransferState state) {
            Logger.i(TAG, "onStateChanged id " + id + " " + state);
            switch (state) {
                case IN_PROGRESS:
                    if (!fileTransferService.isConnected()) {
                        result.set(RESULT_NEED_RESTART);
                        sTransferUtility.pause(id);
                        latch.countDown();
                    }
                    break;
                case WAITING_FOR_NETWORK:
                    result.set(RESULT_NEED_RESTART);
                    latch.countDown();
                    break;
                case COMPLETED:
                    latch.countDown();
                    break;
                case FAILED:
                    latch.countDown();
                    break;
            }
        }

        @Override
        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
        }

        @Override
        public void onError(int id, Exception e) {
            if (e instanceof AmazonServiceException) {
                handleServiceException((AmazonServiceException) e);
                result.set(notRetryableServiceException((AmazonServiceException) e) ? RESULT_FAILED : RESULT_NEED_RESTART);
            } else if (e instanceof AmazonClientException) {
                handleClientException((AmazonClientException) e);
                result.set(notRetryableClientException((AmazonClientException) e) ? RESULT_FAILED : RESULT_NEED_RESTART);
            } else {
                Dispatch.dispatch(e, null);
            }
        }
    }
}
