package com.zazoapp.client.ui.helpers;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;
import com.zazoapp.client.R;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.GridElement;
import com.zazoapp.client.model.GridElementFactory;
import com.zazoapp.client.model.OutgoingVideo;
import com.zazoapp.client.multimedia.CameraManager;
import com.zazoapp.client.multimedia.Recorder;
import com.zazoapp.client.multimedia.VideoRecorder;
import com.zazoapp.client.ui.ZazoManagerProvider;
import com.zazoapp.client.ui.view.PreviewTextureFrame;
import com.zazoapp.client.utilities.DialogShower;

/**
 * Created by skamenkovych@codeminders.com on 2/9/2015.
 */
public class VideoRecorderManager implements VideoRecorder.VideoRecorderExceptionHandler, Recorder {

    private static final String TAG = VideoRecorderManager.class.getSimpleName();

    private final VideoRecorder videoRecorder;
    private final Context context;
    private final ZazoManagerProvider managerProvider;

    public VideoRecorderManager(Context context, ZazoManagerProvider managerProvider) {
        this.context = context;
        videoRecorder = new VideoRecorder(context);
        videoRecorder.addExceptionHandlerDelegate(this);
        this.managerProvider = managerProvider;
    }

    @Override
    public void start(String friendId) {
        GridElement ge = GridElementFactory.getFactoryInstance().findWithFriendId(friendId);
        if (!ge.hasFriend())
            return;

        Friend f = ge.getFriend();
        f.setLastActionTime();
        if (videoRecorder.startRecording(f)) {
            Log.i(TAG, "onRecordStart: START RECORDING: " + f.get(Friend.Attributes.FIRST_NAME));
        } else {
            Dispatch.dispatch("onRecordStart: unable to start recording" + f.get(Friend.Attributes.FIRST_NAME));
        }
    }

    @Override
    public void cancel() {
        // Different from abortAnyRecording because we always toast here.
        videoRecorder.stopRecording();
        DialogShower.showToast(context, R.string.toast_not_sent);
    }

    @Override
    public void stop() {
        if (videoRecorder.stopRecording()) {
            Friend f = videoRecorder.getCurrentFriend();
            Log.i(TAG, "onRecordStop: STOP RECORDING. to " + f.get(Friend.Attributes.FIRST_NAME));
            f.setAndNotifyOutgoingVideoStatus(f.getOutgoingVideoId(), OutgoingVideo.Status.NEW);
            f.uploadVideo(f.getOutgoingVideoId());
        }
    }

    // ---------------------------------------
    // Video Recorder ExceptionHandler delegate
    // ----------------------------------------
    @Override
    public void unableToSetPreview() {
        DialogShower.showToast(context, R.string.toast_unable_to_set_preview);
    }

    @Override
    public void unableToPrepareMediaRecorder() {
        DialogShower.showToast(context, R.string.toast_unable_to_prepare_media_recorder);
    }

    @Override
    public void recordingAborted() {
        DialogShower.showToast(context, R.string.toast_recording_aborted_wrong_state);
    }

    @Override
    public void recordingTooShort() {
        DialogShower.showToast(context, R.string.toast_too_short);
    }

    @Override
    public void illegalStateOnStart() {
        DialogShower.showToast(context, R.string.toast_illegal_media_recorder_state);
    }

    @Override
    public void runtimeErrorOnStart() {
        DialogShower.showToast(context, R.string.toast_unable_to_start_recording);
    }

    @Override
    public void resume() {
        videoRecorder.onResume();
    }

    @Override
    public void pause() {
        videoRecorder.onPause();
        videoRecorder.stopRecording();
    }

    @Override
    public void addPreviewTo(ViewGroup container) {
        PreviewTextureFrame vrFrame = (PreviewTextureFrame) videoRecorder.getView();
        container.addView(vrFrame, new PreviewTextureFrame.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void switchCamera() {
        videoRecorder.release(false);
        CameraManager.switchCamera(context);
        videoRecorder.onResume();
    }

    @Override
    public void reconnect() {
        videoRecorder.dispose();
        videoRecorder.restore();
    }

    @Override
    public boolean isRecording() {
        return videoRecorder.isRecording();
    }
}
