package com.zazoapp.client.tutorial;

import android.app.Activity;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import butterknife.ButterKnife;
import com.zazoapp.client.R;
import com.zazoapp.client.core.PreferencesHelper;
import com.zazoapp.client.core.SyncManager;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.dispatch.ZazoAnalytics;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.IncomingMessageFactory;
import com.zazoapp.client.ui.ZazoManagerProvider;
import com.zazoapp.client.ui.view.GridElementView;
import com.zazoapp.client.ui.view.NineViewGroup;
import com.zazoapp.client.utilities.Convenience;

/**
 * Created by skamenkovych@codeminders.com on 5/7/2015.
 */
public class Tutorial implements TutorialLayout.OnTutorialEventListener, View.OnTouchListener {
    private static final String TAG = Tutorial.class.getSimpleName();
    public static final String HINT_TYPE_KEY = "hint_type";
    public static final String FEATURE_KEY = "feature";
    public static final String FRIEND_KEY = "just_invited_friend";
    public static final String BOX_KEY = "view_id";

    private TutorialLayout tutorialLayout;
    private DrawerLayout drawer;
    private Activity activity;
    private ZazoManagerProvider managers;
    private PreferencesHelper preferences;
    private HintType current;
    private Runnable onNewMessageAction;
    private Runnable onNextHintAction;
    private boolean isLaunched;

    public Tutorial(Activity activity, ZazoManagerProvider managerProvider) {
        this.activity = activity;
        tutorialLayout = ButterKnife.findById(activity, R.id.tutorial_layout);
        drawer = ButterKnife.findById(activity, R.id.drawer_layout);
        managers = managerProvider;
        preferences = new PreferencesHelper(TbmApplication.getInstance());
        tutorialLayout.setOnTouchListener(this);
        tutorialLayout.setOnTutorialEventListener(this);
    }

    public void registerCallbacks() {
        preferences.putBoolean(HintType.RECORD.getPrefSessionName(), true);
        preferences.putBoolean(HintType.INVITE_2.getPrefSessionName(), true);
        preferences.putBoolean(HintType.NEXT_FEATURE.getPrefSessionName(), true);
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
        int unviewedMessages = IncomingMessageFactory.getFactoryInstance().allNotViewedCount();
        Log.i(TAG, "onLaunch: friends " + friendsCount + " unviewed " + unviewedMessages);
        isLaunched = true;
        if (managers.getFeatures().shouldShowAwardDialog()) {
            managers.getFeatures().showFeatureAwardDialog(managers, managers.getFeatures().lastUnlockedFeature());
        } else {
            HintType hint = getHintToShow(TutorialEvent.LAUNCH, null);
            if (hint != null) {
                showHint(hint, view);
            } else if (onNextHintAction != null) {
                onNextHintAction.run();
            }
        }
    }

    public void onNewMessage(final View view) {
        Log.i(TAG, "onNewMessage");
        HintType hint = getHintToShow(TutorialEvent.NEW_MESSAGE, null);
        if (hint != null) {
            showHint(hint, view);
        }
    }

    public void onVideoViewed(final View view) {
        Log.i(TAG, "onVideoViewed " + isLaunched);
        HintType hint = getHintToShow(TutorialEvent.VIDEO_VIEWED, null);
        if (hint != null) {
            showHint(hint, view);
        }
    }

    public void onVideoStartPlayingByUser() {
        HintType.PLAY.markHintAsShowed(preferences);
    }

    public void onFriendModelChanged(View view, Friend friend) {
        Log.i(TAG, "onFriendModelChanged");
        Bundle params = new Bundle();
        if (friend != null && friend.equals(managers.getInviteHelper().getLastInvitedFriend())) {
            params.putString(FRIEND_KEY, friend.getId());
        }
        HintType hint = getHintToShow(TutorialEvent.FRIEND_ADDED, params);
        if (hint != null) {
            if (hint == HintType.SEND_WELCOME_WITH_RECORD || hint == HintType.SEND_WELCOME) {
                showHint(hint, view, hint.getHint(tutorialLayout.getContext(), friend.getFirstName()));
                managers.getInviteHelper().dropLastInvitedFriend();
            } else {
                showHint(hint, view);
            }
        }
    }

    public void onVideoSentIndicatorShowed(final View view) {
        Log.i(TAG, "onVideoSentIndicatorShowed");
        HintType hint = getHintToShow(TutorialEvent.SENT_INDICATOR_SHOWED, null);
        if (hint != null) {
            showHint(hint, view);
        }
    }

    public void onVideoRecorded(Friend friend) {
        HintType.RECORD.markHintAsShowed(preferences);
        if (!friend.hasApp() && friend.everSent()) {
            managers.getInviteHelper().nudge(friend);
        }
    }

