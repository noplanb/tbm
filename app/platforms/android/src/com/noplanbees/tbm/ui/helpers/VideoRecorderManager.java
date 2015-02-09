package com.noplanbees.tbm.ui.helpers;

import android.util.Log;
import android.view.ViewGroup;
import com.noplanbees.tbm.GridManager;
import com.noplanbees.tbm.dispatch.Dispatch;
import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.GridElement;
import com.noplanbees.tbm.model.GridElementFactory;
import com.noplanbees.tbm.multimedia.VideoRecorder;
import com.noplanbees.tbm.ui.GridViewFragment;
import com.noplanbees.tbm.ui.view.PreviewTextureFrame;

/**
 * Created by skamenkovych@codeminders.com on 2/9/2015.
 */
public class VideoRecorderManager implements VideoRecorder.VideoRecorderExceptionHandler {

    private static final String TAG = VideoRecorderManager.class.getSimpleName();

    private final VideoRecorder videoRecorder;
    private final GridViewFragment fragment;

    public VideoRecorderManager(GridViewFragment fragment) {
        this.fragment = fragment;
        videoRecorder = new VideoRecorder(fragment.getActivity());
        videoRecorder.addExceptionHandlerDelegate(this);
    }

    public void onRecordStart(String friendId) {
        GridElement ge = GridElementFactory.getFactoryInstance().getGridElementByFriendId(friendId);
        if (!ge.hasFriend())
            return;

        Friend f = ge.getFriend();
        GridManager.getInstance().rankingActionOccurred(f);
        if (videoRecorder.startRecording(f)) {
            Log.i(TAG, "onRecordStart: START RECORDING: " + f.get(Friend.Attributes.FIRST_NAME));
        } else {
            Dispatch.dispatch("onRecordStart: unable to start recording" + f.get(Friend.Attributes.FIRST_NAME));
        }
    }

    public void onRecordCancel() {
        // Different from abortAnyRecording because we always toast here.
        videoRecorder.stopRecording();
        fragment.showToast("Not sent.");
    }

    public void onRecordStop() {
        if (videoRecorder.stopRecording()) {
            Friend f = videoRecorder.getCurrentFriend();
            Log.i(TAG, "onRecordStop: STOP RECORDING. to " + f.get(Friend.Attributes.FIRST_NAME));
            f.setAndNotifyOutgoingVideoStatus(Friend.OutgoingVideoStatus.NEW);
            f.uploadVideo();
        }
    }

    // ---------------------------------------
    // Video Recorder ExceptionHandler delegate
    // ----------------------------------------
    @Override
    public void unableToSetPreview() {
        fragment.showToast("unable to set preview");
    }

    @Override
    public void unableToPrepareMediaRecorder() {
        fragment.showToast("Unable to prepare MediaRecorder");
    }

    @Override
    public void recordingAborted() {
        fragment.showToast("Recording Aborted due to Release before Stop.");
    }

    @Override
    public void recordingTooShort() {
        fragment.showToast("Not sent. Too short.");
    }

    @Override
    public void illegalStateOnStart() {
        fragment.showToast("Runtime exception on MediaRecorder.start. Quitting app.");
    }

    @Override
    public void runtimeErrorOnStart() {
        fragment.showToast("Unable to start recording. Try again.");
    }

    public void onResume() {
        videoRecorder.onResume();
    }

    public void onPause() {
        videoRecorder.onPause();
        videoRecorder.stopRecording();
    }

    public void addRecorderTo(ViewGroup container) {
        PreviewTextureFrame vrFrame = (PreviewTextureFrame) videoRecorder.getView();
        container.addView(vrFrame, new PreviewTextureFrame.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public void reconnect() {
        videoRecorder.dispose();
        videoRecorder.restore();
    }
}
