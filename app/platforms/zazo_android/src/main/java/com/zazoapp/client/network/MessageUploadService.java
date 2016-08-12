package com.zazoapp.client.network;

import android.content.Intent;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.model.OutgoingMessage;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by skamenkovych@codeminders.com on 8/12/2016.
 */
public class MessageUploadService extends FileUploadService {
    private static final String TAG = MessageUploadService.class.getSimpleName();

    public MessageUploadService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fileTransferAgent = new MessageTransferAgent(this);
    }

    private static class MessageTransferAgent implements IFileTransferAgent {
        private Intent intent;
        private String path;
        private FileTransferService fileTransferService;
        private String messageId;
        private String receiver;
        private String messageType;
        private String body;

        MessageTransferAgent(FileTransferService service) {
            fileTransferService = service;
        }

        @Override
        public boolean upload() throws InterruptedException {
            if (body == null) {
                body = Convenience.getTextFromFile(path);
                if (body == null) {
                    fileTransferService.reportStatus(intent, OutgoingMessage.Status.FAILED_PERMANENTLY);
                    return false;
                }
            }
            JSONObject object = new JSONObject();
            try {
                object.put("id", messageId);
                object.put("receiver_mkey", receiver);
                object.put("type", messageType);
                object.put("body", body);
            } catch (JSONException e) {
                return true;
            }
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicBoolean result = new AtomicBoolean(false);
            new HttpRequest.Builder()
                    .setUri("/api/v1/messages")
                    .setMethod(HttpRequest.POST)
                    .setJsonParams(object)
                    .setCallbacks(new HttpRequest.Callbacks() {
                        @Override
                        public void success(String response) {
                            result.set(true);
                            latch.countDown();
                        }

                        @Override
                        public void error(String errorString) {
                            latch.countDown();
                        }
                    }).build();
            if (latch.await(30, TimeUnit.SECONDS)) {
                if (!DebugConfig.Bool.ALLOW_RESEND.get()) {
                    new File(path).delete(); // remove uploaded file
                }
                fileTransferService.reportStatus(intent, OutgoingMessage.Status.UPLOADED);
                return true;
            }
            return false;
        }

        @Override
        public boolean download() throws InterruptedException {
            return false;
        }

        @Override
        public boolean delete() throws InterruptedException {
            return false;
        }

        @Override
        public void setInstanceVariables(Intent intent) throws InterruptedException {
            messageId = intent.getStringExtra(IntentFields.MESSAGE_ID_KEY);
            Logger.i(TAG, "setInstanceVariables: " + " id " + messageId);
            this.path = intent.getStringExtra(IntentFields.FILE_PATH_KEY);
            this.messageType = intent.getStringExtra(IntentFields.MESSAGE_TYPE);
            this.receiver = intent.getStringExtra(IntentFields.RECEIVER_MKEY);
            this.intent = intent;
            this.body = null;
        }
    }
}
