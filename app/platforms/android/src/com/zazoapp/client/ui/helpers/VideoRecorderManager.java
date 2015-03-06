package com.zazoapp.client.ui.helpers;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;
import com.zazoapp.client.GridManager;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.GridElement;
import com.zazoapp.client.model.GridElementFactory;
import com.zazoapp.client.multimedia.VideoRecorder;
import com.zazoapp.client.ui.view.PreviewTextureFrame;
import com.zazoapp.client.utilities.DialogShower;

/**
 * Created by skamenkovych@codeminders.com on 2/9/2015.
 */
public class VideoRecorderManager implements VideoRecorder.VideoRecorderExceptionHandler {

    private static final String TAG = VideoRecorderManager.class.getSimpleName();

    private final VideoRecorder videoRecorder;
    private final Context context;

    public VideoRecorderManager(Context context) {
        this.context = context;
        videoRecorder = new VideoRecorder(context);
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
        DialogShower.showToast(context, "Not sent.");
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
        DialogShower.showToast(context, "unable to set preview");
    }

    @Override
    public void unableToPrepareMediaRecorder() {
        DialogShower.showToast(context, "Unable to prepare MediaRecorder");
    }

    @Override
    public void recordingAborted() {
        DialogShower.showToast(context, "Recording Aborted due to Release before Stop.");
    }

    @Override
    public void recordingTooShort() {
        DialogShower.showToast(context, "Too short.");
    }

    @Override
    public void illegalStateOnStart() {
        DialogShower.showToast(context, "Runtime exception on MediaRecorder.start. Quitting app.");
    }

    @Override
    public void runtimeErrorOnStart() {
        DialogShower.showToast(context, "Unable to start recording. Try again.");
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
