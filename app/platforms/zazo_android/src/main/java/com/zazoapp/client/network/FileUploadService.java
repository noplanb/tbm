package com.zazoapp.client.network;

import android.content.Intent;
import com.zazoapp.client.model.OutgoingMessage;

public class FileUploadService extends FileTransferService {
    private static final String TAG = FileUploadService.class.getSimpleName();

    public FileUploadService() {
        super(TAG);
    }

    protected FileUploadService(String tag) {
        super(tag);
    }

    @Override
    protected boolean doTransfer(Intent intent) throws InterruptedException {
        intent.putExtra(IntentFields.TRANSFER_TYPE_KEY, IntentFields.TRANSFER_TYPE_UPLOAD);
        reportStatus(intent, OutgoingMessage.Status.UPLOADING);
        return fileTransferAgent.upload();
    }

    @Override
    protected void maxRetriesReached(Intent intent) throws InterruptedException {
        reportStatus(intent, OutgoingMessage.Status.FAILED_PERMANENTLY);
    }

}
