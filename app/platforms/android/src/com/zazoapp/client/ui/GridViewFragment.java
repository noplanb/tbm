package com.zazoapp.client.ui;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.zazoapp.client.GridElementController;
import com.zazoapp.client.GridManager;
import com.zazoapp.client.IntentHandler;
import com.zazoapp.client.R;
import com.zazoapp.client.SyncManager;
import com.zazoapp.client.TbmApplication;
import com.zazoapp.client.bench.BenchViewManager;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.GridElement;
import com.zazoapp.client.model.GridElementFactory;
import com.zazoapp.client.model.VideoFactory;
import com.zazoapp.client.multimedia.AudioManager;
import com.zazoapp.client.multimedia.CameraException;
import com.zazoapp.client.multimedia.CameraManager;
import com.zazoapp.client.multimedia.CameraManager.CameraExceptionHandler;
import com.zazoapp.client.multimedia.VideoPlayer;
import com.zazoapp.client.network.FileDownloadService;
import com.zazoapp.client.network.FileUploadService;
import com.zazoapp.client.ui.dialogs.DoubleActionDialogFragment.DoubleActionDialogListener;
import com.zazoapp.client.ui.helpers.UnexpectedTerminationHelper;
import com.zazoapp.client.ui.helpers.VideoRecorderManager;
import com.zazoapp.client.ui.view.NineViewGroup;
import com.zazoapp.client.ui.view.NineViewGroup.LayoutCompleteListener;
import com.zazoapp.client.ui.view.VideoView;
import com.zazoapp.client.utilities.DialogShower;
import com.zazoapp.client.utilities.Logger;

import java.util.ArrayList;

// TODO: This file is still really ugly and needs to be made more organized and more readable. Some work may need to be factored out. -- Sani

public class GridViewFragment extends Fragment implements CameraExceptionHandler, DoubleActionDialogListener, UnexpectedTerminationHelper.TerminationCallback {

    private static final String TAG = GridViewFragment.class.getSimpleName();

    private ArrayList<GridElementController> viewControllers;

    private NineViewGroup nineViewGroup;
    private VideoPlayer videoPlayer;
    private VideoRecorderManager videoRecorderManager;
    private AudioManager audioManager;

    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private boolean viewLoaded;
    private boolean focused;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        TbmApplication.getInstance().addTerminationCallback(this);
        CameraManager.addExceptionHandlerDelegate(this);
        videoRecorderManager = new VideoRecorderManager(getActivity());

        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (proximitySensor == null) {
            Log.i(TAG, "Proximity sensor not found");
        }

