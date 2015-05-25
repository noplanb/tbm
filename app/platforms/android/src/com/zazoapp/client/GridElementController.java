package com.zazoapp.client;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import com.zazoapp.client.model.ActiveModel;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.GridElement;
import com.zazoapp.client.model.Video;
import com.zazoapp.client.multimedia.VideoPlayer;
import com.zazoapp.client.notification.NotificationAlertManager;
import com.zazoapp.client.ui.view.GridElementView;
import com.zazoapp.client.utilities.DialogShower;

/**
 * Created by User on 1/30/2015.
 */
public class GridElementController implements GridElementView.ClickListener, VideoPlayer.StatusCallbacks,
        GridElementView.FriendViewListener, ActiveModel.ModelChangeCallback, Friend.VideoStatusChangedCallback {

    private static final String TAG = GridElementController.class.getSimpleName();

    private GridElement gridElement;
    private ViewGroup container;
    private GridElementView gridElementView;
    private Activity activity;
    private ZazoManagerProvider managerProvider;
    private boolean isVideoPlaying = false;

    private Handler uiHandler = new Handler(Looper.getMainLooper());

    public GridElementController(Activity activity, GridElement gridElement, ViewGroup container, ZazoManagerProvider managerProvider) {
        Log.i(TAG, "instance with view " + container);
        this.activity = activity;
        this.container = container;
        this.gridElement = gridElement;
        this.managerProvider = managerProvider;
        setUpView();
    }

    private void setUpView() {
        if (container.getChildCount() == 0) {
            gridElementView = new GridElementView(activity);
            container.setVisibility(View.INVISIBLE); // hide view until content isn't loaded
            container.addView(gridElementView);
        } else if (gridElementView == null) {
            // when activity recreates we loose GridElementController but not view
            // So here we restore gridElementView from container
            gridElementView = (GridElementView) container.getChildAt(0);
        }
        gridElementView.setOnClickListener(this);
        gridElementView.setEventListener(this);

        gridElement.addCallback(this);

        FriendFactory.getFactoryInstance().addVideoStatusObserver(this);

        managerProvider.getPlayer().registerStatusCallbacks(this);

        updateVideoStatus();
    }

    /**
     * Updates view content whenever video status is changed.
     * It could be called from any thread.
     * @param friend friend
     */
    @Override
    public void onVideoStatusChanged(Friend friend) {
        if (isForMe(friend.getId())) {
            updateVideoStatus();
        }
    }

    @Override
    public void onNudgeClicked() {
        managerProvider.getInviteHelper().nudge(gridElement.getFriend());
    }

    @Override
    public void onRecordClicked() {
        DialogShower.showHintDialog(activity, activity.getString(R.string.dialog_record_title), activity.getString(R.string.dialog_record_message));
    }

    @Override
    public void onEmptyViewClicked() {
        managerProvider.getBenchViewManager().showBench();
    }

    @Override
    public void onThumbViewClicked() {
        // As it has thumb it must have friend, so play/stop video if there are any.
        // Otherwise show toast "Video is not playable"
        Friend friend = gridElement.getFriend();
        if (friend.hasIncomingPlayableVideos()) {
            managerProvider.getPlayer().togglePlayOverView(container, gridElement.getFriendId());
            managerProvider.getTutorial().onVideoStartPlayingByUser();
        } else {
            DialogShower.showToast(activity, R.string.video_is_not_playable);
        }
    }

    @Override
    public void onVideoPlaying(String friendId, String videoId) {
        if (isForMe(friendId)) {
            Log.d(TAG, "onVideoPlaying " + friendId);
            isVideoPlaying = true;
            updateContentFromUi(true);
        }
    }

    @Override
    public void onVideoStopPlaying(final String friendId) {
        if (isForMe(friendId)) {
            Log.d(TAG, "onVideoStopPlaying " + friendId);
            isVideoPlaying = false;
            updateContentFromUi(false);
        }
    }

    @Override
    public void onCompletion(String friendId) {
        if (isForMe(friendId)) {
            managerProvider.getTutorial().onVideoViewed(gridElementView);
        }
    }

    @Override
    public void onVideoPlaybackError(String friendId, String videoId) {
        if (isForMe(friendId)) {
            Friend friend = gridElement.getFriend();
            if (!friend.hasIncomingPlayableVideos()) {
                DialogShower.showToast(activity, R.string.video_is_not_playable);
            }
        }
    }

    private boolean isForMe(String friendId) {
        Friend friend = gridElement.getFriend();
        return friend != null && friendId != null && friendId.equals(friend.getId());
    }

    private void updateContentFromUi(final boolean hideIndicators) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                updateContent(hideIndicators);
            }
        });
    }

    /**
     * Update content. Should be called from UI thread
     *
     * @param animating hide indicators flag
     */
    private void updateContent(boolean animating) {
        Friend friend = gridElement.getFriend();
        if (friend == null) {
            container.setVisibility(View.VISIBLE); // as content is loaded, display view
            return;
        }
        gridElementView.showEmpty(false);
        int unreadMsgCount = friend.incomingVideoNotViewedCount();
        boolean lastEventOutgoing = friend.getLastEventType() == Friend.VideoStatusEventType.OUTGOING;

        boolean showNewMessages = unreadMsgCount > 0 && !animating && !isVideoPlaying;
        boolean showVideoViewed = friend.getOutgoingVideoStatus() == Friend.OutgoingVideoStatus.VIEWED
                && !showNewMessages && lastEventOutgoing;
        gridElementView.showNudge(!friend.hasApp());
        gridElementView.setVideoViewed(showVideoViewed);
        if (showVideoViewed) {
            managerProvider.getTutorial().onVideoViewedIndicatorShowed(gridElementView);
        }
        if (!animating) {
            gridElementView.showUploadingMark(isUploading() && !showNewMessages);
            gridElementView.showDownloadingMark(isDownloading() && !showNewMessages);
        }

        gridElementView.setUnreadCount(showNewMessages, unreadMsgCount);
        boolean thumbExists = friend.thumbExists();
        if (thumbExists) {
            gridElementView.setThumbnail(friend.thumbBitmap());
            gridElementView.showButtons(false);
        } else if (friend.hasIncomingPlayableVideos()) {
            // Normally should not happen. Only for case of the very first video whose thumb is broken
            gridElementView.setStubThumbnail();
            gridElementView.showButtons(false);
        } else {
            gridElementView.setThumbnail(null);
            gridElementView.showButtons(true);
        }

        gridElementView.setName(friend.getDisplayName());

        ((View) container.getParent()).invalidate();
        container.setVisibility(View.VISIBLE); // as content is loaded, display view
    }

    private void updateVideoStatus() {
        Friend friend = gridElement.getFriend();
        if (friend == null) {
            updateContent(false);
            return;
        }
        int lastEventType = friend.getLastEventType();

        int incomingStatus = friend.getIncomingVideoStatus();
        int outgoingStatus = friend.getOutgoingVideoStatus();

        Log.d(TAG, this + "| incomingStatus=" + incomingStatus + ", outgoingStatus=" + outgoingStatus);

        // if video is currently not played we update view content and show animation if needed
        if (!isVideoPlaying) {
            if (lastEventType == Friend.VideoStatusEventType.INCOMING) {
                updateUiForIncomingVideoStatus(incomingStatus);
            } else if (lastEventType == Friend.VideoStatusEventType.OUTGOING) {
                updateUiForOutgoingVideoStatus(outgoingStatus);
            }
        }
    }

    private void updateUiForIncomingVideoStatus(final int status) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                switch (status) {
                    case Video.IncomingVideoStatus.NEW:
                    case Video.IncomingVideoStatus.QUEUED:
                    case Video.IncomingVideoStatus.DOWNLOADING:
                        updateContent(false);
                        break;
                    case Video.IncomingVideoStatus.DOWNLOADED:
                        if (gridElementView.isReadyToAnimate()) {
                            // sound only if activity is really visible to user
                            if (!(NotificationAlertManager.screenIsLocked(activity) ||
                                    NotificationAlertManager.screenIsOff(activity))) {
                                if (!managerProvider.getRecorder().isRecording()) {
                                    NotificationAlertManager.playTone();
                                }
                            }
                            updateContent(true);
                            gridElementView.animateDownloading(new Runnable() {
                                @Override
                                public void run() {
                                    updateContent(false);
                                    managerProvider.getTutorial().onNewMessage(gridElementView);
                                }
                            });
                        } else {
                            updateContent(false);
                        }
                        break;
                    default:
                        updateContent(gridElementView.isAnimating());
                        break;
                }
            }
        });
    }

    private void updateUiForOutgoingVideoStatus(final int status) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                switch (status) {
                    case Friend.OutgoingVideoStatus.QUEUED:
                        updateContent(true);
                        managerProvider.getTutorial().onVideoRecorded();
                        gridElementView.animateUploading(new Runnable() {
                            @Override
                            public void run() {
                                updateContent(false);
                                if (gridElement.getFriend().incomingVideoNotViewedCount() == 0) {
                                    managerProvider.getTutorial().onVideoSentIndicatorShowed(gridElementView);
                                } else {
                                    managerProvider.getTutorial().onNewMessage(gridElementView);
                                }
                            }
                        });
                        break;
                    case Friend.OutgoingVideoStatus.VIEWED:
                        gridElementView.showUploadingMark(false);
                        updateContent(gridElementView.isAnimating());
                        break;
                    default:
                        updateContent(gridElementView.isAnimating());
                        break;
                }
            }
        });
    }

    private boolean isUploading() {
        Friend friend = gridElement.getFriend();
        boolean result = friend != null;
        if (result) {
            int lastEventType = friend.getLastEventType();
            int outgoingStatus = friend.getOutgoingVideoStatus();
            result = (outgoingStatus != Friend.OutgoingVideoStatus.NONE &&
                    outgoingStatus != Friend.OutgoingVideoStatus.VIEWED &&
                    outgoingStatus != Friend.OutgoingVideoStatus.FAILED_PERMANENTLY &&
                    lastEventType == Friend.VideoStatusEventType.OUTGOING);
        }
        return result;
    }

    private boolean isDownloading() {
        Friend friend = gridElement.getFriend();
        boolean result = friend != null;
        if (result) {
            int lastEventType = friend.getLastEventType();
            int incomingStatus = friend.getIncomingVideoStatus();
            result = lastEventType == Friend.VideoStatusEventType.INCOMING &&
                    (incomingStatus == Video.IncomingVideoStatus.DOWNLOADING ||
                            incomingStatus == Video.IncomingVideoStatus.NEW ||
                            incomingStatus == Video.IncomingVideoStatus.QUEUED);
        }
        return result;
    }

    @Override
    public void onAttached() {
        setUpView();
    }

    @Override
    public void onDetached() {
        cleanUp();
    }

    public void cleanUp() {
        managerProvider.getPlayer().unregisterStatusCallbacks(this);
        gridElement.removeCallback(this);
        FriendFactory.getFactoryInstance().removeOnVideoStatusChangedObserver(this);
    }

    @Override
    public void onModelUpdated(boolean changed) {
        if (changed) {
            updateContentFromUi(false);
            managerProvider.getBenchViewManager().updateBench();
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    managerProvider.getTutorial().onFriendModelChanged(gridElementView);
                }
            });
        }
        highLightElementForFriend();
    }

    //----------------------
    // Highlight grid change
    //----------------------
    private void highLightElementForFriend() {
        if (!gridElement.hasFriend()) {
            return;
        }

        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                final View v = container.findViewById(R.id.animation_view);
                v.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.grid_element_appear));
            }
        });

    }
}
