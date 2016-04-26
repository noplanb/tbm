package com.zazoapp.client.ui.helpers;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.zazoapp.client.R;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.GridElement;
import com.zazoapp.client.model.GridElementFactory;
import com.zazoapp.client.multimedia.CameraManager;
import com.zazoapp.client.multimedia.Recorder;
import com.zazoapp.client.multimedia.VideoRecorder;
import com.zazoapp.client.ui.BaseManagerProvider;
import com.zazoapp.client.ui.animations.CameraAnimation;
import com.zazoapp.client.ui.view.BasePreviewTextureFrame;
import com.zazoapp.client.ui.view.GridPreviewFrame;
import com.zazoapp.client.utilities.AsyncTaskManager;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.DialogShower;

import java.lang.ref.WeakReference;

/**
 * Created by skamenkovych@codeminders.com on 2/9/2015.
 */
public class VideoRecorderManager implements VideoRecorder.VideoRecorderExceptionHandler, Recorder, VideoRecorder.PreviewListener {

    private static final String TAG = VideoRecorderManager.class.getSimpleName();

    private final VideoRecorder videoRecorder;
    private final Context context;
    private final BaseManagerProvider managerProvider;
    private volatile boolean cameraSwitchAllowed = true;
    private WeakReference<View> containerRef;

    public VideoRecorderManager(Context context, BaseManagerProvider managerProvider) {
        this.context = context;
        videoRecorder = new VideoRecorder(context);
        videoRecorder.addExceptionHandlerDelegate(this);
        videoRecorder.setAudioController(managerProvider.getAudioController());
        this.managerProvider = managerProvider;
    }

    @Override
    public void start(String friendId) {
        GridElement ge = GridElementFactory.getFactoryInstance().findWithFriendId(friendId);
        if (!ge.hasFriend())
            return;
        if (!Convenience.checkAndNotifyNoSpace(context)) {
            return;
        }
        if (!cameraSwitchAllowed) {
            DialogShower.showToast(context, R.string.toast_camera_is_switching);
            return;
        }
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
        videoRecorder.stopRecording(true);
        DialogShower.showToast(context, R.string.toast_not_sent);
    }

    @Override
    public boolean stop() {
        return videoRecorder.stopRecording(false);
    }

    // ---------------------------------------
    // Video Recorder ExceptionHandler delegate
    // ----------------------------------------
    @Override
    public void unableToSetPreview() {
        DialogShower.showToast(context, R.string.toast_unable_to_set_preview);
        final View container = containerRef.get();
        if (container != null && videoRecorder.getView().getSwitchAnimationView().getAlpha() == 0) {
            CameraAnimation.animateIn(videoRecorder.getView().getSwitchAnimationView(), null);
        }
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
    public void pause(boolean release) {
        videoRecorder.onPause(release);
        if (release) {
            videoRecorder.stopRecording(false);
        }
    }

    @Override
    public void addPreviewTo(ViewGroup container, boolean inCard) {
        BasePreviewTextureFrame frame = videoRecorder.getView();
        if (frame == null) {
            if (inCard) {
                GridPreviewFrame vrFrame = new GridPreviewFrame(context);
                View blackBackground = new View(context);
                blackBackground.setBackgroundColor(Color.BLACK);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    CardView cardView = new CardView(context);
                    cardView.addView(blackBackground, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    cardView.addView(vrFrame, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    container.addView(cardView, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    container.setBackgroundResource(R.drawable.card_empty);
                } else {
                    container.addView(blackBackground, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    container.addView(vrFrame, new GridPreviewFrame.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    container.setBackgroundResource(R.drawable.card);
                    vrFrame.setOuterRecordingBorder(container);
                }
                frame = vrFrame;
            } else {
                frame = (BasePreviewTextureFrame) container;
            }
            videoRecorder.setView(frame);
        }
        containerRef = new WeakReference<View>(container);
    }

    @Override
    public void switchCamera() {
        if (cameraSwitchAllowed && !isRecording()) {
            cameraSwitchAllowed = false;
            AsyncTaskManager.executeAsyncTask(false, new AsyncTask<Void, Void, Void>() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    View switchCameraIcon = videoRecorder.getView().getCameraIconView();
                    switchCameraIcon.animate().setDuration(400).rotationYBy(180).start();
                }

                @Override
                protected Void doInBackground(Void... params) {
                    videoRecorder.release(false);
                    CameraManager.switchCamera(context);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    final View container = containerRef.get();
                    if (container != null) {
                        CameraAnimation.animateOut(videoRecorder.getView().getSwitchAnimationView(), new Runnable() {
                            @Override
                            public void run() {
                                videoRecorder.setPreviewListener(VideoRecorderManager.this);
                                videoRecorder.onResume();
                                cameraSwitchAllowed = true;
                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    public void indicateSwitchCameraFeature() {
        BasePreviewTextureFrame frame = videoRecorder.getView();
        frame.setSwitchCameraIndication();
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

    @Override
    public void onPreviewAvailable() {
        videoRecorder.setPreviewListener(null);
        final View container = containerRef.get();
        if (container != null) {
            CameraAnimation.animateIn(videoRecorder.getView().getSwitchAnimationView(), null);
        }
    }
}
