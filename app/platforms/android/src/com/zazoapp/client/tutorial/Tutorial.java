package com.zazoapp.client.tutorial;

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

        int left = view.getLeft();
        int top = view.getTop();
        int right = view.getRight();
        int bottom = view.getBottom();
        Log.i(TAG, "onLaunch: " + left + " " + right + " " + top + " " + bottom);
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
        int left = view.getLeft();
        int top = view.getTop();
        int right = view.getRight();
        int bottom = view.getBottom();
        Log.i(TAG, "onNewMessage: " + left + " " + right + " " + top + " " + bottom);
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
        int left = view.getLeft();
        int top = view.getTop();
        int right = view.getRight();
        int bottom = view.getBottom();
        Log.i(TAG, "onVideoViewed: " + left + " " + right + " " + top + " " + bottom);
        //RecordHint
    }

    public void onFriendModelChanged(View view, String friendId) {
        int friendsCount = FriendFactory.getFactoryInstance().count();
        Log.i(TAG, "onFriendModelChanged: friends " + friendsCount);
        //RecordHint
    }

    public void onVideoSentIndicatorShowed(View indicator) {
        int left = indicator.getLeft();
        int top = indicator.getTop();
        int right = indicator.getRight();
        int bottom = indicator.getBottom();
        Log.i(TAG, "onVideoSentIndicatorShowed: " + left + " " + right + " " + top + " " + bottom);
        // SentHint
        // InviteHint2
    }

    public void onVideoViewedIndicatorShowed(View indicator) {
        int left = indicator.getLeft();
        int top = indicator.getTop();
        int right = indicator.getRight();
        int bottom = indicator.getBottom();
        Log.i(TAG, "onVideoViewedIndicatorShowed: " + left + " " + right + " " + top + " " + bottom);
        // ViewedHint
    }
}
