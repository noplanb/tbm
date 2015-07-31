package com.zazoapp.client.tutorial;

import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.zazoapp.client.core.PreferencesHelper;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.IncomingVideoFactory;
import com.zazoapp.client.ui.ZazoManagerProvider;

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
        preferences.putBoolean(HintType.INVITE_2.getPrefSessionName(), true);
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
        int unviewedMessages = IncomingVideoFactory.getFactoryInstance().allNotViewedCount();
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
        } else if (managers.getFeatures().shouldShowAwardDialog()) {
            managers.getFeatures().showFeatureAwardDialog(managers, managers.getFeatures().lastUnlockedFeature());
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

    public void onFriendModelChanged(View view, Friend friend) {
        int friendsCount = FriendFactory.getFactoryInstance().count();
        Log.i(TAG, "onFriendModelChanged: friends " + friendsCount);
        if (shouldShow(HintType.RECORD)) {
            showHint(HintType.RECORD, view);
            markHintAsShowedForSession(HintType.RECORD);
        } else if (friend != null && friend.equals(managers.getInviteHelper().getLastInvitedFriend()) && shouldShow(HintType.SEND_WELCOME)) {
            if (shouldShow(HintType.SEND_WELCOME_WITH_RECORD) || !friend.hasApp()) {
                showHint(HintType.SEND_WELCOME_WITH_RECORD, view, HintType.SEND_WELCOME_WITH_RECORD.getHint(tutorialLayout.getContext(), friend.getFirstName()));
            } else {
                showHint(HintType.SEND_WELCOME, view, HintType.SEND_WELCOME.getHint(tutorialLayout.getContext(), friend.getFirstName()));
            }
            managers.getInviteHelper().dropLastInvitedFriend();
        }
    }

    public void onVideoSentIndicatorShowed(final View view) {
        Log.i(TAG, "onVideoSentIndicatorShowed");
        if (shouldShow(HintType.SENT)) {
            showHint(HintType.SENT, view);
            if (shouldShow(HintType.INVITE_2)) {
                onNextHintAction = new Runnable() {
                    @Override
                    public void run() {
                        showHint(HintType.INVITE_2, view);
                        markHintAsShowedForSession(HintType.INVITE_2);
                    }
                };
            }
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

    public void onMessageSent() {
        Features.Feature feature = managers.getFeatures().checkAndUnlock();
        if (feature != null) {
            tutorialLayout.dismiss();
            onNextHintAction = null;
            managers.getFeatures().showFeatureAwardDialog(managers, feature);
        }
    }

    private boolean shouldShow(HintType hint) {
        boolean inProcess = managers.getRecorder().isRecording() || managers.getPlayer().isPlaying();
        return !inProcess && hint.shouldShow(current, preferences);
    }

    private void showHint(HintType hint, View view) {
        showHint(hint, view, hint.getHint(tutorialLayout.getContext()));
    }

    private void showHint(HintType hint, View view, String text) {
        Log.i(TAG, "Show hint " + hint + " " + HintType.getViewRect(view));
        if (managers.getBenchViewManager().isBenchShowed()) {
            managers.getBenchViewManager().hideBench();
        }
        current = hint;
        tutorialLayout.setHintText(text);
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
