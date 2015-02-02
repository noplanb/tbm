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
        addGridElementView();
//        updateContent(false);
    }

    private void addGridElementView() {
	}

	@Override
    public void onNudgeClicked() {
        callbacks.onNudgeFriend(gridElement.getFriend());
    }

    @Override
    public void onRecordClicked() {
        callbacks.onRecordDialogRequested();
    }

    @Override
    public void onEmptyViewClicked() {
        callbacks.onBenchRequest();
    }

    @Override
    public void onVideoPlaying(String friendId, String videoId) {
        String id = gridElement.getFriend().getId();
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
        Friend friend = gridElement.getFriend();
        if (friend == null) {
            return;
        }
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
                updateContent(true);
                gridElementView.animateDownloading();
                break;
            case Video.IncomingVideoStatus.DOWNLOADED:
                updateContent(false);
                gridElementView.showProgressLine(false);
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
                gridElementView.animateUploading();
                break;
            case Friend.OutgoingVideoStatus.UPLOADED:
                gridElementView.showProgressLine(false);
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
        return friend != null && friend.getOutgoingVideoStatus() != Friend.OutgoingVideoStatus.NONE &&
                friend.getOutgoingVideoStatus() != Friend.OutgoingVideoStatus.VIEWED &&
                friend.getOutgoingVideoStatus() != Friend.OutgoingVideoStatus.QUEUED &&
                friend.getOutgoingVideoStatus() != Friend.OutgoingVideoStatus.UPLOADING &&
                friend.getIncomingVideoStatus() != Video.IncomingVideoStatus.DOWNLOADING;
    }

    @Override
    public void onAttached() {
        updateVideoStatus();

        VideoPlayer videoPlayer = VideoPlayer.getInstance(context);
        videoPlayer.registerStatusCallbacks(this);
    }

    @Override
    public void onDetached() {
        VideoPlayer videoPlayer = VideoPlayer.getInstance(context);
        videoPlayer.unregisterStatusCallbacks(this);
    }
}
