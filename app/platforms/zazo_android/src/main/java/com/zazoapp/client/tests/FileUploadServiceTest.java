package com.zazoapp.client.tests;

import android.content.Intent;
import com.zazoapp.client.model.OutgoingMessage;
import com.zazoapp.client.network.FileUploadService;

/**
 * Created by skamenkovych@codeminders.com on 3/17/2015.
 */
public class FileUploadServiceTest extends FileUploadService {

    private static UploadCallback listener;

    @Override
    public void reportStatus(Intent intent, int status) {
        if (listener == null)
            return;
        switch (status) {
            case OutgoingMessage.Status.FAILED_PERMANENTLY:
                listener.onFailed();
                break;
            case OutgoingMessage.Status.UPLOADED:
                listener.onUploaded();
                break;
        }
    }

    public static void setListener(UploadCallback callback) {
        listener = callback;
    }

    public interface UploadCallback {
        void onUploaded();
        void onFailed();
    }
}
