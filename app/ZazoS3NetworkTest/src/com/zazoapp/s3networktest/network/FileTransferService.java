package com.zazoapp.s3networktest.network;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import com.zazoapp.s3networktest.ManagerService;
import com.zazoapp.s3networktest.core.IntentHandlerService;
import com.zazoapp.s3networktest.network.aws.S3FileTransferAgent;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public abstract class FileTransferService extends IntentService {
    private static final String TAG = FileTransferService.class.getSimpleName();
    private final String OTAG = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + ": ";
    public static final String RESET_ACTION = "reset";
    private final static Integer MAX_RETRIES = 100;

    public static class Transfer {
        public static final int NEW = 0;
        public static final int IN_PROGRESS = 1;
        public static final int FINISHED = 2;
        public static final int FAILED = 3;
    }

    private static final Set<String> services = new HashSet<>();
    protected String id;

    protected IFileTransferAgent fileTransferAgent;

    // Interrupt support
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition reset = lock.newCondition();
    private final AtomicInteger retryCount = new AtomicInteger();

    public class IntentFields {
        public static final String ID_KEY = "id";
        public static final String TRANSFER_TYPE_KEY = "transferType";
        public static final String RETRY_COUNT_KEY = "retryCount";
        public static final String FILE_PATH_KEY = "filePath";
        public static final String FILE_NAME_KEY = "filename";
        public static final String PARAMS_KEY = "params";
        public static final String STATUS_KEY = "status";
        public static final String VIDEO_ID_KEY = "videoIdKey";

        public static final String TRANSFER_TYPE_UPLOAD = "upload";
        public static final String TRANSFER_TYPE_DOWNLOAD = "download";
        public static final String TRANSFER_TYPE_DELETE = "delete";
    }

    protected abstract void maxRetriesReached(Intent intent) throws InterruptedException;

    protected abstract boolean doTransfer(Intent intent) throws InterruptedException;

    public FileTransferService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fileTransferAgent = new S3FileTransferAgent(this);
        synchronized (TAG) {
            services.add(OTAG);
            Log.i(TAG, OTAG + "OnCreate");
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        if (RESET_ACTION.equals(intent.getAction())) {
            Log.i(TAG, OTAG + "Reset retries");
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
            Log.i(TAG, OTAG + "onHandleIntent");
            if (RESET_ACTION.equals(intent.getAction())) {
                return;
            }
            fileTransferAgent.setInstanceVariables(intent);
            retryCount.set(0);
            while (true) {
                if (!isConnected()) {
                    if (!reset.await(2, TimeUnit.SECONDS)) {
                        continue;
                    }
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
        Log.i(TAG, OTAG + "reportStatus: " + status);
        Intent newIntent = new Intent(intent);
        newIntent.setClass(getApplicationContext(), IntentHandlerService.class);
        newIntent.putExtra(IntentFields.STATUS_KEY, status);
        StringBuilder builder = new StringBuilder();
        synchronized (TAG) {
            for (String service : services) {
                builder.append(service).append("\n");
            }
            if (status == Transfer.FINISHED || status == Transfer.FAILED) {
                services.remove(OTAG);
            }
        }
        getApplicationContext().startService(newIntent);
        sendBroadcast(new Intent(ManagerService.ACTION_ON_INFO_UPDATED).putExtra("services", builder.toString()));
    }

    private void retrySleep(Intent intent) throws InterruptedException {
        intent.putExtra(IntentFields.RETRY_COUNT_KEY, retryCount.incrementAndGet());
        Log.i(TAG, OTAG + "retry: " + retryCount.get());

        long sleepTime = sleepTime(retryCount.get());

        Log.i(TAG, OTAG + "Sleeping for: " + sleepTime + "ms");
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        synchronized (TAG) {
            services.remove(OTAG);
        }
    }
}