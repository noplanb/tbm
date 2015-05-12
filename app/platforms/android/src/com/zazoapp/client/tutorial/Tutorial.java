package com.zazoapp.client.tutorial;

import android.content.Context;
import android.graphics.RectF;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import com.zazoapp.client.PreferencesHelper;
import com.zazoapp.client.R;
import com.zazoapp.client.TbmApplication;
import com.zazoapp.client.ZazoManagerProvider;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.VideoFactory;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by skamenkovych@codeminders.com on 5/7/2015.
 */
public class Tutorial implements TutorialLayout.OnTutorialEventListener, View.OnTouchListener {
    private static final String TAG = Tutorial.class.getSimpleName();

    private TutorialLayout tutorialLayout;
    private ZazoManagerProvider managers;
    private PreferencesHelper preferences;
    private Hint current;
    private Queue<Pair<Hint, View>> hintsQueue = new LinkedList<>();

    private enum Hint {
        INVITE_1(R.string.tutorial_hint_invite_1),
        INVITE_2(R.string.tutorial_hint_invite_2),
        PLAY(R.string.tutorial_hint_play),
        RECORD(R.string.tutorial_hint_record),
        SENT(R.string.tutorial_hint_sent),
        VIEWED(R.string.tutorial_hint_viewed);

        private String prefName;
        private int hintTextId;

        Hint(int id) {
            prefName = "pref_hint_" + name().toLowerCase();
            hintTextId = id;
        }

        public String getPrefName() {
            return prefName;
        }

        public String getHint(Context context) {
            return context.getString(hintTextId);
        }
    }

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

