package com.zazoapp.client.network;

import android.content.Intent;
import com.zazoapp.client.model.OutgoingVideo;

public class FileUploadService extends FileTransferService {
    private static final String TAG = FileUploadService.class.getSimpleName();

    public FileUploadService() {
        super(TAG);
    }

    @Override
    protected boolean doTransfer(Intent intent) throws InterruptedException {
        intent.putExtra(IntentFields.TRANSFER_TYPE_KEY, IntentFields.TRANSFER_TYPE_UPLOAD);
        reportStatus(intent, OutgoingVideo.Status.UPLOADING);
        return fileTransferAgent.upload();
    }

    @Override
    protected void maxRetriesReached(Intent intent) throws InterruptedException {
        reportStatus(intent, OutgoingVideo.Status.FAILED_PERMANENTLY);
    }

}
