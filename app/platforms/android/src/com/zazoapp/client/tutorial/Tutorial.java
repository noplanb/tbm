package com.zazoapp.client.tutorial;

import android.graphics.RectF;
import android.util.Log;
import android.view.View;
import com.zazoapp.client.PreferencesHelper;
import com.zazoapp.client.TbmApplication;
import com.zazoapp.client.ZazoManagerProvider;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.VideoFactory;

/**
 * Created by skamenkovych@codeminders.com on 5/7/2015.
 */
public class Tutorial {
    private static final String TAG = Tutorial.class.getSimpleName();

    private TutorialLayout tutorialLayout;
    private ZazoManagerProvider managers;
    private PreferencesHelper preferences;

    public Tutorial(TutorialLayout layout, ZazoManagerProvider managerProvider) {
        tutorialLayout = layout;
        managers = managerProvider;
        preferences = new PreferencesHelper(TbmApplication.getInstance());
        tutorialLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tutorialLayout.dismiss();
            }
        });
    }

    public void registerCallbacks() {
        //FriendFactory.getFactoryInstance().addVideoStatusObserver(this);
        //FriendFactory.getFactoryInstance().addCallback(this);
    }

    public void unregisterCallbacks() {
        //FriendFactory.getFactoryInstance().removeOnVideoStatusChangedObserver(this);
        //FriendFactory.getFactoryInstance().removeCallback(this);
    }

    public void onLaunch(View view) {
        int friendsCount = FriendFactory.getFactoryInstance().count();
        int unviewedMessages = VideoFactory.getFactoryInstance().allNotViewedCount();
        Log.i(TAG, "onLaunch: friends " + friendsCount + " unviewed " + unviewedMessages);

        if (friendsCount == 0) {
            showInviteHint1(view);
        }
        //Points to: Orange plus in center right box #1.
        //
        //Msg: “Send a Zazo”
        //
        //Displayed if:
        //User has no friends.
        //
        //Displayed when (event):
        //Every time app is launched in this state.

        //PlayHint
    }

    public void onNewMessage(View view) {
        Log.i(TAG, "onNewMessage: " + getViewRect(view));
        //tutorialLayout.dimExceptForRect(getViewRect(view));
        //delayedDismiss();
        //PlayHint
        //Points to: Unviewed message in center right box #1.
        //
        //Msg: “Tap to play”
        //
        //Displayed if:
        //User has at least 1 unviewed message &&
        //User has only 1 friend &&
        //        User has never played a message ever before
        //
        //Displayed when (event):
        //When this state occurs for the first time e.g.
        //        The first message arrives.
        //        Each time app is launched in this state

    }

    public void onVideoViewed(View view, String friendId) {
        Log.i(TAG, "onVideoViewed: " + getViewRect(view));
        tutorialLayout.dimExceptForRect(getViewRect(view));
        delayedDismiss();
        //RecordHint
    }

    public void onFriendModelChanged(View view, String friendId) {
        int friendsCount = FriendFactory.getFactoryInstance().count();
        Log.i(TAG, "onFriendModelChanged: friends " + friendsCount + " " + getViewRect(view));
        tutorialLayout.dimExceptForRect(getViewRect(view));
        delayedDismiss();
        //RecordHint
    }

    public void onVideoSentIndicatorShowed(View indicator) {
        Log.i(TAG, "onVideoSentIndicatorShowed: " + getViewRect(indicator));
        tutorialLayout.dimExceptForRect(getViewRect(indicator));
        delayedDismiss();
        // SentHint
        // InviteHint2
    }

    public void onVideoViewedIndicatorShowed(View indicator) {
        Log.i(TAG, "onVideoViewedIndicatorShowed: " + getViewRect(indicator));
        tutorialLayout.dimExceptForRect(getViewRect(indicator));
        delayedDismiss();
        // ViewedHint
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

    private void delayedDismiss() {
        tutorialLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                tutorialLayout.dismiss();
            }
        }, 2000);
    }

    private void showInviteHint1(View view) {
        tutorialLayout.dimExceptForRect(getViewRect(view));
    }
}
