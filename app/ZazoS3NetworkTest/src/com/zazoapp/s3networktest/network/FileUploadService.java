package com.zazoapp.s3networktest.network;

import android.content.Intent;

public class FileUploadService extends FileTransferService {
    private static final String TAG = FileUploadService.class.getSimpleName();

    public FileUploadService() {
        super(TAG);
    }

    @Override
    protected boolean doTransfer(Intent intent) throws InterruptedException {
        intent.putExtra(IntentFields.TRANSFER_TYPE_KEY, IntentFields.TRANSFER_TYPE_UPLOAD);
        reportStatus(intent, Transfer.IN_PROGRESS);
        return fileTransferAgent.upload();
    }

    @Override
    protected void maxRetriesReached(Intent intent) throws InterruptedException {
        reportStatus(intent, Transfer.FAILED);
    }

}
