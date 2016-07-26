package com.zazoapp.client.network;

import android.content.Intent;
import com.zazoapp.client.model.IncomingMessage;

public class FileDownloadService extends FileTransferService {
    private static final String TAG = FileDownloadService.class.getSimpleName();

    public FileDownloadService() {
        super(TAG);
    }

    @Override
    protected boolean doTransfer(Intent intent) throws InterruptedException {
        intent.putExtra(IntentFields.TRANSFER_TYPE_KEY, IntentFields.TRANSFER_TYPE_DOWNLOAD);
        reportStatus(intent, IncomingMessage.Status.DOWNLOADING);
        return fileTransferAgent.download();
    }

    @Override
    protected void maxRetriesReached(Intent intent) throws InterruptedException {
        reportStatus(intent, IncomingMessage.Status.FAILED_PERMANENTLY);
    }
}
