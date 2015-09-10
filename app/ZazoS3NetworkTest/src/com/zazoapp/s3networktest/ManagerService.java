package com.zazoapp.s3networktest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import com.zazoapp.s3networktest.core.PreferencesHelper;
import com.zazoapp.s3networktest.dispatch.Dispatch;
import com.zazoapp.s3networktest.dispatch.RollbarTracker;
import com.zazoapp.s3networktest.model.Friend;
import com.zazoapp.s3networktest.network.FileDeleteService;
import com.zazoapp.s3networktest.network.FileDownloadService;
import com.zazoapp.s3networktest.network.FileTransferService;
import com.zazoapp.s3networktest.network.FileUploadService;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Serhii on 13.12.2014.
 */
public class ManagerService extends Service {
    private static final String TAG = ManagerService.class.getSimpleName();
    private static final String CLASS_NAME = ManagerService.class.getCanonicalName();

    public static final String ACTION_START = CLASS_NAME + ".ACTION_START";
    public static final String ACTION_STOP = CLASS_NAME + ".ACTION_STOP";
    public static final String ACTION_RESET = CLASS_NAME + ".ACTION_RESET";
    public static final String ACTION_UPDATE_INFO = CLASS_NAME + ".ACTION_UPDATE_INFO";
    public static final String ACTION_DOWNLOAD = CLASS_NAME + ".ACTION_DOWNLOAD";
    public static final String ACTION_RESET_STATS = CLASS_NAME + ".ACTION_RESET_STATS";

    public static final String ACTION_ON_START = CLASS_NAME + ".ACTION_ON_START";
    public static final String ACTION_ON_STOP = CLASS_NAME + ".ACTION_ON_STOP";
    public static final String ACTION_ON_FINISHED = CLASS_NAME + ".ACTION_ON_FINISHED";


    public static final String ACTION_ON_INFO_UPDATED = CLASS_NAME + ".ACTION_ON_INFO_UPDATED";

    public static final String EXTRA_FILES_LIST = "files_list";
    public static final String VIDEO_ID_KEY = "video_id";
    public static final String TRANSFER_TYPE_KEY = "transfer_type";
    public static final String STATUS_KEY = "status_key";

    public static final String RETRY_COUNT_KEY = "retry_count";
    private static final int THREADS_NUMBER = 1;
    public static final String EXTRA_INFO = "info";
    public static String VIDEO_ID = "1011";
    private ScheduledExecutorService mExecutor;
    private boolean isStarted = false;

    private TestInfo info = new TestInfo();

    private Friend friend;

    public static boolean isStopped;

    private PreferencesHelper data;

    enum TransferTask {
        WAITING('\u231A'),
        UPLOADING('↑'),
        DOWNLOADING('↓'),
        DELETING('⊗');

        public char getChar() {
            return ch;
        }
        private char ch;
        TransferTask(char c) {
            ch = c;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        data = new PreferencesHelper(this);
        VIDEO_ID = data.getString("deviceVideoId", "_1_" + Build.SERIAL + "_" + System.currentTimeMillis());
        data.putString("deviceVideoId", VIDEO_ID);
        Dispatch.registerTracker(this, new RollbarTracker());
        if (isStopped) {
            stopSelf();
            return;
        }
        startForeground(1, getNotification());
        mExecutor = Executors.newScheduledThreadPool(THREADS_NUMBER);
        friend = Friend.getInstance(this);
        info.load(data);
    }

