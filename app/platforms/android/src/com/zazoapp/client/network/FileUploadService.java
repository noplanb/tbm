package com.zazoapp.client.network;

import android.content.Intent;
import com.zazoapp.client.model.Friend;

public class FileUploadService extends FileTransferService {
    private static final String TAG = FileUploadService.class.getSimpleName();

    public FileUploadService() {
        super(TAG);
    }

    @Override
    protected boolean doTransfer(Intent intent) throws InterruptedException {
        intent.putExtra(IntentFields.TRANSFER_TYPE_KEY, IntentFields.TRANSFER_TYPE_UPLOAD);
        reportStatus(intent, Friend.OutgoingVideoStatus.UPLOADING);
        return fileTransferAgent.upload();
    }

    @Override
    protected void maxRetriesReached(Intent intent) throws InterruptedException {
        reportStatus(intent, Friend.OutgoingVideoStatus.FAILED_PERMANENTLY);
    }

}