        viewControllers = new ArrayList<>(GridManager.GRID_ELEMENTS_COUNT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        viewLoaded = false;
        View v = inflater.inflate(R.layout.nineviewgroup_fragment, container, false);

        setupVideoPlayer(v);
        setupNineViewGroup(v);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.i(TAG, "onResume");
        videoRecorderManager.onResume();
        setUpAudioManager();
        sensorManager.registerListener(audioManager, proximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void setUpAudioManager() {
        audioManager = new AudioManager(getActivity(), videoRecorderManager, videoPlayer);
        if (!audioManager.gainFocus()) {
            DialogShower.showToast(getActivity(), R.string.toast_could_not_get_audio_focus);
        }
        videoPlayer.setAudioManager(audioManager);
    }

    @Override
    public void onStart() {
        super.onStart();
        restartFileTransfersPendingRetry();
        new SyncManager(getActivity()).getAndPollAllFriends();
    }

    @Override
    public void onPause() {
        Logger.i(TAG, "onPause");
        super.onPause();
        releaseResources();
    }

    private void setupNineViewGroup(View v) {
        nineViewGroup = (NineViewGroup) v.findViewById(R.id.grid_view);
        nineViewGroup.setGestureListener(new NineViewGestureListener());
        nineViewGroup.setChildLayoutCompleteListener(new LayoutCompleteListener() {
            @Override
            public void onLayoutComplete() {
                setupGridElements();
                layoutVideoRecorder();
                viewLoaded = true;
                handleIntentAction(getActivity().getIntent());
            }
        });
    }

    private void setupVideoPlayer(View v) {
        videoPlayer = VideoPlayer.getInstance();
        ViewGroup videoBody = (ViewGroup) v.findViewById(R.id.video_body);
        VideoView videoView = (VideoView) v.findViewById(R.id.video_view);
        videoPlayer.init(getActivity(), videoBody, videoView);
    }

    private void setupGridElements(){
        if (!viewControllers.isEmpty()) {
            for (GridElementController controller : viewControllers) {
                controller.cleanUp();
            }
            viewControllers.clear();
        }
        BenchViewManager benchViewManager;
        try {
            benchViewManager = ((BenchViewManager.Provider) getActivity()).getBenchViewManager();
        } catch (ClassCastException e) {
            throw new RuntimeException("Activity must inherit BenchViewManagerProvider.");
        }
        int i = 0;
        for (GridElement ge : GridElementFactory.getFactoryInstance().all()) {
            GridElementController gec = new GridElementController(getActivity(), ge, nineViewGroup.getSurroundingFrame(i),
                    benchViewManager, videoRecorderManager);
            viewControllers.add(gec);
            i++;
        }
    }

    /**
     * Setup VideoRecorder layout. Adds recorder frame into centerFrame
     */
    private void layoutVideoRecorder() {
        FrameLayout fl = nineViewGroup.getCenterFrame();
        if (fl.getChildCount() != 0) {
            Log.w(TAG, "layoutVideoRecorder: not adding preview view as it appears to already have been added.");
            return;
        }
        Log.i(TAG, "layoutVideoRecorder: adding videoRecorder preview");
        videoRecorderManager.addRecorderTo(fl);
    }

    // -------------------------------
    // CameraExceptionHandler delegate
    // -------------------------------
    @Override
    public void onDialogActionClicked(int id, int button, Bundle bundle) {
        switch (button) {
            case DoubleActionDialogListener.BUTTON_POSITIVE:
                videoRecorderManager.reconnect();
                break;
            case DoubleActionDialogListener.BUTTON_NEGATIVE:
                getActivity().finish();
                break;
        }
    }

    @Override
    public void onCameraException(CameraException exception) {
        DialogShower.showCameraException(getActivity(), exception, this, exception.ordinal());
    }

    // -------------------------------
    // Hints
    // -------------------------------
    private void checkAndShowHint() {
        if (FriendFactory.getFactoryInstance().count() > 0) {                   // has at least one friend
            if (VideoFactory.getFactoryInstance().allNotViewedCount() > 0) {    // has at least one unviewed video
                DialogShower.showInfoDialog(getActivity(), getString(R.string.dialog_hint_title),
                        getString(R.string.dialog_play_hint_message));
            } else {
                DialogShower.showInfoDialog(getActivity(), getString(R.string.dialog_hint_title),
                        getString(R.string.dialog_record_hint_message));
            }
        }
    }

    public void play(String friendId) {
        Friend f = FriendFactory.getFactoryInstance().find(friendId);

        if (f == null)
            throw new RuntimeException("Play from notification found no friendId: " + friendId);

        int index = GridElementFactory.getFactoryInstance().gridElementIndexWithFriend(f);

        if (index == -1)
            throw new RuntimeException("Play from notification found no grid element index for friendId: " + friendId);

        View view = nineViewGroup.getSurroundingFrame(index);
        videoPlayer.togglePlayOverView(view, friendId);
    }

    // -------------------
    // HandleIntentAction
    // -------------------
    private void handleIntentAction(Intent currentIntent) {
        // Right now the only actions are
        // 1) to automatically start playing the appropriate video if the user
        // got here by clicking a notification.
        // 2) to notify the user if there was a problem sending Sms invite to a
        // friend. (Not used as decided this is unnecessarily disruptive)
        if (currentIntent == null) {
            Log.i(TAG, "handleIntentAction: no intent. Exiting.");
            return;
        }
        if (!viewLoaded || !focused) {
            Log.i(TAG, "View is not loaded yet or showed to user. Ignore for now.");
            return;
        }
        Log.i(TAG, "handleIntentAction: " + currentIntent.toString());

        String action = currentIntent.getAction();
        Uri data = currentIntent.getData();
        if (action == null || data == null) {
            Log.i(TAG, "handleIntentAction: no ation or data. Exiting.");
            return;
        }

        String friendId = currentIntent.getData().getQueryParameter(IntentHandler.IntentParamKeys.FRIEND_ID);
        if (action == null || friendId == null) {
            Log.i(TAG, "handleIntentAction: no friendId or action. Exiting." + currentIntent.toString());
            return;
        }

        if (action.equals(IntentHandler.IntentActions.PLAY_VIDEO)) {
            currentIntent.setAction(IntentHandler.IntentActions.NONE);
            play(friendId);
        }

        // Not used as I decided pending intent coming back from sending sms is
        // to disruptive. Just assume
        // sms's sent go through.
        if (action.equals(IntentHandler.IntentActions.SMS_RESULT)) {
            currentIntent.setAction(IntentHandler.IntentActions.NONE);
            Log.i(TAG, currentIntent.toString());
        }
    }

    @Override
    public void onTerminate() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                releaseResources();
            }
        });
    }

    private void releaseResources() {
        if (videoRecorderManager != null) {
            videoRecorderManager.onPause();
        }
        CameraManager.releaseCamera();
        if (videoPlayer != null) {
            videoPlayer.release();
        }
        if (sensorManager != null) {
            sensorManager.unregisterListener(audioManager);
        }
        if (audioManager != null) {
            audioManager.abandonFocus();
        }
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        focused = hasFocus;
        if (getActivity() != null) { // callback may come when fragment isn't attached to activity yet
            handleIntentAction(getActivity().getIntent());
        }
    }

    //------------------------------
    // nineViewGroup Gesture listner
    //------------------------------
    private class NineViewGestureListener implements NineViewGroup.GestureCallbacks {
        @Override
        public boolean onSurroundingClick(View view, int position) {
            return true;
        }

        @Override
        public boolean onSurroundingStartLongpress(View view, int position) {
            Log.d(TAG, "onSurroundingStartLongpress: " + position);
            GridElement ge = GridElementFactory.getFactoryInstance().get(position);
            String friendId = ge.getFriendId();
            if (friendId != null && !friendId.equals("")) {
                Logger.d("START RECORD");
                videoPlayer.stop();
                videoRecorderManager.onRecordStart(friendId);
            }
            return true;
        }

        @Override
        public boolean onEndLongpress() {
            Log.d(TAG, "onEndLongpress");
            videoRecorderManager.onRecordStop();
            return false;
        }

        @Override
        public boolean onCancelLongpress(int reason) {
            Log.d(TAG, "onCancelLongpress: " + getActivity().getString(reason));
            DialogShower.showToast(getActivity(), reason);
            videoRecorderManager.onRecordCancel();
            return false;
        }

        @Override
        public boolean onCenterClick(View view) {
            checkAndShowHint();
            return true;
        }

        @Override
        public boolean onCenterStartLongpress(View view) {
            checkAndShowHint();
            return true;
        }
    }
    
    //------------
    // FileTransfer
    //-------------
	private void restartFileTransfersPendingRetry() {
		FileDownloadService.restartTransfersPendingRetry(getActivity());
		FileUploadService.restartTransfersPendingRetry(getActivity());
	}
}
