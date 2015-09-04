package com.zazoapp.s3networktest.network;

import android.content.Intent;

public class FileDownloadService extends FileTransferService {
    private static final String TAG = FileDownloadService.class.getSimpleName();

    public FileDownloadService() {
        super(TAG);
    }

    @Override
    protected boolean doTransfer(Intent intent) throws InterruptedException {
        intent.putExtra(IntentFields.TRANSFER_TYPE_KEY, IntentFields.TRANSFER_TYPE_DOWNLOAD);
        reportStatus(intent, Transfer.IN_PROGRESS);
        return fileTransferAgent.download();
    }

    @Override
    protected void maxRetriesReached(Intent intent) throws InterruptedException {
        reportStatus(intent, Transfer.FAILED);
    }
}
