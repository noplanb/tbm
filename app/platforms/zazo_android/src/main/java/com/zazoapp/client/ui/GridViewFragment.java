package com.zazoapp.client.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.zazoapp.client.R;
import com.zazoapp.client.core.IntentHandlerService;
import com.zazoapp.client.core.PreferencesHelper;
import com.zazoapp.client.core.SyncManager;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.GridElement;
import com.zazoapp.client.model.GridElementFactory;
import com.zazoapp.client.model.GridManager;
import com.zazoapp.client.model.IncomingMessageFactory;
import com.zazoapp.client.multimedia.CameraException;
import com.zazoapp.client.multimedia.CameraManager;
import com.zazoapp.client.multimedia.CameraManager.CameraExceptionHandler;
import com.zazoapp.client.network.FileDownloadService;
import com.zazoapp.client.network.FileTransferService;
import com.zazoapp.client.network.FileUploadService;
import com.zazoapp.client.notification.NotificationAlertManager;
import com.zazoapp.client.ui.dialogs.DoubleActionDialogFragment.DoubleActionDialogListener;
import com.zazoapp.client.ui.helpers.GridElementController;
import com.zazoapp.client.ui.view.NineViewGroup;
import com.zazoapp.client.ui.view.NineViewGroup.LayoutCompleteListener;
import com.zazoapp.client.utilities.DialogShower;
import com.zazoapp.client.utilities.Logger;

import java.util.ArrayList;

public class GridViewFragment extends Fragment implements CameraExceptionHandler, DoubleActionDialogListener, NineViewGroup.SpinChangedListener, Features.FeatureChangedCallback {

    private static final String TAG = GridViewFragment.class.getSimpleName();
    private static final String PREF_SPIN_OFFSET = "spin_offset";

    private ArrayList<GridElementController> viewControllers;

    private NineViewGroup nineViewGroup;

