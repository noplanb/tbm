package com.zazoapp.client;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import com.zazoapp.client.bench.BenchViewManager;
import com.zazoapp.client.bench.InviteManager;
import com.zazoapp.client.model.ActiveModel;
import com.zazoapp.client.model.ActiveModelsHandler;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.GridElement;
import com.zazoapp.client.model.Video;
import com.zazoapp.client.multimedia.VideoPlayer;
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
    private BenchViewManager benchViewManager;
    private Activity activity;
    private boolean isVideoPlaying = false;

    private Handler uiHandler = new Handler(Looper.getMainLooper());

    public GridElementController(Activity activity, GridElement gridElement, ViewGroup container, BenchViewManager benchViewManager) {
        Log.i(TAG, "instance with view " + container);
        this.activity = activity;
        this.container = container;
        this.gridElement = gridElement;
        this.benchViewManager = benchViewManager;
        setUpView();
    }

    private void setUpView() {
        if (container.getChildCount() == 0) {
            gridElementView = new GridElementView(activity);
            container.addView(gridElementView);
        }
        gridElementView.setOnClickListener(this);
        gridElementView.setEventListener(this);

        gridElement.addCallback(this);

        ActiveModelsHandler.getActiveModelsHandler().getFf().addVideoStatusObserver(this);

        VideoPlayer videoPlayer = VideoPlayer.getInstance();
        videoPlayer.registerStatusCallbacks(this);

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
        InviteManager.getInstance().nudge(gridElement.getFriend());
    }

    @Override
    public void onRecordClicked() {
        DialogShower.showInfoDialog(activity, activity.getString(R.string.dialog_record_title), activity.getString(R.string.dialog_record_message));
    }

    @Override
    public void onEmptyViewClicked() {
        benchViewManager.showBench();
    }

    @Override
    public void onThumbViewClicked() {
        // As it has thumb it must have friend, so play video
        VideoPlayer videoPlayer = VideoPlayer.getInstance();
        videoPlayer.togglePlayOverView(container, gridElement.getFriendId());
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
    public void onVideoStopPlaying(String friendId) {
        if (isForMe(friendId)) {
            Log.d(TAG, "onVideoStopPlaying " + friendId);
            isVideoPlaying = false;
            updateContentFromUi(false);
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
        if (!animating) {
            gridElementView.showUploadingMark(isUploading() && !showNewMessages);
            gridElementView.showDownloadingMark(isDownloading() && !showNewMessages);
        }

        gridElementView.setUnreadCount(showNewMessages, unreadMsgCount);
        if (friend.thumbExists()) {
            gridElementView.setThumbnail(friend.lastThumbBitmap());
            gridElementView.showButtons(false);
        } else {
            gridElementView.setThumbnail(null);
            gridElementView.showButtons(true);
        }

        gridElementView.setName(friend.getDisplayName());

        ((View) container.getParent()).invalidate();
    }

    private void updateVideoStatus() {
        Friend friend = gridElement.getFriend();
        if (friend == null) {
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
                        updateContent(true);
                        gridElementView.animateDownloading(new Runnable() {
                            @Override
                            public void run() {
                                updateContent(false);
                            }
                        });
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
                        gridElementView.animateUploading(new Runnable() {
                            @Override
                            public void run() {
                                updateContent(false);
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
        VideoPlayer videoPlayer = VideoPlayer.getInstance();
        videoPlayer.unregisterStatusCallbacks(this);
        gridElement.removeCallback(this);
        ActiveModelsHandler.getActiveModelsHandler().getFf().removeOnVideoStatusChangedObserver(this);
    }

    @Override
    public void onModelChanged() {
        updateContentFromUi(false);
        highLightElementForFriend();
        benchViewManager.updateBench();
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
