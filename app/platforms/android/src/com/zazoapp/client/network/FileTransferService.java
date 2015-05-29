package com.zazoapp.client.network;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import com.zazoapp.client.DispatcherService;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.network.aws.S3FileTransferAgent;

public abstract class FileTransferService extends IntentService {
    private static final String TAG = FileTransferService.class.getSimpleName();

    private final static Integer MAX_RETRIES = 100;

    protected String id;

    protected IFileTransferAgent fileTransferAgent;

    public class IntentFields {
        public static final String ID_KEY = "id";
        public final static String TRANSFER_TYPE_KEY = "transferType";
        public final static String RETRY_COUNT_KEY = "retryCount";
        public final static String FILE_PATH_KEY = "filePath";
        public final static String FILE_NAME_KEY = "filename";
        public final static String PARAMS_KEY = "params";
        public final static String STATUS_KEY = "status";
        public final static String VIDEO_ID_KEY = "videoIdKey";

        public final static String TRANSFER_TYPE_UPLOAD = "upload";
        public final static String TRANSFER_TYPE_DOWNLOAD = "download";
        public final static String TRANSFER_TYPE_DELETE = "delete";
    }

    protected abstract void maxRetriesReached(Intent intent) throws InterruptedException;

    protected abstract boolean doTransfer(Intent intent) throws InterruptedException;

    public FileTransferService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (NetworkConfig.IS_AWS_USING) {
            fileTransferAgent = new S3FileTransferAgent(this);
        } else {
            fileTransferAgent = new ServerFileTransferAgent(this);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            Log.i(TAG, "onHandleIntent");
            fileTransferAgent.setInstanceVariables(intent);
        } catch (InterruptedException e) {
            Log.i(TAG, "Interrupt caught for Restart retry outside of loop.");
            intent.putExtra(FileTransferService.IntentFields.RETRY_COUNT_KEY, 0);
        }

        while (true) {
            try {
                if (intent.getIntExtra(IntentFields.RETRY_COUNT_KEY, 0) > MAX_RETRIES) {
                    Log.i(TAG, "onHandleIntent: MAX_RETRIES reached: " + MAX_RETRIES);
                    maxRetriesReached(intent);
                    break;
                }
                if (doTransfer(intent))
                    break;
                retrySleep(intent);
            } catch (InterruptedException e) {
                Log.i(TAG, "Interrupt caught for Restart retry inside loop.");
                intent.putExtra(FileTransferService.IntentFields.RETRY_COUNT_KEY, 0);
            }
        }
    }

    public void reportStatus(Intent intent, int status) {
        Log.i(TAG, "reportStatus");
        intent.setClass(getApplicationContext(), DispatcherService.class);
        intent.putExtra(IntentFields.STATUS_KEY, status);
        getApplicationContext().startService(intent);
    }

    private void retrySleep(Intent intent) throws InterruptedException {
        int retryCount = intent.getIntExtra(IntentFields.RETRY_COUNT_KEY, 0);
        retryCount++;
        Log.i(TAG, "retry: " + retryCount);
        intent.putExtra(IntentFields.RETRY_COUNT_KEY, retryCount);

        Long sleepTime;
        if (DebugConfig.getInstance(this).isDebugEnabled())
            sleepTime = (long) 1000;
        else
            sleepTime = sleepTime(retryCount);

        Log.i(TAG, "Sleeping for: " + sleepTime + "ms");
        Thread.sleep(sleepTime);
    }

    private long sleepTime(int retryCount) {
        if (retryCount <= 9) {
            return 1000 * (1 << retryCount);
        }
        return 512000;
    }

}