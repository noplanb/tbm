package com.zazoapp.client.ui.helpers;

import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.zazoapp.client.R;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.model.ActiveModel;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.GridElement;
import com.zazoapp.client.model.IncomingVideo;
import com.zazoapp.client.model.OutgoingVideo;
import com.zazoapp.client.multimedia.VideoPlayer;
import com.zazoapp.client.notification.NotificationAlertManager;
import com.zazoapp.client.ui.ZazoManagerProvider;
import com.zazoapp.client.ui.animations.GridElementAnimation;
import com.zazoapp.client.ui.view.GridElementView;
import com.zazoapp.client.utilities.DialogShower;
import com.zazoapp.client.utilities.StringUtils;

/**
 * Created by User on 1/30/2015.
 */
public class GridElementController implements GridElementView.ClickListener, VideoPlayer.StatusCallbacks,
        GridElementView.FriendViewListener, ActiveModel.ModelChangeCallback, Friend.VideoStatusChangedCallback {

    private static final String TAG = GridElementController.class.getSimpleName();

    private GridElement gridElement;
    private ViewGroup container;
    private GridElementView gridElementView;
    private FragmentActivity activity;
    private ZazoManagerProvider managerProvider;
    private boolean isVideoPlaying = false;
    private boolean pendingAnim = false;

    private Handler uiHandler = new Handler(Looper.getMainLooper());

    public GridElementController(FragmentActivity activity, GridElement gridElement, ViewGroup container, ZazoManagerProvider managerProvider) {
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

        updateVideoStatus(false, true);
    }

    /**
     * Updates view content whenever video status is changed.
     * It could be called from any thread.
     * @param friend friend
     */
    @Override
    public void onVideoStatusChanged(Friend friend) {
        if (isForMe(friend.getId())) {
            updateVideoStatus(true, false);
        }
    }

    @Override
    public void onRecordClicked() {
        GridElementAnimation.holdToRec(gridElementView).start();
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
        // TODO FIXME TEST
        //if (friend != null) {
        //    IncomingVideo latest = friend.newestIncomingVideo();
        //    friend.setAndNotifyIncomingVideoStatus(latest.getId(),
        //            (latest.getVideoStatus() != IncomingVideo.Status.DOWNLOADED) ? IncomingVideo.Status.DOWNLOADED : IncomingVideo.Status.VIEWED);
        //}
        if (friend.hasIncomingPlayableVideos()) {
            managerProvider.getPlayer().togglePlayOverView(container, gridElement.getFriendId());
            managerProvider.getTutorial().onVideoStartPlayingByUser();
        } else if (friend.getIncomingNotViewedVideos().isEmpty()){
            GridElementAnimation.holdToRec(gridElementView).start();
        } else {
            DialogShower.showToast(activity, R.string.video_is_not_playable);
        }
    }

    @Override
    public void onResendClicked() {
        Friend friend = gridElement.getFriend();
        if (friend != null && friend.videoToFile(friend.getOutgoingVideoId()).exists()) {
            friend.requestUpload(friend.getOutgoingVideoId());
        }
    }

    @Override
    public void onVideoPlaying(String friendId, String videoId) {
        if (isForMe(friendId)) {
            Log.d(TAG, "onVideoPlaying " + friendId);
            isVideoPlaying = true;
            updateContentFromUi();
        }
    }

    @Override
    public void onVideoStopPlaying(final String friendId) {
        if (isForMe(friendId)) {
            Log.d(TAG, "onVideoStopPlaying " + friendId);
            isVideoPlaying = false;
            updateContentFromUi();
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
            DialogShower.showToast(activity, R.string.video_is_not_playable);
        }
    }

    private boolean isForMe(String friendId) {
        Friend friend = gridElement.getFriend();
        return friend != null && friendId != null && friendId.equals(friend.getId());
    }

    private void updateContentFromUi() {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                updateContent(false, false);
            }
        });
    }

    /**
     * Update content. Should be called from UI thread
     *
     * @param onlyLabel
     * @param force
     */
    private void updateContent(boolean onlyLabel, boolean force) {
        Friend friend = gridElement.getFriend();
        if (friend == null) {
            container.setVisibility(View.VISIBLE); // as content is loaded, display view
            gridElementView.showEmpty(true, gridElement.isNextEmpty());
            return;
        }
        gridElementView.showEmpty(false, false);
        int unreadMsgCount = friend.incomingVideoNotViewedCount();
        boolean lastEventOutgoing = friend.getLastEventType() == Friend.VideoStatusEventType.OUTGOING;

        boolean showNewMessages = unreadMsgCount > 0 && !isVideoPlaying;
        boolean showVideoViewed = friend.getOutgoingVideoStatus() == OutgoingVideo.Status.VIEWED
                && !showNewMessages && lastEventOutgoing;
        gridElementView.setVideoViewed(showVideoViewed);
        if (showVideoViewed) {
            managerProvider.getTutorial().onVideoViewedIndicatorShowed(gridElementView);
        }

        if (!onlyLabel || force) {
            gridElementView.showUploadingMark(isUploading() && !showNewMessages);
            gridElementView.setUnreadCount(showNewMessages, unreadMsgCount, false);
        } else if (!showNewMessages) {
            gridElementView.setUnreadCount(false, unreadMsgCount, false);
        }
        boolean thumbExists = friend.thumbExists();
        if (thumbExists) {
            gridElementView.setThumbnail(friend.thumbBitmap(), pendingAnim ? View.INVISIBLE : View.VISIBLE);
            gridElementView.showButtons(false);
        } else {
            gridElementView.setStubThumbnail(friend.getFullName(), pendingAnim ? View.INVISIBLE : View.VISIBLE);
            gridElementView.showButtons(true);
        }

        gridElementView.setName(friend.getDisplayName());
        String videoTimestamp = (lastEventOutgoing) ? friend.getOutgoingVideoId() : friend.newestIncomingVideo().getId();
        if (force || lastEventOutgoing || friend.getIncomingVideoStatus() == IncomingVideo.Status.DOWNLOADED) {
            gridElementView.setDate(StringUtils.getEventTime(videoTimestamp), !force);
        }

        gridElementView.showResendButton(DebugConfig.getInstance().isResendAllowed() && friend.videoToFile(friend.getOutgoingVideoId()).exists());
        ((View) container.getParent()).invalidate();
        container.setVisibility(View.VISIBLE); // as content is loaded, display view
    }

    private void animateUnreadCountChanging(Runnable endAction) {
        Friend friend = gridElement.getFriend();
        if (friend == null) {
            return;
        }
        int unreadMsgCount = friend.incomingVideoNotViewedCount();
        boolean showNewMessages = unreadMsgCount > 0 && !isVideoPlaying;
        gridElementView.setUnreadCountWithAnimation(showNewMessages, unreadMsgCount, endAction);
    }

    private void updateVideoStatus(boolean statusChanged, boolean force) {
        Friend friend = gridElement.getFriend();
        if (friend == null) {
            updateContent(false, force);
            return;
        }
        int lastEventType = friend.getLastEventType();

        int incomingStatus = friend.getIncomingVideoStatus();
        int outgoingStatus = friend.getOutgoingVideoStatus();

        Log.d(TAG, this + "| incomingStatus=" + incomingStatus + ", outgoingStatus=" + outgoingStatus);

        // if video is currently not played we update view content and show animation if needed
        if (!isVideoPlaying) {
            if (lastEventType == Friend.VideoStatusEventType.INCOMING) {
                updateUiForIncomingVideoStatus(incomingStatus, statusChanged, force);
            } else if (lastEventType == Friend.VideoStatusEventType.OUTGOING) {
                updateUiForOutgoingVideoStatus(outgoingStatus, statusChanged, force);
            }
        }
    }

    private void updateUiForIncomingVideoStatus(final int status, final boolean statusChanged, final boolean force) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                switch (status) {
                    case IncomingVideo.Status.DOWNLOADED:
                        if (gridElementView.isReadyToAnimate() && statusChanged) {
                            // sound only if activity is really visible to user
                            if (!(NotificationAlertManager.screenIsLocked(activity) ||
                                    NotificationAlertManager.screenIsOff(activity))) {
                                if (!managerProvider.getRecorder().isRecording() && !managerProvider.getPlayer().isPlaying()) {
                                    NotificationAlertManager.playTone(NotificationAlertManager.Tone.BEEP, 0.3f);
                                }
                            }
                            updateContent(true, force);
                            gridElementView.animateDownloading(new Runnable() {
                                @Override
                                public void run() {
                                    updateContent(true, force);
                                    animateUnreadCountChanging(new Runnable() {
                                        @Override
                                        public void run() {
                                            managerProvider.getTutorial().onNewMessage(gridElementView);
                                        }
                                    });
                                }
                            });
                        } else {
                            updateContent(false, force);
                        }
                        break;
                    default:
                        updateContent(true, force);
                        break;
                }
            }
        });
    }

    private void updateUiForOutgoingVideoStatus(final int status, final boolean statusChanged, final boolean force) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                switch (status) {
                    case OutgoingVideo.Status.QUEUED:
                        updateContent(true, force);
                        gridElementView.animateUploading(new Runnable() {
                            @Override
                            public void run() {
                                updateContent(false, force);
                                if (gridElement.getFriend().incomingVideoNotViewedCount() == 0) {
                                    managerProvider.getTutorial().onVideoSentIndicatorShowed(gridElementView);
                                } else {
                                    managerProvider.getTutorial().onNewMessage(gridElementView);
                                }
                                managerProvider.getTutorial().onMessageSent(gridElementView, gridElement.getFriend());
                            }
                        });
                        break;
                    case OutgoingVideo.Status.VIEWED:
                        gridElementView.showUploadingMark(false);
                        updateContent(false, force);
                        if (statusChanged && !force) {
                            gridElementView.animateViewed(new Runnable() {
                                @Override
                                public void run() {
                                    updateContent(false, force);
                                }
                            });
                        }
                        break;
                    default:
                        updateContent(true, force);
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
            result = (outgoingStatus != OutgoingVideo.Status.NONE &&
                    outgoingStatus != OutgoingVideo.Status.VIEWED &&
                    outgoingStatus != OutgoingVideo.Status.FAILED_PERMANENTLY &&
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
                    (incomingStatus == IncomingVideo.Status.DOWNLOADING ||
                            incomingStatus == IncomingVideo.Status.NEW ||
                            incomingStatus == IncomingVideo.Status.QUEUED);
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
        pendingAnim = changed;
        if (changed) {
            updateContentFromUi();
            managerProvider.getBenchViewManager().updateBench();
        }
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                managerProvider.getTutorial().onFriendModelChanged(
                        gridElementView,
                        gridElement.getFriend());
            }
        });
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
                GridElementAnimation.appearing(activity, gridElementView, true).start();
                pendingAnim = false;
            }
        });
    }
}
