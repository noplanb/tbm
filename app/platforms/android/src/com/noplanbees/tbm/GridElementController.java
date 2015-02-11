package com.noplanbees.tbm;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import com.noplanbees.tbm.bench.BenchViewManager;
import com.noplanbees.tbm.bench.InviteManager;
import com.noplanbees.tbm.model.ActiveModel;
import com.noplanbees.tbm.model.ActiveModelsHandler;
import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.GridElement;
import com.noplanbees.tbm.model.Video;
import com.noplanbees.tbm.multimedia.VideoPlayer;
import com.noplanbees.tbm.ui.view.GridElementView;
import com.noplanbees.tbm.utilities.DialogShower;

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

        VideoPlayer videoPlayer = VideoPlayer.getInstance(activity);
        videoPlayer.registerStatusCallbacks(this);

        updateContent(false);
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
            // Serhii - I added updateContent here to cover the following case:
            // if we getFriends due to app coming to the foreground. And hasApp for the friend
            // this event is signalled by onVideoStatusChanged as well and we want this to be reflected in the ui.
            updateContent(false);
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
        VideoPlayer videoPlayer = VideoPlayer.getInstance(activity);
        videoPlayer.playOverView(container, gridElement.getFriendId());
    }

    @Override
    public void onVideoPlaying(String friendId, String videoId) {
        if (isForMe(friendId)) {
            Log.d(TAG, "onVideoPlaying " + friendId);
            isVideoPlaying = true;
            updateContent(true);
        }
    }

    @Override
    public void onVideoStopPlaying(String friendId) {
        if (isForMe(friendId)) {
            Log.d(TAG, "onVideoStopPlaying " + friendId);
            isVideoPlaying = false;
            updateContent(false);
        }
    }

    private boolean isForMe(String friendId) {
        Friend friend = gridElement.getFriend();
        return friend != null && friendId != null && friendId.equals(friend.getId());
    }

    @Override
    public void onFileDownloading() {    }

    @Override
    public void onFileDownloadingRetry() {   }

    private void updateContent(final boolean hideIndicators) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                Friend friend = gridElement.getFriend();
                if (friend == null) {
                    return;
                }
                gridElementView.showEmpty(false);
                int unreadMsgCount = friend.incomingVideoNotViewedCount();

                gridElementView.showNudge(!friend.hasApp());

                gridElementView.setVideoViewed(friend.getOutgoingVideoStatus() == Friend.OutgoingVideoStatus.VIEWED && !hideIndicators);
                gridElementView.setUnreadCount(unreadMsgCount > 0 && !hideIndicators, unreadMsgCount);
                if (friend.thumbExists()) {
                    gridElementView.setThumbnail(friend.lastThumbBitmap());
                    gridElementView.showButtons(false);
                } else {
                    gridElementView.setThumbnail(null);
                    gridElementView.showButtons(true);
                }
                gridElementView.showUploadingMark(isUploading());
                gridElementView.showDownloadingMark(isDownloading());
//                gridElementView.setName(gridElement.shouldUseAlterName() ? friend.getDisplayNameAlternative() : friend.getDisplayName());
                gridElementView.setName(friend.getStatusString()); // TODO Use line above for release
                ((View) container.getParent()).invalidate();
            }
        });
    }

    private void updateVideoStatus() {
        Friend friend = gridElement.getFriend();
        if (friend == null) {
            return;
        }
        int lastEventType = friend.getLastEventType();

        int incomingStatus = friend.getIncomingVideoStatus();
        int outgoingStatus = friend.getOutgoingVideoStatus();

        Log.d(TAG, this + "| incomingStatus="+incomingStatus+", outgoingStatus="+outgoingStatus);

        if (lastEventType == Friend.VideoStatusEventType.INCOMING) {
            updateUiForIncomingVideoStatus(incomingStatus);
        } else if (lastEventType == Friend.VideoStatusEventType.OUTGOING) {
            updateUiForOutgoingVideoStatus(outgoingStatus);
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
                	case Video.IncomingVideoStatus.FAILED_PERMANENTLY:
                        if (!isVideoPlaying) {
                            updateContent(true);
                        }
                        break;
                    case Video.IncomingVideoStatus.DOWNLOADED:
                        if (!isVideoPlaying) {
                            gridElementView.animateDownloading(new Runnable() {
                                @Override
                                public void run() {
                                    updateContent(false);
                                }
                            });
                        }
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
                        if (!isVideoPlaying) {
                            updateContent(true);
                            gridElementView.animateUploading();
                        }
                        break;
                    case Friend.OutgoingVideoStatus.NEW:
                    case Friend.OutgoingVideoStatus.UPLOADING:
                    case Friend.OutgoingVideoStatus.UPLOADED:
                    case Friend.OutgoingVideoStatus.DOWNLOADED:
                    case Friend.OutgoingVideoStatus.VIEWED:
                    case Friend.OutgoingVideoStatus.FAILED_PERMANENTLY:
                        updateContent(false);
                        break;
                }
            }
        });
    }

    private boolean isUploading() {
        Friend friend = gridElement.getFriend();
        boolean result = friend != null;
        if (result) {
            int outgoingStatus = friend.getOutgoingVideoStatus();
            int incomingStatus = friend.getIncomingVideoStatus();
            int lastEventType = friend.getLastEventType();
            result = lastEventType == Friend.VideoStatusEventType.OUTGOING &&
                    (outgoingStatus == Friend.OutgoingVideoStatus.NEW ||
                            outgoingStatus == Friend.OutgoingVideoStatus.UPLOADING);
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
                    incomingStatus == Video.IncomingVideoStatus.DOWNLOADING;
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
        VideoPlayer videoPlayer = VideoPlayer.getInstance(activity);
        videoPlayer.unregisterStatusCallbacks(this);
        gridElement.removeCallback(this);
        ActiveModelsHandler.getActiveModelsHandler().getFf().removeOnVideoStatusChangedObserver(this);
    }

    @Override
    public void onModelChanged() {
        updateContent(false);
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
        final View v = container.findViewById(R.id.animation_view);
        v.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.grid_element_appear));
    }
}
