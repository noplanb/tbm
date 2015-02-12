package com.noplanbees.tbm.ui;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.VideoView;
import com.noplanbees.tbm.GridElementController;
import com.noplanbees.tbm.GridManager;
import com.noplanbees.tbm.IntentHandler;
import com.noplanbees.tbm.R;
import com.noplanbees.tbm.SyncManager;
import com.noplanbees.tbm.bench.BenchViewManager;
import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.FriendFactory;
import com.noplanbees.tbm.model.GridElement;
import com.noplanbees.tbm.model.GridElementFactory;
import com.noplanbees.tbm.multimedia.CameraException;
import com.noplanbees.tbm.multimedia.CameraManager;
import com.noplanbees.tbm.multimedia.CameraManager.CameraExceptionHandler;
import com.noplanbees.tbm.multimedia.VideoPlayer;
import com.noplanbees.tbm.network.FileDownloadService;
import com.noplanbees.tbm.network.FileUploadService;
import com.noplanbees.tbm.ui.dialogs.DoubleActionDialogFragment.DoubleActionDialogListener;
import com.noplanbees.tbm.ui.helpers.UnexpectedTerminationHelper;
import com.noplanbees.tbm.ui.helpers.VideoRecorderManager;
import com.noplanbees.tbm.ui.view.NineViewGroup;
import com.noplanbees.tbm.ui.view.NineViewGroup.LayoutCompleteListener;
import com.noplanbees.tbm.utilities.DialogShower;
import com.noplanbees.tbm.utilities.Logger;

import java.util.ArrayList;

// TODO: This file is still really ugly and needs to be made more organized and more readable. Some work may need to be factored out. -- Sani

public class GridViewFragment extends Fragment implements CameraExceptionHandler, SensorEventListener, DoubleActionDialogListener {

    private static final String TAG = GridViewFragment.class.getSimpleName();

    private ArrayList<GridElementController> viewControllers;

    private NineViewGroup nineViewGroup;
    private VideoPlayer videoPlayer;
    private VideoRecorderManager videoRecorderManager;

    private UnexpectedTerminationHelper unexpectedTerminationHelper = new UnexpectedTerminationHelper();

    private SensorManager sensorManager;
    private Sensor proximitySensor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");

        CameraManager.addExceptionHandlerDelegate(this);
        videoRecorderManager = new VideoRecorderManager(getActivity());

        unexpectedTerminationHelper.init();

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
        View v = inflater.inflate(R.layout.nineviewgroup_fragment, container, false);

        setupVideoPlayer(v);
        setupNineViewGroup(v);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.i(TAG, "onResume");
        videoRecorderManager.onResume();
        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
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
        videoRecorderManager.onPause();
        videoPlayer.release();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unexpectedTerminationHelper.finish();
    }

    private void setupNineViewGroup(View v) {
        nineViewGroup = (NineViewGroup) v.findViewById(R.id.grid_view);
        nineViewGroup.setGestureListener(new NineViewGestureListener());
        nineViewGroup.setChildLayoutCompleteListener(new LayoutCompleteListener() {
            @Override
            public void onLayoutComplete() {
                setupGridElements();
                layoutVideoRecorder();
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
        int i = 0;
        BenchViewManager benchViewManager;
        try {
            benchViewManager = ((BenchViewManager.Provider) getActivity()).getBenchViewManager();
        } catch (ClassCastException e) {
            throw new RuntimeException("Activity must inherit BenchViewManagerProvider.");
        }

        for (GridElement ge : GridElementFactory.getFactoryInstance().all()) {
            GridElementController gec = new GridElementController(getActivity(), ge, nineViewGroup.getSurroundingFrame(i), benchViewManager);
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
    public void onExceptionDialogActionClicked(int id, int button) {
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
        DialogShower.showCameraException(getActivity(), exception, this);
    }

    // TODO: Sani, should friend be moved to the grid on new message? --Serhii
    // Yes if user clicks on notification which opens app for an intent
    // Friend should be moved to the grid and play started automatically.
    // GridManager.getInstance().moveFriendToGrid(getActivity(), friend);

    public void play(String friendId) {
        Friend f = (Friend) FriendFactory.getFactoryInstance().find(friendId);

        if (f == null)
            throw new RuntimeException("Play from notification found no friendId: " + friendId);

        //GridManager.getInstance().moveFriendToGrid(getActivity(), f);
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
    public void onSensorChanged(SensorEvent event) {
        AudioManager am = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        if (event.values[0] == 0) {
            am.setMode(AudioManager.MODE_IN_CALL);
            am.setSpeakerphoneOn(false);
        } else {
            am.setMode(AudioManager.MODE_NORMAL);
            am.setSpeakerphoneOn(true);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // TODO: Serhii. Please remove this and have the controllers register directly for any callback they need.
    // my head is spinning tracing who is calling whom.
    private void notifyViewControllers(ViewControllerTask task) {
        for (GridElementController controller : viewControllers) {
            task.onEvent(controller);
        }
    }

    // TODO: again let us remove this and have the gridElementControllers registerFor and handle the callbacks they need
    // directly with the models.
    private interface ViewControllerTask {
        void onEvent(GridElementController controller);
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
        public boolean onCancelLongpress(String reason) {
            Log.d(TAG, "onCancelLongpress: " + reason);
            DialogShower.showToast(getActivity(), reason);
            videoRecorderManager.onRecordCancel();
            return false;
        }

        @Override
        public boolean onCenterClick(View view) {
            // TODO: add proper alert
            Log.d(TAG, "onCenterClick");
            return false;
        }

        @Override
        public boolean onCenterStartLongpress(View view) {
            // TODO: add proper alert
            Log.d(TAG, "onCenterStartLongpress");
            return false;
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
