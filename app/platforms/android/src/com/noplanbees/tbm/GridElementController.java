package com.noplanbees.tbm;

import android.util.Log;

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
    private GridElement mGridElement;
    private final GridElementView mView;
    private final Callbacks mCallbacks;

    public interface Callbacks {
        void onBenchRequest(int pos);
        void onNudgeFriend(Friend f);
        void onRecordDialogRequested();
    }
    
    public GridElementController(GridElement gridElement, GridElementView view, GridElementController.Callbacks callbacks) {
    	Log.i(TAG, "instance with view " + view);
        mView = view;
        mGridElement = gridElement;
        mView.setOnClickListener(this);
        mCallbacks = callbacks;
        updateContent(false);
    }

    @Override
    public void onNudgeClicked() {
        mCallbacks.onNudgeFriend(mGridElement.friend());
    }

    @Override
    public void onRecordClicked() {
        mCallbacks.onRecordDialogRequested();
    }

    @Override
    public void onEmptyViewClicked() {
        mCallbacks.onBenchRequest(mView.getPosition());
    }

    @Override
    public void onVideoPlaying(String friendId, String videoId) {
        String id = mGridElement.friend().getId();
        Log.d(TAG, "onVideoPlaying " + id + " ? " + friendId);
        updateContent(id.equals(friendId));
    }

    @Override
    public void onVideoStopPlaying() {
        Log.d(TAG, "onVideoStopPlaying");
        updateContent(false);
    }

    @Override
    public void onFileDownloading() {    }

    @Override
    public void onFileDownloadingRetry() {   }

    private void updateContent(boolean hideIndicators) {
        Friend friend = mGridElement.friend();
        if (friend == null) {
            return;
        }
        int unreadMsgCount = friend.incomingVideoNotViewedCount();

        mView.showNudge(!friend.hasApp());

        mView.setVideoViewed(friend.getOutgoingVideoStatus() == Friend.OutgoingVideoStatus.VIEWED && !hideIndicators);
        mView.setUnreadCount(unreadMsgCount > 0 && !hideIndicators, unreadMsgCount);
        if (friend.thumbExists()) {
            mView.setThumbnail(friend.lastThumbBitmap());
            mView.showButtons(false);
        } else {
            mView.showButtons(true);
        }
        mView.showUploadingMark(isUploading());
        mView.setName(friend.getStatusString()); // TODO Remove
    }

    private void updateVideoStatus() {
        Friend friend = mGridElement.friend();
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
                updateContent(true);
                mView.animateDownloading();
                break;
            case Video.IncomingVideoStatus.DOWNLOADED:
                updateContent(false);
                mView.showProgressLine(false);
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
                updateContent(true);
                mView.animateUploading();
                break;
            case Friend.OutgoingVideoStatus.UPLOADED:
                mView.showProgressLine(false);
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
        return mGridElement.friend() != null;
    }

    private boolean isUploading() {
        Friend friend = mGridElement.friend();
        return friend != null && friend.getOutgoingVideoStatus() != Friend.OutgoingVideoStatus.NONE &&
                friend.getOutgoingVideoStatus() != Friend.OutgoingVideoStatus.VIEWED &&
                friend.getOutgoingVideoStatus() != Friend.OutgoingVideoStatus.QUEUED &&
                friend.getOutgoingVideoStatus() != Friend.OutgoingVideoStatus.UPLOADING &&
                friend.getIncomingVideoStatus() != Video.IncomingVideoStatus.DOWNLOADING;
    }

    @Override
    public void onAttached() {
        updateVideoStatus();

        VideoPlayer videoPlayer = VideoPlayer.getInstance(mView.getContext());
        videoPlayer.registerStatusCallbacks(this);
    }

    @Override
    public void onDetached() {
        VideoPlayer videoPlayer = VideoPlayer.getInstance(mView.getContext());
        videoPlayer.unregisterStatusCallbacks(this);
    }
}
