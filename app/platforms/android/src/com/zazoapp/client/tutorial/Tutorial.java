package com.zazoapp.client.tutorial;

import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.zazoapp.client.PreferencesHelper;
import com.zazoapp.client.R;
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
        tutorialLayout.clear();
    }

    public void onLaunch(View view) {
        int friendsCount = FriendFactory.getFactoryInstance().count();
        int unviewedMessages = VideoFactory.getFactoryInstance().allNotViewedCount();
        Log.i(TAG, "onLaunch: friends " + friendsCount + " unviewed " + unviewedMessages);

        if (shouldShow(HintType.INVITE_1)) {
            showHint(HintType.INVITE_1, view);
        }
        if (!shouldShow(HintType.PLAY) && shouldShow(HintType.RECORD)) {
            showHint(HintType.RECORD, view);
        }

    }

    public void onNewMessage(View view) {
        Log.i(TAG, "onNewMessage");
        if (shouldShow(HintType.PLAY)) {
            showHint(HintType.PLAY, view);
        }
    }

    public void onVideoViewed(View view) {
        Log.i(TAG, "onVideoViewed");
        markHintAsShowed(HintType.PLAY);
        if (shouldShow(HintType.RECORD)) {
            if (!managers.getRecorder().isRecording() && !managers.getPlayer().isPlaying()) {
                showHint(HintType.RECORD, view);
            }
        }
    }

    public void onFriendModelChanged(View view) {
        int friendsCount = FriendFactory.getFactoryInstance().count();
        Log.i(TAG, "onFriendModelChanged: friends " + friendsCount);
        if (shouldShow(HintType.RECORD)) {
            if (!managers.getRecorder().isRecording() && !managers.getPlayer().isPlaying()) {
                showHint(HintType.RECORD, view);
            }
        }
    }

    public void onVideoSentIndicatorShowed(View view) {
        Log.i(TAG, "onVideoSentIndicatorShowed");
        markHintAsShowed(HintType.RECORD);
        if (shouldShow(HintType.SENT)) {
            showHint(HintType.SENT, view);
            markHintAsShowed(HintType.SENT);
        } else if (shouldShow(HintType.INVITE_2)) {
            View menu = ((View) tutorialLayout.getParent()).findViewById(R.id.home_menu);
            showHint(HintType.INVITE_2, menu);
        }
    }

    public void onVideoViewedIndicatorShowed(View view) {
        Log.i(TAG, "onVideoViewedIndicatorShowed");
        if (shouldShow(HintType.VIEWED)) {
            showHint(HintType.VIEWED, view);
            markHintAsShowed(HintType.VIEWED);
        }
    }

    private boolean shouldShow(HintType hint) {
        return hint.shouldShow(current, preferences);
    }

    private void showHint(HintType hint, View view) {
        Log.i(TAG, "Show hint " + hint + " " + HintType.getViewRect(view));
        current = hint;
        tutorialLayout.setHintText(current.getHint(tutorialLayout.getContext()));
        hint.show(tutorialLayout, view);
    }

    private void markHintAsShowed(HintType hint) {
        if (preferences.getBoolean(hint.getPrefName(), true)) {
            preferences.putBoolean(hint.getPrefName(), false);
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
    }

    @Override
    public void onDimmed() {

    }
}
