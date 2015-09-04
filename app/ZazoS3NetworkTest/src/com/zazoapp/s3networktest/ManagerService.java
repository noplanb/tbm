package com.zazoapp.s3networktest;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import com.zazoapp.s3networktest.network.FileDeleteService;
import com.zazoapp.s3networktest.network.FileDownloadService;
import com.zazoapp.s3networktest.network.FileTransferService;
import com.zazoapp.s3networktest.network.FileUploadService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Serhii on 13.12.2014.
 */
public class ManagerService extends Service {
    private static final String CLASS_NAME = ManagerService.class.getCanonicalName();

    public static final String ACTION_START = CLASS_NAME + ".ACTION_START";
    public static final String ACTION_STOP = CLASS_NAME + ".ACTION_STOP";
    public static final String ACTION_RESET = CLASS_NAME + ".ACTION_RESET";
    public static final String ACTION_UPDATE_INFO = CLASS_NAME + ".ACTION_UPDATE_INFO";

    public static final String ACTION_ON_START = CLASS_NAME + ".ACTION_ON_START";
    public static final String ACTION_ON_STOP = CLASS_NAME + ".ACTION_ON_STOP";
    public static final String ACTION_ON_FINISHED = CLASS_NAME + ".ACTION_ON_FINISHED";


    public static final String EXTRA_FILES_LIST = "files_list";

    public static final String VIDEO_ID_KEY = "video_id";
    public static final String TRANSFER_TYPE_KEY = "transfer_type";
    public static final String STATUS_KEY = "status_key";
    public static final String RETRY_COUNT_KEY = "retry_count";

    private static final int THREADS_NUMBER = 1;
    private ScheduledExecutorService mExecutor;
    private boolean isStarted = false;

    private TestInfo info = new TestInfo();

    private static class TestInfo {
        long uploaded;
        long downloaded;
        long deleted;
        TransferTask currentTask;
        int currentStatus;
        int retryCount;
    }

    private enum TransferTask {
        WAITING,
        UPLOADING,
        DOWNLOADING,
        DELETING;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentText("Zazo S3 network test is running");
        builder.setSubText("Tap to manage");
        Intent intent = new Intent(this, MyActivity.class);
        intent.setAction(ACTION_STOP);
        builder.setContentIntent(PendingIntent.getActivity(this, 0, intent, 0));
        startForeground(1, builder.build());
        mExecutor = Executors.newScheduledThreadPool(THREADS_NUMBER);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_STICKY;
        }
        final String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            sendBroadcast(new Intent(ACTION_ON_START));
            doWork();
        } else if (ACTION_STOP.equals(action)) {
            stopSelf(startId);
            stopService(new Intent(getApplicationContext(), FileDownloadService.class));
            stopService(new Intent(getApplicationContext(), FileUploadService.class));
            stopService(new Intent(getApplicationContext(), FileDeleteService.class));
            return START_NOT_STICKY;
        } else if (ACTION_RESET.equals(action)) {
            FileTransferService.reset(this, FileDownloadService.class);
            FileTransferService.reset(this, FileUploadService.class);
        } else if (ACTION_UPDATE_INFO.equals(action)) {
            info.currentStatus = intent.getIntExtra(STATUS_KEY, -1);
            info.retryCount = intent.getIntExtra(RETRY_COUNT_KEY, -1);
            String type = intent.getStringExtra(TRANSFER_TYPE_KEY);
            if (FileTransferService.IntentFields.TRANSFER_TYPE_DOWNLOAD.equals(type)) {
                info.currentTask = TransferTask.DOWNLOADING;
            } else if (FileTransferService.IntentFields.TRANSFER_TYPE_UPLOAD.equals(type)) {
                info.currentTask = TransferTask.UPLOADING;
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        sendBroadcast(new Intent(ACTION_ON_STOP));
        mExecutor.shutdown();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void doWork() {
        if (!isStarted) {
            isStarted = true;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (checkStorageAvailability()) {
                        File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                        ArrayList<String> list = new ArrayList<String>();
                        listFolder(dcim, list);
                        sendBroadcast(new Intent(ACTION_ON_FINISHED).putStringArrayListExtra(EXTRA_FILES_LIST, list));
                    }
                    mExecutor.schedule(this, 10, TimeUnit.SECONDS);
                }
            };
            mExecutor.execute(runnable);
        }
    }

    private boolean checkStorageAvailability() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    private static void listFolder(File folder, List<String> list) {
        File[] fileList = folder.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (file.isDirectory()) {
                    if (!file.isHidden()) {
                        listFolder(file, list);
                    }
                } else {
                    list.add(file.getPath());
                }
            }
        }
    }
}
