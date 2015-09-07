package com.zazoapp.s3networktest.core;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.zazoapp.s3networktest.ManagerService;
import com.zazoapp.s3networktest.dispatch.Dispatch;
import com.zazoapp.s3networktest.model.Friend;
import com.zazoapp.s3networktest.network.FileDownloadService;
import com.zazoapp.s3networktest.network.FileTransferService;
import com.zazoapp.s3networktest.network.FileUploadService;

public class IntentHandlerService extends Service {

    private static final String TAG = IntentHandlerService.class.getSimpleName();

    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;

    private ShutdownReceiver shutdownReceiver;
    private TransferTasksHolder transferTasks;

    // The BroadcastReceiver that tracks network connectivity changes.
    private NetworkReceiver networkReceiver;
    private Friend friend;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent) msg.obj, msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        friend = Friend.getInstance(this);

        HandlerThread thread = new HandlerThread("IntentService[" + TAG + "]");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        shutdownReceiver = new ShutdownReceiver();
        IntentFilter shutDownIntentFilter = new IntentFilter(Intent.ACTION_SHUTDOWN);
        registerReceiver(shutdownReceiver, shutDownIntentFilter);

        // Register BroadcastReceiver to track connection changes.
        networkReceiver = new NetworkReceiver();
        IntentFilter networkIntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, networkIntentFilter);

        transferTasks = new TransferTasksHolder();
        restoreTransferring();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.i(TAG, "onStart");

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand flags=" + flags + " startId=" + startId);

        if (intent != null)
            onStart(intent, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        releaseResources();
        mServiceHandler.removeCallbacksAndMessages(null);
        mServiceLooper.quit();
        //onTerminate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    protected void onHandleIntent(final Intent intent, int startId) {
        new IntentHandler(intent).handle();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "onTaskRemoved");
        releaseResources();
    }

    private class ShutdownReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("ShutdownReceiver", "onReceive");
            releaseResources();
        }

    }

    private void releaseResources() {
    }

    private void restoreTransferring() {

    }

    public static void onApplicationStart() {
    }

    private class IntentHandler {

        private Intent intent;
        private String transferType;
        private String videoId;
        private int status;
        private int retryCount;

        public IntentHandler(Intent i) {
            // Convenience.printBundle(i.getExtras());
            intent = i;
            transferType = intent.getStringExtra(FileTransferService.IntentFields.TRANSFER_TYPE_KEY);
            videoId = intent.getStringExtra(FileTransferService.IntentFields.VIDEO_ID_KEY);
            status = intent.getIntExtra(FileTransferService.IntentFields.STATUS_KEY, -1);
            retryCount = intent.getIntExtra(FileTransferService.IntentFields.RETRY_COUNT_KEY, 0);
            if (i.getExtras() != null) {
                Log.i(TAG, i.getExtras().toString());
            }
        }

        public void handle() {
            Log.i(TAG, "handle:");
            // This should never happen except perhaps when debugging and notifications are coming in even though
            // the user is not registered.
            if (isDownloadIntent()) {
                handleDownloadIntent();
            } else if (isUploadIntent()) {
                handleUploadIntent();
            } else if (isStoringIntent()) {
                handleStoringIntent();
            }
        }

        // ------------
        // Convenience
        // ------------
        private boolean isUploadIntent() {
            return transferType != null && transferType.equals(FileTransferService.IntentFields.TRANSFER_TYPE_UPLOAD);
        }

        private boolean isDownloadIntent() {
            return transferType != null && transferType.equals(FileTransferService.IntentFields.TRANSFER_TYPE_DOWNLOAD);
        }

        private boolean isStoringIntent() {
            return IntentActions.SAVE_MODEL.equals(intent.getAction());
        }

        // ---------------------
        // Handle upload intent
        // ---------------------
        private void handleUploadIntent() {
            Log.i(TAG, "handleUploadIntent");
            updateStatus();
            if (status == FileTransferService.Transfer.FINISHED) {
                //// Set remote videoIdKV
                //RemoteStorageHandler.addRemoteOutgoingVideoId(friend, videoId);
                //
                //// Send outgoing notification
                //NotificationHandler.sendForVideoReceived(friend, videoId);
            }
        }

        // -------------------------
        // Handle Download Intent
        // -------------------------
        private void handleDownloadIntent() {
            Log.i(TAG, "handleDownloadIntent");

            // if (VideoIdUtils.isOlderThanOldestIncomingVideo(friend, videoId)){
            // Log.w(TAG,
            // "handleDownloadIntent: Ignoring download intent for video id that is older than the current incoming video.");
            // rSHandler.deleteRemoteVideoIdAndFile(friend, videoId);
            // return;
            // }

            // We may be getting a message from someone who is not a friend yet. Get
            // new friends and poll them all.

            // Create and download the video if this was a videoReceived intent.
            if (status == FileTransferService.Transfer.NEW) {

                if (!transferTasks.addDownloadId(videoId)) {
                    Log.w(TAG, "handleDownloadIntent: Ignoring download intent for video id that that is currently in process.");
                    return;
                }

                friend.downloadVideo(videoId);
            }

            if (status == FileTransferService.Transfer.FINISHED) {

                // Always delete the remote video even if the one we got is corrupted. Otherwise it may never be deleted
                deleteRemoteVideoAndKV();

                transferTasks.removeDownloadId(videoId);
            }

            if (status == FileTransferService.Transfer.FAILED) {
                Log.i(TAG, "deleteRemoteVideoAndKV for a video that failed permanently");
                deleteRemoteVideoAndKV();
                transferTasks.removeDownloadId(videoId);
            }

            if (status == FileTransferService.Transfer.IN_PROGRESS) {
                // No need to do anything special in this case.
            }

            // Update the status and notify based on the intent if we have not exited for another reason above.
            updateStatus();
        }

        private void handleStoringIntent() {
            //ActiveModelFactory<?> modelFactory = ActiveModelsHandler.getInstance(IntentHandlerService.this).getModelFromIntent(intent);
            //if (modelFactory != null) {
            //    // It is posted with delay to skip several requests in the row per one factory
            //    // Only last one will be handled
            //    mServiceHandler.removeCallbacks(modelFactory.getSaveTask(IntentHandlerService.this));
            //    mServiceHandler.postDelayed(modelFactory.getSaveTask(IntentHandlerService.this), 200);
            //}
        }

        //--------
        // Helpers
        //--------
        private void deleteRemoteVideoAndKV() {
            // Note it is ok if deleting the file fails as s3 will clean itself up after a few days.
            // Delete remote video.
            friend.deleteRemoteVideo(videoId);

        }

        public void updateStatus() {
            if (transferType.equals(FileTransferService.IntentFields.TRANSFER_TYPE_DOWNLOAD)) {
                friend.setAndNotifyIncomingVideoStatus(videoId, status);
            } else if (transferType.equals(FileTransferService.IntentFields.TRANSFER_TYPE_UPLOAD)) {
                friend.setAndNotifyOutgoingVideoStatus(videoId, status);
            } else {
                Dispatch.dispatch("ERROR: updateStatus: unknown TransferType passed in intent. This should never happen.");
                throw new RuntimeException();
            }
            Intent i = new Intent(IntentHandlerService.this, ManagerService.class);
            i.setAction(ManagerService.ACTION_UPDATE_INFO);
            i.putExtra(ManagerService.VIDEO_ID_KEY, videoId);
            i.putExtra(ManagerService.TRANSFER_TYPE_KEY, transferType);
            i.putExtra(ManagerService.STATUS_KEY, status);
            i.putExtra(ManagerService.RETRY_COUNT_KEY, retryCount);
            startService(i);
        }

    }

    public class IntentParamKeys {
        public static final String FRIEND_ID = "friendId";
        public static final String MODEL = "modelName";
    }

    public class IntentActions {
        public static final String NONE = "none";
        public static final String PLAY_VIDEO = "playVideo";
        public static final String SMS_RESULT = "smsResult";
        public static final String SAVE_MODEL = "saveModel";
    }

    public class NetworkReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connMgr =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

            if (networkInfo != null && networkInfo.isConnected()) {
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    Log.i(TAG, "Wi-Fi connected");
                } else {
                    Log.i(TAG, "Wi-Fi connection lost");
                }
                FileTransferService.reset(context, FileUploadService.class);
                FileTransferService.reset(context, FileDownloadService.class);
            } else {
                Log.i(TAG, "Connection lost");
            }
        }
    }
}
