package com.zazoapp.client.tutorial;

import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.zazoapp.client.PreferencesHelper;
import com.zazoapp.client.TbmApplication;
import com.zazoapp.client.ZazoManagerProvider;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.VideoFactory;

/**
 * Created by skamenkovych@codeminders.com on 5/7/2015.
 */
public class Tutorial implements TutorialLayout.OnTutorialEventListener, View.OnTouchListener {
    private static final String TAG = Tutorial.class.getSimpleName();

    private TutorialLayout tutorialLayout;
    private ZazoManagerProvider managers;
    private PreferencesHelper preferences;
    private HintType current;
    private Runnable onNewMessageAction;
    private Runnable onNextHintAction;

    public Tutorial(TutorialLayout layout, ZazoManagerProvider managerProvider) {
        tutorialLayout = layout;
        managers = managerProvider;
        preferences = new PreferencesHelper(TbmApplication.getInstance());
        tutorialLayout.setOnTouchListener(this);
        tutorialLayout.setOnTutorialEventListener(this);
    }

    public void registerCallbacks() {
        //FriendFactory.getFactoryInstance().addVideoStatusObserver(this);
        //FriendFactory.getFactoryInstance().addCallback(this);
    }

    public void unregisterCallbacks() {
        //FriendFactory.getFactoryInstance().removeOnVideoStatusChangedObserver(this);
        //FriendFactory.getFactoryInstance().removeCallback(this);
        current = null;
        tutorialLayout.removeCallbacks(onNewMessageAction);
        tutorialLayout.clear();
    }

    public void onLaunch(final View view) {
        int friendsCount = FriendFactory.getFactoryInstance().count();
        int unviewedMessages = VideoFactory.getFactoryInstance().allNotViewedCount();
        Log.i(TAG, "onLaunch: friends " + friendsCount + " unviewed " + unviewedMessages);

        if (shouldShow(HintType.INVITE_1)) {
            showHint(HintType.INVITE_1, view);
        }
        if (!shouldShow(HintType.PLAY) && shouldShow(HintType.RECORD)) {
            showHint(HintType.RECORD, view);
            markHintAsShowedForSession(HintType.RECORD);
        } else if (shouldShow(HintType.PLAY)) {
            // FIX for https://zazo.fogbugz.com/f/cases/443/ caused by long view layout
            // Do not show this hint at all if it is still not loaded after some time
            onNewMessageAction = new Runnable() {
                @Override
                public void run() {
                    if (shouldShow(HintType.PLAY) && view.getWidth() != 0) {
                        showHint(HintType.PLAY, view);
                    }
                }
            };
            tutorialLayout.postDelayed(onNewMessageAction, 2000);
        }
    }

    public void onNewMessage(final View view) {
        Log.i(TAG, "onNewMessage");
        if (shouldShow(HintType.PLAY)) {
            showHint(HintType.PLAY, view);
        } else if (current != null) {
            tutorialLayout.dismiss();
            onNextHintAction = new Runnable() {
                @Override
                public void run() {
                    onNewMessage(view);
                }
            };
        }
    }

    public void onVideoViewed(View view) {
        Log.i(TAG, "onVideoViewed");
        if (shouldShow(HintType.RECORD)) {
            if (!managers.getRecorder().isRecording() && !managers.getPlayer().isPlaying()) {
                showHint(HintType.RECORD, view);
                markHintAsShowedForSession(HintType.RECORD);
            }
        }
    }

    public void onVideoStartPlayingByUser() {
        markHintAsShowed(HintType.PLAY);
    }

    public void onFriendModelChanged(View view) {
        int friendsCount = FriendFactory.getFactoryInstance().count();
        Log.i(TAG, "onFriendModelChanged: friends " + friendsCount);
        if (shouldShow(HintType.RECORD)) {
            showHint(HintType.RECORD, view);
            markHintAsShowedForSession(HintType.RECORD);
        }
    }

    public void onVideoSentIndicatorShowed(View view) {
        Log.i(TAG, "onVideoSentIndicatorShowed");
        if (shouldShow(HintType.SENT)) {
            showHint(HintType.SENT, view);
            markHintAsShowed(HintType.SENT);
        } else if (shouldShow(HintType.INVITE_2)) {
            showHint(HintType.INVITE_2, view);
            markHintAsShowedForSession(HintType.INVITE_2);
        }
    }

    public void onVideoRecorded() {
        markHintAsShowed(HintType.RECORD);
    }

    public void onVideoViewedIndicatorShowed(View view) {
        Log.i(TAG, "onVideoViewedIndicatorShowed");
        if (shouldShow(HintType.VIEWED)) {
            showHint(HintType.VIEWED, view);
            markHintAsShowed(HintType.VIEWED);
        }
    }

    private boolean shouldShow(HintType hint) {
        boolean inProcess = managers.getRecorder().isRecording() || managers.getPlayer().isPlaying();
        return !inProcess && hint.shouldShow(current, preferences);
    }

    private void showHint(HintType hint, View view) {
        Log.i(TAG, "Show hint " + hint + " " + HintType.getViewRect(view));
        if (managers.getBenchViewManager().isBenchShowed()) {
            managers.getBenchViewManager().hideBench();
        }
        current = hint;
        tutorialLayout.setHintText(current.getHint(tutorialLayout.getContext()));
        hint.show(tutorialLayout, view, this);
    }

    private void markHintAsShowed(HintType hint) {
        if (preferences.getBoolean(hint.getPrefName(), true)) {
            preferences.putBoolean(hint.getPrefName(), false);
        }
    }

    private void markHintAsShowedForSession(HintType hint) {
        if (preferences.getBoolean(hint.getPrefSessionName(), true)) {
            preferences.putBoolean(hint.getPrefSessionName(), false);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float posX = event.getX();
            float posY = event.getY();
            RectF rect = tutorialLayout.getExcludedRect();
            tutorialLayout.dismiss();
            return !rect.contains(posX, posY);
        }
        return false;
    }

    @Override
    public void onDismiss() {
        current = null;
        if (onNextHintAction != null) {
            onNextHintAction.run();
            onNextHintAction = null;
        }
    }

    @Override
    public void onDimmed() {

    }

    public HintType getCurrent() {
        return current;
    }
}