    public void onVideoViewedIndicatorShowed(final View view) {
        Log.i(TAG, "onVideoViewedIndicatorShowed");
        final Bundle params = new Bundle();
        if (view instanceof GridElementView) {
            NineViewGroup.Box box = NineViewGroup.Box.values()[((View) view.getParent()).getId()];
            params.putInt(Tutorial.BOX_KEY, box.ordinal());
        }
        Runnable showVideoViewedIndicatorHintTask = new Runnable() {
            @Override
            public void run() {
                HintType hint = getHintToShow(TutorialEvent.VIEWED_INDICATOR_SHOWED, params);
                if (hint != null) {
                    showHint(hint, view);
                }
            }
        };
        if (isLaunched) {
            showVideoViewedIndicatorHintTask.run();
        } else if (onNextHintAction == null) {
            onNextHintAction = showVideoViewedIndicatorHintTask;
        }
    }

    public void onMessageSent(View view, Friend friend) {
        if (friend != null && !friend.everSent()) {
            friend.setEverSent(true);
            ZazoAnalytics.trackEvent("UniqueZazo");
            if (HintType.RECORD.isHintShowed(preferences) && FriendFactory.getFactoryInstance().getNumberOfEverSentFriends() == 1) {
                ZazoAnalytics.trackEvent("FirstZazo");
            }
            final Features.Feature feature = managers.getFeatures().checkAndUnlock();
            SyncManager.syncWelcomedFriends(managers);
            if (feature != null) {
                if (current != null) {
                    onNextHintAction = new Runnable() {
                        @Override
                        public void run() {
                            managers.getFeatures().showFeatureAwardDialog(managers, feature);
                        }
                    };
                } else {
                    managers.getFeatures().showFeatureAwardDialog(managers, feature);
                }
                return;
            }
        }
        HintType hint = getHintToShow(TutorialEvent.MESSAGE_SENT, null);
        if (hint != null) {
            showHint(hint, view);
        }
    }

    private HintType getHintToShow(TutorialEvent event, Bundle params) {
        boolean inProcess = managers.getRecorder().isRecording() || managers.getPlayer().isPlaying();
        boolean featureAwardDialogShowed = managers.getFeatures().isAwardDialogShowed();
        if (!inProcess && !featureAwardDialogShowed) {
            return HintType.shouldShowHintByPriority(event, current, preferences, params);
        }
        return null;
    }

    private void showHint(HintType hint, View view) {
        if (hint == HintType.NEXT_FEATURE || hint == HintType.NEXT_FEATURE_AFTER_UNLOCK) {
            if (managers.getFeatures().showNextFeatureDialog(managers, hint == HintType.NEXT_FEATURE_AFTER_UNLOCK)) {
                current = hint;
                HintType.NEXT_FEATURE.markHintAsShowedForSession(preferences);
            }
        } else {
            showHint(hint, view, hint.getHint(tutorialLayout.getContext()));
        }
    }

    private void showHint(HintType hint, View view, String text) {
        Log.i(TAG, "Show hint " + hint + " " + Convenience.getViewRect(view));
        if (managers.getBenchViewManager().isBenchShown() && !managers.getInviteHelper().isInvitationInProcess()) {
            managers.getBenchViewManager().hideBench();
        }
        if (drawer.isDrawerOpen(Gravity.LEFT)) {
            drawer.closeDrawer(Gravity.LEFT);
        }
        current = hint;
        tutorialLayout.setHintText(text);
        tutorialLayout.setButtonText(hint.getButtonText(activity));
        hint.show(tutorialLayout, view, this, preferences);
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

    public boolean isShown() {
        return current != null;
    }

    public void dismissHint() {
        tutorialLayout.dismiss();
    }

    @Override
    public void onDismiss() {
        HintType last = current;
        current = null;
        if (onNextHintAction != null) {
            onNextHintAction.run();
            onNextHintAction = null;
        } else {
            if (last != null) { // for the case it was unregistered before completely dismissed last can be null
                Bundle params = new Bundle();
                params.putInt(HINT_TYPE_KEY, last.ordinal());
                HintType hint = getHintToShow(TutorialEvent.HINT_DISMISSED, params);
                if (hint != null) {
                    showHint(hint, activity.findViewById(R.id.grid_view));
                }
            }
        }
    }

    @Override
    public void onDimmed() {

    }

    public HintType getCurrent() {
        return current;
    }

    public void onFeatureAwardDialogHidden() {
        Bundle params = new Bundle();
        params.putInt(FEATURE_KEY, managers.getFeatures().lastUnlockedFeature().ordinal());
        HintType hint = getHintToShow(TutorialEvent.FEATURE_AWARD_DISMISSED, params);
        if (hint != null) {
            showHint(hint, activity.findViewById(R.id.grid_view));
        }
    }

    public void onVideoStartPlaying() {
        if (isShown()) {
            dismissHint();
        }
    }

    public void showAward() {
        if (managers.getFeatures().shouldShowAwardDialog() && !isShown()) {
            managers.getFeatures().showFeatureAwardDialog(managers, managers.getFeatures().lastUnlockedFeature());
        }
    }

    public void update() {
        if (isShown()) {
            Log.i(TAG, "update");
            tutorialLayout.updateViews();
        }
    }

    public void updateForView(View view) {
        if (view == tutorialLayout.getHelpView()) {
            update();
        }
    }
}
