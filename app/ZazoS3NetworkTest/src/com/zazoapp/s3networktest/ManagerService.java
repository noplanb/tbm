package com.zazoapp.s3networktest;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
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
    public static final String VIDEO_ID = "1011";
    private ScheduledExecutorService mExecutor;
    private boolean isStarted = false;

    private TestInfo info = new TestInfo();

    private Friend friend;

    public static boolean isStopped;

    private PreferencesHelper data;

    public static class TestInfo implements Parcelable {
        long tries;
        long uploaded;
        long downloaded;
        long deleted;
        TransferTask currentTask;
        int currentStatus;
        int retryCount;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(tries);
            dest.writeLong(uploaded);
            dest.writeLong(downloaded);
            dest.writeLong(deleted);
            dest.writeInt(currentTask.ordinal());
            dest.writeInt(currentStatus);
            dest.writeInt(retryCount);
        }

        public static final Parcelable.Creator<TestInfo> CREATOR
                = new Parcelable.Creator<TestInfo>() {
            public TestInfo createFromParcel(Parcel in) {
                return new TestInfo(in);
            }

            public TestInfo[] newArray(int size) {
                return new TestInfo[size];
            }
        };

        private TestInfo(Parcel in) {
            tries = in.readLong();
            uploaded = in.readLong();
            downloaded = in.readLong();
            deleted = in.readLong();
            currentTask = TransferTask.values()[in.readInt()];
            currentStatus = in.readInt();
            retryCount = in.readInt();
        }

        private TestInfo() {
            currentTask = TransferTask.WAITING;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Tries: ").append(tries).append("\n");
            builder.append("\u2191").append(uploaded).append(" \u2193").append(downloaded).append(" d ").append(deleted);
            builder.append("\ncurrent: \t").append(currentTask).append("\nstatus: \t");
            switch (currentStatus) {
                case FileTransferService.Transfer.NEW:
                    builder.append("new");
                    break;
                case FileTransferService.Transfer.IN_PROGRESS:
                    builder.append("progress");
                    break;
                case FileTransferService.Transfer.FAILED:
                    builder.append("failed");
                    break;
                case FileTransferService.Transfer.FINISHED:
                    builder.append("finished");
                    break;
            }
            builder.append("\nretry: \t").append(retryCount);
            return builder.toString();
        }

        public void save(PreferencesHelper data) {
            if (data != null) {
                data.putString("tries", String.valueOf(tries));
                data.putString("uploaded", String.valueOf(uploaded));
                data.putString("downloaded", String.valueOf(downloaded));
                data.putString("deleted", String.valueOf(deleted));
            }
        }

        public void load(PreferencesHelper data) {
            if (data != null) {
                tries = Long.parseLong(data.getString("tries", "0"));
                uploaded = Long.parseLong(data.getString("uploaded", "0"));
                downloaded = Long.parseLong(data.getString("downloaded", "0"));
                deleted = Long.parseLong(data.getString("deleted", "0"));
            }
        }
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
        Dispatch.registerTracker(this, new RollbarTracker());
        if (isStopped) {
            stopSelf();
            return;
        }
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentText("Running. Tap to manage");
        builder.setContentTitle("Zazo S3 network test");
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setSubText("");
        Intent intent = new Intent(this, MyActivity.class);
        intent.setAction(ACTION_STOP);
        builder.addAction(android.R.drawable.ic_delete, "Cancel", PendingIntent.getActivity(this, 0, intent, 0));
        intent = new Intent(this, MyActivity.class);
        builder.setContentIntent(PendingIntent.getActivity(this, 0, intent, 0));
        startForeground(1, builder.build());
        mExecutor = Executors.newScheduledThreadPool(THREADS_NUMBER);
        friend = Friend.getInstance(this);
        data = new PreferencesHelper(this);
        info.load(data);
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
                            doWork(getUploadTask(), false);
                        }
                        break;
                    case DOWNLOADING:
                        if (info.currentStatus == FileTransferService.Transfer.FINISHED) {
                            info.downloaded++;
                            doWork(getDeleteTask(), true);
                        } else if (info.currentStatus == FileTransferService.Transfer.FAILED) {
                            doWork(getDeleteTask(), true);
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
        }
        return START_STICKY;
    }

    private void updateInfo() {
        sendBroadcast(new Intent(ACTION_ON_INFO_UPDATED).putExtra(EXTRA_INFO, info));
        info.save(data);
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
        if (!isStopped) {
            if (startImmediately) {
                mExecutor.schedule(task, 2, TimeUnit.SECONDS);
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
                String videoId = VIDEO_ID;
                friend.downloadVideo(videoId);
            }
        };
    }

    private Runnable getDeleteTask() {
        return new Runnable() {
            @Override
            public void run() {
                if (isStopped) return;
                String videoId = VIDEO_ID;
                friend.videoToFile(videoId).delete();
                friend.videoFromFile(videoId).delete();
            }
        };
    }
}
