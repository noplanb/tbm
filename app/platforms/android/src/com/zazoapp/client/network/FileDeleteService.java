package com.zazoapp.client.network;

import android.content.Intent;
import com.zazoapp.client.model.Video;

public class FileDeleteService extends FileTransferService {
    private static final String TAG = FileDeleteService.class.getSimpleName();

    public FileDeleteService() {
        super(TAG);
    }

    @Override
    protected boolean doTransfer(Intent intent) throws InterruptedException {
        intent.putExtra(IntentFields.TRANSFER_TYPE_KEY, IntentFields.TRANSFER_TYPE_DELETE);
        // Do not retry deletes always return true.
        fileTransferAgent.delete();
        return true;
    }

    @Override
    protected void maxRetriesReached(Intent intent) throws InterruptedException {
        reportStatus(intent, Video.IncomingVideoStatus.FAILED_PERMANENTLY);
    }
}
