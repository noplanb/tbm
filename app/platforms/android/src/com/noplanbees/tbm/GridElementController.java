package com.noplanbees.tbm;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;
import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.GridElement;
import com.noplanbees.tbm.model.Video;
import com.noplanbees.tbm.multimedia.VideoPlayer;
import com.noplanbees.tbm.ui.view.GridElementView;

/**
 * Created by User on 1/30/2015.
 */
public class GridElementController implements GridElementView.ClickListener, VideoPlayer.StatusCallbacks, GridElementView.FriendViewListener {
    private static final String TAG = GridElementController.class.getSimpleName();
    private GridElement gridElement;
    private ViewGroup container;
    private GridElementView gridElementView;
    private final Callbacks callbacks;
    private Context context;
    private boolean isVideoPlaying = false;

    public interface Callbacks {
        void onBenchRequest();
        void onNudgeFriend(Friend f);
        void onRecordDialogRequested();
    }

    public GridElementController(Context context, GridElement gridElement, ViewGroup container, GridElementController.Callbacks callbacks) {
        Log.i(TAG, "instance with view " + container);
        this.context = context;
        this.container = container;
        this.gridElement = gridElement;
        this.callbacks = callbacks;
        setUpView();
    }

    private void setUpView() {
        if (container.getChildCount() == 0) {
            gridElementView = new GridElementView(context);
            container.addView(gridElementView);
        }
        gridElementView.setOnClickListener(this);
        gridElementView.setEventListener(this);

        VideoPlayer videoPlayer = VideoPlayer.getInstance(context);
        videoPlayer.registerStatusCallbacks(this);

        updateContent(false);
        updateVideoStatus();
    }

    /**
     * Updates view content whenever data is changed
     * @param friendId friend
     * @param forced flag means if view update should be forced
     */
    public void onDataUpdated(String friendId, boolean forced) {
        if (isForMe(friendId) || forced) {
//            updateContent(isVideoPlaying);
            updateVideoStatus();
        }
    }

    @Override
    public void onNudgeClicked() {
        callbacks.onNudgeFriend(gridElement.getFriend());
    }

    @Override
    public void onRecordClicked() {
        // TODO fix issue with click (short longpress) dispatched both in view and gestureRecognizer --Serhii
        callbacks.onRecordDialogRequested();
    }

    @Override
    public void onEmptyViewClicked() {
        callbacks.onBenchRequest();
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

    private void updateContent(boolean hideIndicators) {
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
            gridElementView.showButtons(true);
        }
        gridElementView.showUploadingMark(isUploading());
        gridElementView.showDownloadingMark(isDownloading());
        gridElementView.setName(friend.getStatusString()); // TODO Remove
    }

    private void updateVideoStatus() {
        Friend friend = gridElement.getFriend();
        if (friend == null) {
            return;
        }
        int incomingStatus = friend.getIncomingVideoStatus();
        int outgoingStatus = friend.getOutgoingVideoStatus();

        Log.d(TAG, this + "| incomingStatus="+incomingStatus+", outgoingStatus="+outgoingStatus);

        switch (incomingStatus) {
            case Video.IncomingVideoStatus.NEW:
                break;
            case Video.IncomingVideoStatus.QUEUED:
                break;
            case Video.IncomingVideoStatus.DOWNLOADING:
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
            case Video.IncomingVideoStatus.VIEWED:
                break;
            case Video.IncomingVideoStatus.FAILED_PERMANENTLY:
                break;
        }

        switch (outgoingStatus) {
            case Friend.OutgoingVideoStatus.NEW:
                break;
            case Friend.OutgoingVideoStatus.QUEUED:
                break;
            case Friend.OutgoingVideoStatus.UPLOADING:
                if (!isVideoPlaying) {
                    updateContent(true);
                    gridElementView.animateUploading();
                }
                break;
            case Friend.OutgoingVideoStatus.UPLOADED:
                updateContent(false);
                break;
            case Friend.OutgoingVideoStatus.DOWNLOADED:
                break;
            case Friend.OutgoingVideoStatus.VIEWED:
                break;
            case Friend.OutgoingVideoStatus.FAILED_PERMANENTLY:
                break;
        }
    }

    private boolean hasFriend() {
        return gridElement.getFriend() != null;
    }

    private boolean isUploading() {
        Friend friend = gridElement.getFriend();
        boolean result = friend != null;
        if (result) {
            int outgoingStatus = friend.getOutgoingVideoStatus();
            int incomingStatus = friend.getIncomingVideoStatus();
            result = outgoingStatus != Friend.OutgoingVideoStatus.NONE &&
                    outgoingStatus != Friend.OutgoingVideoStatus.VIEWED &&
                    outgoingStatus != Friend.OutgoingVideoStatus.QUEUED &&
                    outgoingStatus == Friend.OutgoingVideoStatus.UPLOADING &&
                    incomingStatus != Video.IncomingVideoStatus.DOWNLOADING;
        }
        return result;
    }

    private boolean isDownloading() {
        Friend friend = gridElement.getFriend();
        boolean result = friend != null;
        if (result) {
            int outgoingStatus = friend.getOutgoingVideoStatus();
            int incomingStatus = friend.getIncomingVideoStatus();
            result = incomingStatus == Video.IncomingVideoStatus.DOWNLOADING;
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
        VideoPlayer videoPlayer = VideoPlayer.getInstance(context);
        videoPlayer.unregisterStatusCallbacks(this);
    }
}