    private Notification getNotification() {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentText(info.toShortString() + ".\n Tap to manage");
        builder.setContentTitle("Zazo S3 network test");
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setSubText("");
        Intent intent = new Intent(this, MyActivity.class);
        intent.setAction(ACTION_STOP);
        builder.addAction(android.R.drawable.ic_delete, "Stop", PendingIntent.getActivity(this, 0, intent, 0));
        intent = new Intent(this, MyActivity.class);
        builder.setContentIntent(PendingIntent.getActivity(this, 0, intent, 0));
        return builder.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_STICKY;
        }
        final String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            isStopped = false;
            sendBroadcast(new Intent(ACTION_ON_START));
            doWork(getUploadTask(), true);
        } else if (ACTION_STOP.equals(action)) {
            stopSelf(startId);
            stopService(new Intent(getApplicationContext(), FileDownloadService.class));
            stopService(new Intent(getApplicationContext(), FileUploadService.class));
            stopService(new Intent(getApplicationContext(), FileDeleteService.class));
            isStopped = true;
            return START_NOT_STICKY;
        } else if (ACTION_RESET.equals(action)) {
            FileTransferService.reset(this, FileDownloadService.class);
            FileTransferService.reset(this, FileUploadService.class);
        } else if (ACTION_UPDATE_INFO.equals(action) && !isStopped) {
            if (intent.hasExtra(STATUS_KEY)) {
                info.currentStatus = intent.getIntExtra(STATUS_KEY, -1);
                info.retryCount = intent.getIntExtra(RETRY_COUNT_KEY, -1);
                String type = intent.getStringExtra(TRANSFER_TYPE_KEY);
                if (FileTransferService.IntentFields.TRANSFER_TYPE_DOWNLOAD.equals(type)) {
                    info.currentTask = TransferTask.DOWNLOADING;
                } else if (FileTransferService.IntentFields.TRANSFER_TYPE_UPLOAD.equals(type)) {
                    info.currentTask = TransferTask.UPLOADING;
                } else if (FileTransferService.IntentFields.TRANSFER_TYPE_DELETE.equals(type)) {
                    info.currentTask = TransferTask.DELETING;
                } else {
                    info.currentTask = TransferTask.WAITING;
                }
                switch (info.currentTask) {
                    case UPLOADING:
                        if (info.currentStatus == FileTransferService.Transfer.FINISHED) {
                            info.uploaded++;
                            doWork(getDownloadTask(), true);
                        } else if (info.currentStatus == FileTransferService.Transfer.FAILED) {
                            info.uploadedFailed++;
                            doWork(getUploadTask(), false);
                        }
                        break;
                    case DOWNLOADING:
                        if (info.currentStatus == FileTransferService.Transfer.FINISHED) {
                            info.downloaded++;
                            getDeleteTask().run();
                        } else if (info.currentStatus == FileTransferService.Transfer.FAILED) {
                            info.downloadedFailed++;
                            getDeleteTask().run();
                        }
                        break;
                    case DELETING:
                        if (info.currentStatus == FileTransferService.Transfer.FINISHED) {
                            info.deleted++;
                            doWork(getUploadTask(), true);
                        } else if (info.currentStatus == FileTransferService.Transfer.FAILED) {
                            doWork(getUploadTask(), false);
                        }
                        break;

                }
            }
            updateInfo();
        } else if (ACTION_DOWNLOAD.equals(action)) {
            doWork(getDownloadTask(), true);
        } else if (ACTION_RESET_STATS.equals(action)) {
            info.clear();
            info.save(new PreferencesHelper(this));
            sendBroadcast(new Intent(ACTION_ON_INFO_UPDATED).putExtra(EXTRA_INFO, info));
        }
        return START_STICKY;
    }

    private void updateInfo() {
        Log.i(TAG, "updateInfo: " + info.toShortString());
        sendBroadcast(new Intent(ACTION_ON_INFO_UPDATED).putExtra(EXTRA_INFO, info));
        info.save(data);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, getNotification());
    }

    @Override
    public void onDestroy() {
        sendBroadcast(new Intent(ACTION_ON_STOP));
        if (mExecutor != null)
            mExecutor.shutdown();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void doWork(Runnable task, boolean startImmediately) {
        if (!isStarted) {
            isStarted = true;
        }
        if (!isStopped && mExecutor != null) {
            if (startImmediately) {
                mExecutor.execute(task);
                //mExecutor.schedule(task, 500, TimeUnit.MILLISECONDS);
            } else {
                mExecutor.schedule(task, 5, TimeUnit.SECONDS);
            }
        }

    }

    private Runnable getUploadTask() {
        return new Runnable() {
            @Override
            public void run() {
                if (isStopped) return;
                Log.i(TAG, "UploadTask: " + info.toShortString());
                info.tries++;
                String videoId = VIDEO_ID;
                File ed = friend.videoToFile(videoId);
                File ing = Config.recordingFile(ManagerService.this);
                ing.renameTo(ed);
                friend.setNewOutgoingVideoId(videoId);
                friend.uploadVideo(videoId);
            }
        };
    }

    private Runnable getDownloadTask() {
        return new Runnable() {
            @Override
            public void run() {
                if (isStopped) return;
                Log.i(TAG, "DownloadTask: " + info.toShortString());
                String videoId = VIDEO_ID;
                friend.videoToFile(videoId).delete();
                friend.downloadVideo(videoId);
            }
        };
    }

    private Runnable getDeleteTask() {
        return new Runnable() {
            @Override
            public void run() {
                if (isStopped) return;
                Log.i(TAG, "DeleteTask: " + info.toShortString());
                String videoId = VIDEO_ID;
                friend.videoFromFile(videoId).delete();
            }
        };
    }
}
