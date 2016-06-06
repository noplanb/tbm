package com.zazoapp.client.core;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.model.ActiveModelFactory;
import com.zazoapp.client.model.ActiveModelsHandler;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.IncomingVideo;
import com.zazoapp.client.model.IncomingVideoFactory;
import com.zazoapp.client.model.OutgoingVideo;
import com.zazoapp.client.model.OutgoingVideoFactory;
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.Video;
import com.zazoapp.client.network.DeleteFriendRequest;
import com.zazoapp.client.network.FileDownloadService;
import com.zazoapp.client.network.FileTransferService;
import com.zazoapp.client.network.FileUploadService;
import com.zazoapp.client.network.HttpRequest;
import com.zazoapp.client.notification.NotificationAlertManager;
import com.zazoapp.client.notification.NotificationHandler;
import com.zazoapp.client.ui.helpers.UnexpectedTerminationHelper;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.Logger;

import java.util.ArrayList;

public class IntentHandlerService extends Service implements UnexpectedTerminationHelper.TerminationCallback {

    private static final String TAG = IntentHandlerService.class.getSimpleName();

    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;

    private static final TransferTasksHolder transferTasks = new TransferTasksHolder();

    // The BroadcastReceiver that tracks network connectivity changes.
    private NetworkReceiver networkReceiver;

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
        Logger.d(TAG, "onCreate");
        TbmApplication.getInstance().addTerminationCallback(this);

