package com.zazoapp.client.network;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import com.zazoapp.client.core.IntentHandlerService;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.network.aws.S3FileTransferAgent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public abstract class FileTransferService extends IntentService {
    private static final String TAG = FileTransferService.class.getSimpleName();
    public static final String RESET_ACTION = "reset";
    private final static Integer MAX_RETRIES = 100;

    protected String id;

    protected IFileTransferAgent fileTransferAgent;

    // Interrupt support
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition reset = lock.newCondition();
    private final AtomicInteger retryCount = new AtomicInteger();

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
    public void onStart(Intent intent, int startId) {
        if (RESET_ACTION.equals(intent.getAction())) {
            Log.i(TAG, "Reset retries");
            retryCount.set(0);
            if (lock.tryLock()) {
                try {
                    reset.signal();
                } finally {
                    lock.unlock();
                }
            }
        }
        super.onStart(intent, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        lock.lock();
        try {
            Log.i(TAG, "onHandleIntent");
            if (RESET_ACTION.equals(intent.getAction())) {
                return;
            }
            fileTransferAgent.setInstanceVariables(intent);
            retryCount.set(0);
            while (true) {
                if (!isConnected()) {
                    reset.await();
                    intent.putExtra(IntentFields.RETRY_COUNT_KEY, retryCount.get());
                }
                if (retryCount.get() > MAX_RETRIES) {
                    maxRetriesReached(intent);
                    break;
                }
                if (doTransfer(intent))
                    break;
                retrySleep(intent);
            }
        } catch (InterruptedException e) {
        } finally {
            lock.unlock();
        }
    }

    public void reportStatus(Intent intent, int status) {
        Log.i(TAG, "reportStatus");
        intent.setClass(getApplicationContext(), IntentHandlerService.class);
        intent.putExtra(IntentFields.STATUS_KEY, status);
        getApplicationContext().startService(intent);
    }

    private void retrySleep(Intent intent) throws InterruptedException {
        intent.putExtra(IntentFields.RETRY_COUNT_KEY, retryCount.incrementAndGet());
        Log.i(TAG, "retry: " + retryCount.get());

        long sleepTime = (DebugConfig.getInstance(this).isDebugEnabled()) ? 1000L : sleepTime(retryCount.get());

        Log.i(TAG, "Sleeping for: " + sleepTime + "ms");
        if (reset.await(sleepTime, TimeUnit.MILLISECONDS)) {
            intent.putExtra(IntentFields.RETRY_COUNT_KEY, retryCount.get());
        }
    }

    private long sleepTime(int retryCount) {
        if (retryCount <= 9) {
            return 1000 * (1 << retryCount);
        }
        return 512000;
    }

    public static void reset(Context context, Class<? extends FileTransferService> clazz) {
        Intent i = new Intent(context, clazz);
        i.setAction(FileUploadService.RESET_ACTION);
        context.startService(i);
    }

    private boolean isConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}