        if (friendsCount == 0) {
            showInviteHint1(view);
        }
        if (!shouldShowPlayHint() && shouldShowRecordHint()) {
            showRecordHint(view);
        }

    }

    public void onNewMessage(View view) {
        Log.i(TAG, "onNewMessage: " + getViewRect(view));
        if (shouldShowPlayHint()) {
            Log.i(TAG, "~~~~~ showPlayHint");
            showPlayHint(view);
        }
    }

    public void onVideoViewed(View view, String friendId) {
        Log.i(TAG, "onVideoViewed: " + getViewRect(view));
        if (shouldShowRecordHint() && !managers.getRecorder().isRecording()) {
            showRecordHint(view);
        }
    }

    public void onFriendModelChanged(View view, String friendId) {
        int friendsCount = FriendFactory.getFactoryInstance().count();
        Log.i(TAG, "onFriendModelChanged: friends " + friendsCount + " " + getViewRect(view));
        if (shouldShowRecordHint()) {
            showRecordHint(view);
        }
    }

    public void onVideoSentIndicatorShowed(View view) {
        Log.i(TAG, "onVideoSentIndicatorShowed: " + getViewRect(view));
        if (shouldShowSentHint()) {
            showSentHint(view);
        } else if (shouldShowInvite2Hint()) {
            View menu = ((View) tutorialLayout.getParent()).findViewById(R.id.home_menu);
            showInviteHint2(menu);
        }
    }

    public void onVideoViewedIndicatorShowed(View view) {
        Log.i(TAG, "onVideoViewedIndicatorShowed: " + getViewRect(view));
        if (shouldShowViewedHint()) {
            showViewedHint(view);
        }
    }

    private RectF getViewRect(View view) {
        int[] location = new int[2];
        view.getLocationInWindow(location);
        int left = location[0];
        int top = location[1];
        int right = left + view.getWidth();
        int bottom = top + view.getHeight();
        return new RectF(left, top, right, bottom);
    }

    private int[] getViewInnerCircle(View view) {
        int[] circleData = new int[3];
        int[] location = new int[2];
        view.getLocationInWindow(location);
        circleData[0] = location[0] + view.getWidth() / 2;
        circleData[1] = location[1] + view.getHeight() / 2;
        circleData[2] = Math.min(view.getWidth(), view.getHeight()) / 2;
        return circleData;
    }

    private int[] getViewOuterCircle(View view) {
        int[] circleData = new int[3];
        int[] location = new int[2];
        view.getLocationInWindow(location);
        circleData[0] = location[0] + view.getWidth() / 2;
        circleData[1] = location[1] + view.getHeight() / 2;
        circleData[2] = (int) (Math.max(view.getWidth(), view.getHeight()) * 0.8);
        return circleData;
    }

    private void delayedDismiss() {
        tutorialLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                tutorialLayout.dismiss();
            }
        }, 2000);
    }

    private void showInviteHint1(View view) {
        Log.i(TAG, "~~~~~ showInviteHint1 " + getViewRect(view));
        current = Hint.INVITE_1;
        setHintText();
        tutorialLayout.dimExceptForRect(getViewRect(view));
    }

    private void showPlayHint(View view) {
        Log.i(TAG, "~~~~~ showPlayHint " + getViewRect(view));
        current = Hint.PLAY;
        setHintText();
        setExcludedBox(view);
        tutorialLayout.dim();
    }

    private void showRecordHint(View view) {
        Log.i(TAG, "~~~~~ showRecordHint " + getViewRect(view));
        current = Hint.RECORD;
        setHintText();
        tutorialLayout.setHintText(current.getHint(view.getContext()));
        setExcludedBox(view);
        tutorialLayout.dim();
    }

    private void showSentHint(View view) {
        Log.i(TAG, "~~~~~ showSentHint " + getViewRect(view));
        current = Hint.SENT;
        setHintText();
        View indicator = view.findViewById(R.id.img_uploading);
        tutorialLayout.dimExceptForRect(getViewRect(indicator));
    }

    private void showViewedHint(View view) {
        Log.i(TAG, "~~~~~ showViewedHint " + getViewRect(view));
        current = Hint.VIEWED;
        setHintText();
        View indicator = view.findViewById(R.id.img_viewed);
        tutorialLayout.dimExceptForRect(getViewRect(indicator));
    }

    private void showInviteHint2(View view) {
        Log.i(TAG, "~~~~~ showInviteHint2 " + getViewRect(view));
        current = Hint.INVITE_2;
        setHintText();
        tutorialLayout.dimExceptForRect(getViewRect(view));
    }

    private void setExcludedBox(View view) {
        View indicator = view.findViewById(R.id.tw_unread_count);
        if (indicator.getVisibility() == View.VISIBLE) {
            tutorialLayout.setExcludedCircle(getViewInnerCircle(indicator));
        } else {
            tutorialLayout.setExcludedCircle(0, 0, 0);
        }
        tutorialLayout.setExcludedRect(getViewRect(view));
    }

    private void setHintText() {
        tutorialLayout.setHintText(current.getHint(tutorialLayout.getContext()));
    }

    private void dismissInviteHint1() {
        if (current == Hint.INVITE_1) {
            tutorialLayout.dismiss();
        }
    }

    private boolean shouldShowPlayHint() {
        int friendsCount = FriendFactory.getFactoryInstance().count();
        int unviewedCount = VideoFactory.getFactoryInstance().allNotViewedCount();
        return friendsCount > 0 && unviewedCount > 0 && preferences.getBoolean(Hint.PLAY.getPrefName(), true);
    }

    private boolean shouldShowRecordHint() {
        int friendsCount = FriendFactory.getFactoryInstance().count();
        return friendsCount > 0 && current != Hint.PLAY && preferences.getBoolean(Hint.RECORD.getPrefName(), true);
    }

    private boolean shouldShowViewedHint() {
        int friendsCount = FriendFactory.getFactoryInstance().count();
        return friendsCount > 0 && current == null && preferences.getBoolean(Hint.VIEWED.getPrefName(), true);
    }

    private boolean shouldShowSentHint() {
        int friendsCount = FriendFactory.getFactoryInstance().count();
        return friendsCount > 0 && current == null && preferences.getBoolean(Hint.SENT.getPrefName(), true);
    }

    private boolean shouldShowInvite2Hint() {
        boolean oneFriend = FriendFactory.getFactoryInstance().count() == 1;
        boolean allViewed = VideoFactory.getFactoryInstance().allNotViewedCount() == 0;
        boolean playHintShowed = !preferences.getBoolean(Hint.PLAY.getPrefName(), true);
        boolean recordHintShowed = !preferences.getBoolean(Hint.RECORD.getPrefName(), true);
        boolean sentHintShowed = !preferences.getBoolean(Hint.SENT.getPrefName(), true);
        boolean viewedHintShowed = !preferences.getBoolean(Hint.VIEWED.getPrefName(), true);

        return oneFriend && allViewed && playHintShowed && recordHintShowed && sentHintShowed && viewedHintShowed;
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
