package com.zazoapp.client.core;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.zazoapp.client.model.ActiveModelsHandler;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.Video;
import com.zazoapp.client.model.VideoFactory;
import com.zazoapp.client.ui.helpers.UnexpectedTerminationHelper;
import com.zazoapp.client.utilities.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class IntentHandlerService extends Service implements UnexpectedTerminationHelper.TerminationCallback {
    private static final String TAG = IntentHandlerService.class.getSimpleName();

    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;

    private ShutdownReceiver receiver;
    private Set<String> downloadingIds = new HashSet<>();
    private Set<String> uploadingIds = new HashSet<>();

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

        receiver = new ShutdownReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_SHUTDOWN);
        registerReceiver(receiver, filter);
        restoreTransferring();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.i(TAG, "onStart" + intent);

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
        Logger.d(TAG, "onDestroy");
        releaseResources();
        mServiceLooper.quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    protected void onHandleIntent(final Intent intent, int startId) {
        new IntentHandler(IntentHandlerService.this, intent).handle();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Logger.d(TAG, "onTaskRemoved");
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
        ActiveModelsHandler.getInstance(this).onTerminate();
        unregisterReceiver(receiver);
    }

    @Override
    public void onTerminate() {
        unregisterReceiver(receiver);
    }

    private void restoreTransferring() {
        ArrayList<Video> videos = VideoFactory.getFactoryInstance().all();
        FriendFactory friendFactory = FriendFactory.getFactoryInstance();
        for (Video video : videos) {
            switch (video.getIncomingVideoStatus()) {
                case Video.IncomingVideoStatus.NEW:
                    Friend friend = friendFactory.find(video.get(Video.Attributes.FRIEND_ID));
                    if (friend != null) {
                        friend.requestDownload(video.getId());
                    }
                    break;
            }
        }
    }

    public static void onApplicationStart() {
        ArrayList<Video> videos = VideoFactory.getFactoryInstance().all();
        for (Video video : videos) {
            switch (video.getIncomingVideoStatus()) {
                case Video.IncomingVideoStatus.QUEUED:
                case Video.IncomingVideoStatus.DOWNLOADING:
                    video.setIncomingVideoStatus(Video.IncomingVideoStatus.NEW);
                    video.setDownloadRetryCount(0);
                    break;
            }
        }
    }

    public boolean removeDownloadId(String id) {
        return downloadingIds.remove(id);
    }

    public boolean addDownloadId(String id) {
        return downloadingIds.add(id);
    }

    public boolean removeUploadId(String id) {
        return uploadingIds.remove(id);
    }

    public boolean addUploadId(String id) {
        return uploadingIds.add(id);
    }

}