        HandlerThread thread = new HandlerThread("IntentService[" + TAG + "]");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        // Register BroadcastReceiver to track connection changes.
        networkReceiver = new NetworkReceiver();
        IntentFilter networkIntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, networkIntentFilter);
        restoreTransferring();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Logger.i(TAG, "onStart");

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = new Intent(intent);
        mServiceHandler.sendMessage(msg);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.i(TAG, "onStartCommand f=" + flags + " id=" + startId);
        if (intent != null) {
            onStart(intent, startId);
            Logger.i(TAG, "onStartCommand: " + intent + " " + intent.getExtras());
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.d(TAG, "onDestroy");
        releaseResources();
        mServiceHandler.removeCallbacksAndMessages(null);
        mServiceLooper.quit();
        onTerminate();
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
        Logger.d(TAG, "onTaskRemoved");
        releaseResources();
    }

    public static class ShutdownReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.d("ShutdownReceiver", "onReceive");
            if (intent.getAction().equals(Intent.ACTION_SHUTDOWN)) {
                ActiveModelsHandler.getInstance(context).onTerminate();
            }
        }

    }

    private void releaseResources() {
        ActiveModelsHandler.getInstance(this).onTerminate();
    }

    @Override
    public void onTerminate() {
        unregisterReceiver(networkReceiver);
    }

    private void restoreTransferring() {
        Logger.i(TAG, "restoreTransferring");
        ArrayList<IncomingVideo> incomingVideos = IncomingVideoFactory.getFactoryInstance().all();
        FriendFactory friendFactory = FriendFactory.getFactoryInstance();
        for (IncomingVideo video : incomingVideos) {
            switch (video.getVideoStatus()) {
                case IncomingVideo.Status.NEW:
                    Friend friend = friendFactory.find(video.get(Video.Attributes.FRIEND_ID));
                    if (friend != null) {
                        Logger.i(TAG, "Inew: restoreTransferring " + friend.getMkey() + " " + video.getId());
                        friend.requestDownload(video.getId());
                    }
                    break;
                case IncomingVideo.Status.VIEWED:
                case IncomingVideo.Status.FAILED_PERMANENTLY:
                case IncomingVideo.Status.MARKED_FOR_DELETION:
                    video.handleRemoteDeletion();
                    break;
            }
        }
        ArrayList<OutgoingVideo> outgoingVideos = OutgoingVideoFactory.getFactoryInstance().all();
        for (OutgoingVideo video : outgoingVideos) {
            switch (video.getVideoStatus()) {
                case OutgoingVideo.Status.NONE:
                case OutgoingVideo.Status.NEW:
                    Friend friend = friendFactory.find(video.get(Video.Attributes.FRIEND_ID));
                    if (friend != null) {
                        String videoId = video.getId();
                        Logger.i(TAG, "Onew: restoreTransferring " + friend.getMkey() + " " + video.getId());
                        if (friend.videoToFile(videoId).exists()) {
                            friend.requestUpload(videoId);
                        } else {
                            friend.setAndNotifyOutgoingVideoStatus(videoId, OutgoingVideo.Status.UPLOADED);
                        }
                    }
                    break;
            }
        }
    }

    public static void onApplicationStart() {
        Logger.i(TAG, "onApplicationStart");
        transferTasks.clearAll();
        ArrayList<IncomingVideo> incomingVideos = IncomingVideoFactory.getFactoryInstance().all();
        for (IncomingVideo video : incomingVideos) {
            switch (video.getVideoStatus()) {
                case IncomingVideo.Status.QUEUED:
                case IncomingVideo.Status.DOWNLOADING:
                    video.setVideoStatus(IncomingVideo.Status.NEW);
                    video.setRetryCount(0);
                    break;
            }
        }
        ArrayList<OutgoingVideo> outgoingVideos = OutgoingVideoFactory.getFactoryInstance().all();
        for (OutgoingVideo video : outgoingVideos) {
            switch (video.getVideoStatus()) {
                case OutgoingVideo.Status.QUEUED:
                case OutgoingVideo.Status.UPLOADING:
                    video.setVideoStatus(OutgoingVideo.Status.NEW);
                    video.setRetryCount(0);
                    break;
            }
        }
    }

    private class IntentHandler {

        private Intent intent;
        private Friend friend;
        private String transferType;
        private String videoId;
        private int status;
        private int retryCount;

        public IntentHandler(Intent i) {
            // Convenience.printBundle(i.getExtras());
            intent = i;
            friend = FriendFactory.getFactoryInstance().getFriendFromIntent(intent);
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
            if (isStoringIntent()) {
                handleStoringIntent();
            } else if (!User.isRegistered(getApplicationContext())) {
            // This should never happen except perhaps when debugging and notifications are coming in even though
            // the user is not registered.
                Dispatch.dispatch(intent, "IntentHandler: No user", "Got an intent but user was not registered. Not processing it.");
            } else if (isDownloadIntent()) {
                handleDownloadIntent();
            } else if (isUploadIntent()) {
                handleUploadIntent();
            } else if (isFriendJoinedIntent()) {
                handleFriendJoinedIntent();
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

        private boolean isFriendJoinedIntent() {
            return IntentActions.FRIEND_JOINED.equals(intent.getAction());
        }

        // ---------------------
        // Handle upload intent
        // ---------------------
        private void handleUploadIntent() {
            Logger.i(TAG, "handleUploadIntent " + status);
            if (friend == null) {
                StringBuilder msg = new StringBuilder("friend is null in IntentHandler\n");
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    msg.append(extras.toString()).append("\n");
                }
                for (Friend friend : FriendFactory.getFactoryInstance().all()) {
                    msg.append(friend.toString()).append("\n");
                }
                Dispatch.dispatch(msg.toString());
                return;
            }
            if (friend.isDeleted()) {
                DeleteFriendRequest.makeRequest(friend, false, new HttpRequest.Callbacks() {
                    @Override
                    public void success(String response) {
                        friend.setDeleted(false);
                        handleUploadIntent();
                    }

                    @Override
                    public void error(String errorString) {
                    }
                });
                return;
            }
            if (status == OutgoingVideo.Status.NEW) {
                if (!transferTasks.addUploadId(videoId)) {
                    Logger.w(TAG, "handleUploadIntent: Ignoring upload intent for video id that is currently in process.");
                    return;
                }
                friend.uploadVideo(videoId);
            }
            updateStatus();
            if (status == OutgoingVideo.Status.UPLOADED) {
                //// Set remote videoIdKV
                //RemoteStorageHandler.addRemoteOutgoingVideoId(friend, videoId);
                //
                //// Send outgoing notification
                //NotificationHandler.sendForVideoReceived(friend, videoId);
            }
            if (status == OutgoingVideo.Status.UPLOADED || status == OutgoingVideo.Status.FAILED_PERMANENTLY) {
                transferTasks.removeUploadId(videoId);
            }
        }

        // -------------------------
        // Handle Download Intent
        // -------------------------
        private void handleDownloadIntent() {
            Logger.i(TAG, "handleDownloadIntent " + status);

            // if (VideoIdUtils.isOlderThanOldestIncomingVideo(friend, videoId)){
            // Log.w(TAG,
            // "handleDownloadIntent: Ignoring download intent for video id that is older than the current incoming video.");
            // rSHandler.deleteRemoteVideoIdAndFile(friend, videoId);
            // return;
            // }

            // We may be getting a message from someone who is not a friend yet. Get
            // new friends and poll them all.
            if (friend == null) {
                Log.i(TAG, "Got Video from a user who is not currently a friend. Getting friends.");
                SyncManager.getAndPollAllFriends(getApplicationContext(), null);
                return;
            }
            if (friend.isDeleted()) {
                DeleteFriendRequest.makeRequest(friend, false, new HttpRequest.Callbacks() {
                    @Override
                    public void success(String response) {
                        friend.setDeleted(false);
                        handleDownloadIntent();
                    }

                    @Override
                    public void error(String errorString) {
                    }
                });
                return;
            }

            friend.setLastActionTime();
            friend.setHasApp();

            // Create and download the video if this was a videoReceived intent.
            if (status == IncomingVideo.Status.NEW) {
                IncomingVideoFactory incomingVideos = IncomingVideoFactory.getFactoryInstance();
                IncomingVideo video = incomingVideos.find(videoId);
                if (video != null && video.getVideoStatus() != IncomingVideo.Status.NEW || !transferTasks.addDownloadId(videoId)) {
                    Log.w(TAG, "handleDownloadIntent: Ignoring download intent for video id that is currently in process.");
                    return;
                }
                if (Convenience.checkAndNotifyNoSpace(IntentHandlerService.this)) {
                    friend.createIncomingVideo(getApplicationContext(), videoId);
                    friend.downloadVideo(videoId);
                } else {
                    transferTasks.removeDownloadId(videoId);
                }
            }

            if (status == IncomingVideo.Status.DOWNLOADED) {

                // Always delete the remote video even if the one we got is corrupted. Otherwise it may never be deleted
                deleteRemoteVideoAndKV();

                // Always set status for sender to downloaded and send status notification even if the video we got is not corrupted.
                RemoteStorageHandler.setRemoteIncomingVideoStatus(friend, videoId, RemoteStorageHandler.StatusEnum.DOWNLOADED);
                NotificationHandler.sendForVideoStatusUpdate(friend, videoId, NotificationHandler.StatusEnum.DOWNLOADED);

                friend.createThumb(videoId);

                friend.deleteAllViewedVideos();

                // if application is in foreground, alert is decided by GridElementController and connected to animation start
                if (!TbmApplication.getInstance().isForeground() || Convenience.screenIsLockedOrOff(getApplicationContext())) {
                    NotificationAlertManager.alert(getApplicationContext(), friend, videoId);
                }
                transferTasks.removeDownloadId(videoId);
            }

            if (status == IncomingVideo.Status.FAILED_PERMANENTLY) {
                Logger.i(TAG, "deleteRemoteVideoAndKV for a video that failed permanently");
                deleteRemoteVideoAndKV();
                transferTasks.removeDownloadId(videoId);
                IncomingVideo video = IncomingVideoFactory.getFactoryInstance().find(videoId);
                if (video != null && video.isDownloaded()) {
                    // Do not update status as this video has been already downloaded
                    return;
                }
            }

            if (status == IncomingVideo.Status.DOWNLOADING) {
                // No need to do anything special in this case.
            }

            // Update the status and notify based on the intent if we have not exited for another reason above.
            updateStatus();
            if (status == IncomingVideo.Status.DOWNLOADED) {
                // Save Incoming video as soon as it is downloaded
                ActiveModelsHandler.getInstance(IntentHandlerService.this).save(IncomingVideoFactory.getFactoryInstance());
            }
        }

        private void handleStoringIntent() {
            ActiveModelFactory<?> modelFactory = ActiveModelsHandler.getInstance(IntentHandlerService.this).getModelFromIntent(intent);
            if (modelFactory != null) {
                // It is posted with delay to skip several requests in the row per one factory
                // Only last one will be handled
                mServiceHandler.removeCallbacks(modelFactory.getSaveTask(IntentHandlerService.this));
                mServiceHandler.postDelayed(modelFactory.getSaveTask(IntentHandlerService.this), 200);
            }
        }

        private void handleFriendJoinedIntent() {
            final String friendJoinedAction = intent.getStringExtra(FriendJoinedIntentFields.ACTION);
            if (friendJoinedAction == null) {
                return;
            }
            switch (friendJoinedAction) {
                case FriendJoinedActions.NOTIFY:
                    NotificationAlertManager.alertFriendJoined(IntentHandlerService.this, new Intent(intent));
                    break;
            }
        }

        //--------
        // Helpers
        //--------
        private void deleteRemoteVideoAndKV() {
            IncomingVideo video = IncomingVideoFactory.getFactoryInstance().find(videoId);
            if (video != null) {
                video.deleteFromRemote();
            }
        }

        public void updateStatus() {
            if (transferType.equals(FileTransferService.IntentFields.TRANSFER_TYPE_DOWNLOAD)) {
                friend.setAndNotifyIncomingVideoStatus(videoId, status);
                friend.setAndNotifyDownloadRetryCount(videoId, retryCount);
            } else if (transferType.equals(FileTransferService.IntentFields.TRANSFER_TYPE_UPLOAD)) {
                friend.setAndNotifyOutgoingVideoStatus(videoId, status);
                friend.setAndNotifyUploadRetryCount(videoId, retryCount);
            } else {
                Dispatch.dispatch("ERROR: updateStatus: unknown TransferType passed in intent. This should never happen.");
                throw new RuntimeException();
            }
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
        public static final String FRIEND_JOINED = "friend_joined";
        public static final String SUGGESTIONS = "suggestions";
        public static final String SEND_VIDEO = "send_video";
        public static final String SHOW_AWARD = "show_award";
    }

    public static class FriendJoinedIntentFields {
        public static final String DATA = "data";
        public static final String ACTION = "action";
        public static final String SUBACTION = "subaction";
        public static final String CHOSEN_PHONE = "chosen_phone";
    }

    public static class FriendJoinedActions {
        public static final String NOTIFY = "notify";
        public static final String ADD = "add";
        public static final String IGNORE = "ignore";
        public static final String UNSUBSCRIBE = "unsubscribe";
        public static final String SUBSCRIBE = "subscribe";
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
                Logger.i(TAG, networkInfo.toString());
                if (!networkInfo.isRoaming() || Settings.Bool.ALLOW_DATA_IN_ROAMING.isSet()) {
                    FileTransferService.reset(context, FileUploadService.class);
                    FileTransferService.reset(context, FileDownloadService.class);
                }
            } else {
                Logger.i(TAG, "Connection lost");
            }
        }
    }
}