    private boolean viewLoaded;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.i(TAG, "onCreate");
        CameraManager.addExceptionHandlerDelegate(this);
        viewControllers = new ArrayList<>(GridManager.GRID_ELEMENTS_COUNT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Logger.i(TAG, "onCreateView");
        viewLoaded = false;
        View v = inflater.inflate(R.layout.nineviewgroup_fragment, container, false);

        setupVideoPlayer(v);
        setupNineViewGroup(v);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getManagerProvider().getFeatures().addCallback(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.i(TAG, "onResume");
        setupSpinFeature(getActivity());
        CameraManager.setUsePreferredPreviewSize(false);
        getManagerProvider().getRecorder().resume();
    }

    @Override
    public void onStart() {
        super.onStart();
        FileTransferService.reset(getActivity(), FileDownloadService.class);
        FileTransferService.reset(getActivity(), FileUploadService.class);
        SyncManager.getAndPollAllFriends(getActivity(), getManagerProvider());
    }

    private void setupNineViewGroup(View v) {
        nineViewGroup = (NineViewGroup) v.findViewById(R.id.grid_view);
        setupSpinFeature(getActivity());
        nineViewGroup.setGestureRecognizer(getManagerProvider());
        nineViewGroup.setGestureListener(new NineViewGestureListener());
        nineViewGroup.setChildLayoutCompleteListener(new LayoutCompleteListener() {
            @Override
            public void onLayoutComplete() {
                Logger.i(TAG, "onLayoutComplete " + viewLoaded);
                if (!viewLoaded) {
                    setupGridElements();
                    layoutVideoRecorder();
                    viewLoaded = true;
                }
                getManagerProvider().getPlayer().updatePlayerPosition();
                getManagerProvider().getTutorial().update();
                if (isFocused(false)) {
                    handleIntentAction(getActivity().getIntent());
                }
            }
        });
    }

    private void setupSpinFeature(Context context) {
        if (nineViewGroup.getSpinStrategy() == null) {
            nineViewGroup.setSpinStrategy(new RectangleSpin(nineViewGroup));
            nineViewGroup.setSpinOffset(new PreferencesHelper(context).getInt(PREF_SPIN_OFFSET, 0));
            nineViewGroup.setSpinChangedListener(this);
        }
        nineViewGroup.enableSpin(getManagerProvider().getFeatures().isUnlocked(Features.Feature.CAROUSEL));
    }

    private void setupVideoPlayer(View v) {
        getManagerProvider().getPlayer().init(v);
    }

    private ZazoManagerProvider getManagerProvider() {
        return TbmApplication.getInstance().getManagerProvider();
    }

    private void setupGridElements(){
        if (!viewControllers.isEmpty()) {
            for (GridElementController controller : viewControllers) {
                controller.cleanUp();
            }
            viewControllers.clear();
        }
        int i = 0;
        for (GridElement ge : GridElementFactory.getFactoryInstance().all()) {
            GridElementController gec = new GridElementController(getActivity(), ge, nineViewGroup.getSurroundingFrame(i),
                    getManagerProvider());
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
        CameraManager.setUsePreferredPreviewSize(false);
        getManagerProvider().getRecorder().addPreviewTo(fl, true);
    }

    // -------------------------------
    // CameraExceptionHandler delegate
    // -------------------------------
    @Override
    public void onDialogActionClicked(int id, int button, Bundle bundle) {
        switch (button) {
            case DoubleActionDialogListener.BUTTON_POSITIVE:
                getManagerProvider().getRecorder().reconnect();
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
            if (IncomingMessageFactory.getFactoryInstance().allNotViewedCount() > 0) {    // has at least one unviewed video
                DialogShower.showHintDialog(getActivity(), getString(R.string.dialog_hint_title),
                        getString(R.string.dialog_play_hint_message));
            } else {
                DialogShower.showHintDialog(getActivity(), getString(R.string.dialog_hint_title),
                        getString(R.string.dialog_record_hint_message));
            }
        }
    }

    public void play(String friendId) {
        Friend f = FriendFactory.getFactoryInstance().find(friendId);

        if (f == null)
            throw new RuntimeException("Play from notification found no friendId: " + friendId);

        int index = GridElementFactory.getFactoryInstance().gridElementIndexWithFriend(f);

        if (index == -1) {
            Log.d(TAG, "Play from notification found no grid element index for friendId: " + friendId);
            return;
        }
        View view = nineViewGroup.getSurroundingFrame(index);
        if (getManagerProvider().getPlayer().togglePlayOverView(view, friendId, 0)) {
            getManagerProvider().getTutorial().onVideoStartPlaying();
        }
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
            Logger.i(TAG, "handleIntentAction: no intent. Exiting.");
            return;
        }
        if (!viewLoaded) {
            Logger.i(TAG, "handleIntentAction: View is not loaded yet or showed to user. Ignore for now.");
            return;
        }
        Logger.i(TAG, "handleIntentAction: " + currentIntent.toString());

        String action = currentIntent.getAction();
        Uri data = currentIntent.getData();

        if (Intent.ACTION_MAIN.equals(action)) {
            currentIntent.setAction(IntentHandlerService.IntentActions.NONE);
            getManagerProvider().getTutorial().onLaunch(nineViewGroup.getFrame(NineViewGroup.Box.CENTER_RIGHT));
            return;
        }

        if (IntentHandlerService.IntentActions.SHOW_AWARD.equals(action) || IntentHandlerService.IntentActions.NONE.equals(action) && getManagerProvider().getFeatures().shouldShowAwardDialog()) {
            Logger.i(TAG, "handleIntentAction: show award");
            getManagerProvider().getTutorial().showAward();
            currentIntent.setAction(IntentHandlerService.IntentActions.NONE);
            return;
        }

        if (action == null || data == null) {
            Logger.i(TAG, "handleIntentAction: no action or data. Exiting.");
            return;
        }

        String friendId = currentIntent.getData().getQueryParameter(IntentHandlerService.IntentParamKeys.FRIEND_ID);
        if (friendId == null) {
            Logger.i(TAG, "handleIntentAction: no friendId or action. Exiting." + currentIntent.toString());
            return;
        }

        if (action.equals(IntentHandlerService.IntentActions.PLAY_VIDEO)) {
            Logger.i(TAG, "handleIntentAction: play video");
            currentIntent.setAction(IntentHandlerService.IntentActions.NONE);
            play(friendId);
            return;
        }

        // Not used as I decided pending intent coming back from sending sms is
        // to disruptive. Just assume
        // sms's sent go through.
        if (action.equals(IntentHandlerService.IntentActions.SMS_RESULT)) {
            currentIntent.setAction(IntentHandlerService.IntentActions.NONE);
            Log.i(TAG, currentIntent.toString());
        }
    }

    @Override
    public void onSpinChanged(int newSpinOffset) {
        Context context = getActivity();
        if (context != null) {
            new PreferencesHelper(context).putInt(PREF_SPIN_OFFSET, newSpinOffset);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (nineViewGroup.getSpinStrategy() != null) {
            nineViewGroup.getSpinStrategy().reset();
        }
    }

    @Override
    public void onFeatureChanged(Features.Feature feature, boolean unlocked) {
        if (feature == Features.Feature.CAROUSEL && nineViewGroup != null) {
            nineViewGroup.enableSpin(unlocked);
        }
        if (feature == Features.Feature.SWITCH_CAMERA && nineViewGroup != null) {
            getManagerProvider().getRecorder().indicateSwitchCameraFeature();
        }
    }

    private boolean isFocused(boolean activityFocused) {
        Activity activity = getActivity();
        return activity != null && (activity.hasWindowFocus() || activityFocused) && !NotificationAlertManager.screenIsLocked(activity);
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        Logger.i(TAG, "onWindowFocusChanged " + hasFocus);
        Intent intent = getActivity().getIntent();
        if (isFocused(hasFocus) && intent != null) {
            handleIntentAction(intent);
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
                getManagerProvider().getPlayer().stop();
                if (getManagerProvider().getAudioController().gainFocus()) {
                    getManagerProvider().getRecorder().start(friendId);
                } else {
                    DialogShower.showToast(getActivity(), R.string.toast_could_not_get_audio_focus);
                }
            }
            return true;
        }

        @Override
        public boolean onEndLongpress(int position) {
            Log.d(TAG, "onEndLongpress");
            if (getManagerProvider().getRecorder().stop()) {
                GridElement ge = GridElementFactory.getFactoryInstance().get(position);
                String friendId = ge.getFriendId();
                if (friendId != null && !friendId.equals("")) {
                    Friend f = FriendFactory.getFactoryInstance().find(friendId);
                    if (f != null) {
                        Log.i(TAG, "onRecordStop: STOP RECORDING. to " + f.get(Friend.Attributes.FIRST_NAME));
                        f.requestUpload(f.getOutgoingVideoId());
                        getManagerProvider().getTutorial().onVideoRecorded(f);
                    }
                }
            }
            return false;
        }

        @Override
        public boolean onCancelLongpress(int reason) {
            Log.d(TAG, "onCancelLongpress: " + getActivity().getString(reason));
            if (getManagerProvider().getRecorder().isRecording()) {
                DialogShower.showToast(getActivity(), reason);
                getManagerProvider().getRecorder().cancel();
            }
            return false;
        }

        @Override
        public boolean onSurroundingMovingAway(View view, int position) {
            return false;
        }

        @Override
        public boolean onSurroundingMovingIn(View view, int position) {
            return false;
        }

        @Override
        public void notifyUpdateDebug() {
            if (getManagerProvider().getPlayer().isPlaying()) {
                getManagerProvider().getPlayer().stop();
            }
        }

        @Override
        public boolean onCenterClick(View view) {
            if (getManagerProvider().getFeatures().isUnlocked(Features.Feature.SWITCH_CAMERA)) {
                getManagerProvider().getRecorder().switchCamera();
            } else {
                checkAndShowHint();
            }
            return true;
        }

        @Override
        public boolean onCenterStartLongpress(View view) {
            checkAndShowHint();
            return true;
        }
    }
}
