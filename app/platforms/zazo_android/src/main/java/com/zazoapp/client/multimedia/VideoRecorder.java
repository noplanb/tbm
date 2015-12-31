package com.zazoapp.client.multimedia;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import com.zazoapp.client.Config;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.ui.CancelableTask;
import com.zazoapp.client.ui.view.PreviewTextureFrame;
import com.zazoapp.client.utilities.Logger;

import java.io.File;
import java.io.IOException;

public class VideoRecorder implements SurfaceTextureListener {

    private static final String TAG = VideoRecorder.class.getSimpleName();

    // Even though I set up this exception handling interface to be complete I
    // probably wont use it the failure cause of failure
    // for all of these errors will not be something that the user can do
    // anything about. And the result of the failure will
    // be that the VideoRecorder wont work. I do need a general system in the
    // app though whereby we can report caught exceptions that
    // make the app unusable back to our servers for analysis.
    public interface VideoRecorderExceptionHandler {
        void unableToSetPreview();

        void unableToPrepareMediaRecorder();

        void recordingAborted();

        void recordingTooShort();

        void illegalStateOnStart();

        void runtimeErrorOnStart();
    }

    private SurfaceTexture holder;
    private boolean isPreviewing = false;
    private boolean isSurfaceAvailable = false;
    private Context context;
    private MediaRecorder mediaRecorder;
    private Friend currentFriend;
    private PreviewTextureFrame preview;
    private AudioController audioController;

    // Allow registration of a single delegate to handle exceptions.
    private VideoRecorderExceptionHandler videoRecorderExceptionHandler;

    private CancelableTask turnOnExclusiveAudioMode;

    public VideoRecorder(Context c) {
        context = c;
    }

    public void onResume() {
        startPreview(holder);
    }

    public void onPause(boolean release) {
        if (release) {
            release(true);
        } else {
            stopPreview();
        }
    }

    public Friend getCurrentFriend() {
        return currentFriend;
    }

    public void addExceptionHandlerDelegate(VideoRecorderExceptionHandler handler) {
        videoRecorderExceptionHandler = handler;
    }

    public boolean startRecording(Friend f) {
        Log.i(TAG, "startRecording");

        currentFriend = f;
        if (mediaRecorder == null) {
            prepareMediaRecorder();
        }
        try {
            mediaRecorder.start();
        } catch (IllegalStateException e) {
            Dispatch.dispatch("startRecording: called in illegal state.");
            releaseMediaRecorder();
            if (videoRecorderExceptionHandler != null)
                videoRecorderExceptionHandler.illegalStateOnStart();
            return false;
        } catch (RuntimeException e) {
            // Since this seems to get the media recorder into a wedged state I
            // will just finish the app here.
            CameraManager.releaseCamera();
            Dispatch.dispatch("ERROR: RuntimeException: this should never happen according to google. But I have seen it. "
                    + e.toString());
            releaseMediaRecorder();
            if (videoRecorderExceptionHandler != null)
                videoRecorderExceptionHandler.runtimeErrorOnStart();
            return false;
        }
        showRecordingIndicator();
        return true;
    }

    public boolean stopRecording() {
        Log.i(TAG, "stopRecording");
        boolean rval = false;
        hideRecordingIndicator(); // It should be safe to call this even if the
        // sufraces have already been destroyed.
        if (mediaRecorder != null) {
            // hideRecordingIndicator is in the if statement because if
            // VideoRecorder was disposed due to an external event such as a
            // phone call while the user was still pressing record when he
            // releases his finger
            // we will get a stopRecording even though our app has been paused.
            // If we try to hideRecordingIndicator at this point the surface
            // will have already been disposed of and app will crash.
            try {
                mediaRecorder.stop();
                rval = true;
                Logger.i(TAG,
                        String.format("Recorded file %s : %d", Config.recordingFilePath(context),
                                Config.recordingFile(context).length()));
                if (currentFriend != null)
                    moveRecordingToFriend(currentFriend);
            } catch (IllegalStateException e) {
                Dispatch.dispatch("stopRecording: IllegalStateException: " + e.toString());

                rval = false;
            } catch (RuntimeException e) {
                Log.d(TAG, "stopRecording: Recording to short. No output file " + e.toString());
                if (videoRecorderExceptionHandler != null) {
                    videoRecorderExceptionHandler.recordingTooShort();
                }
                rval = false;
            } finally {
                releaseMediaRecorder();
            }
        }
        return rval;
    }

    public void release(boolean releaseCamera) {
        Log.i(TAG, "release");
        isPreviewing = false;
        Boolean abortedRecording = false;
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                abortedRecording = true;
            } catch (IllegalStateException e) {
            } catch (RuntimeException e) {
            }
        }
        releaseMediaRecorder();
        if (releaseCamera) {
            CameraManager.releaseCamera();
        }
        if (abortedRecording && videoRecorderExceptionHandler != null)
            videoRecorderExceptionHandler.recordingAborted();
    }

    public void dispose() {
        Log.i(TAG, "dispose");
        preview.setVisibility(View.GONE);
    }

    public void restore() {
        Log.i(TAG, "restore");
        preview.setVisibility(View.VISIBLE);
    }

    // ---------------
    // Private Methods
    // ---------------
    private void moveRecordingToFriend(Friend friend) {
        String videoId = VideoIdUtils.generateId();
        File ed = friend.videoToFile(videoId);
        File ing = Config.recordingFile(context);
        ing.renameTo(ed);
        friend.setNewOutgoingVideoId(videoId);
    }

    // ---------------------------------

    private void prepareMediaRecorder() {
        if (mediaRecorder == null)
            mediaRecorder = new MediaRecorder();

        Camera camera = CameraManager.getCamera(context);
        if (camera == null) {
            Dispatch.dispatch("prepareMediaRecorde: ERROR: No camera this should never happen!");
            return;
        }

        if (!CameraManager.unlockCamera()) {
            Dispatch.dispatch("prepareMediaRecorde: ERROR: cant unlock camera this should never happen!");
            return;
        }

        mediaRecorder.setCamera(camera);

        // Set sources
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        // mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_QVGA));

        // Set format and encoder see tbm-ios/docs/video_recorder.txt for the
        // research that lead to these settings for compatability with IOS.
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); // Very
        // tinny but plays on ios
        mediaRecorder.setAudioChannels(2);
        mediaRecorder.setAudioEncodingBitRate(96000);
        mediaRecorder.setAudioSamplingRate(48000);

        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setVideoEncodingBitRate(150000);

        if (CameraManager.is15FramesAvailable())
            mediaRecorder.setVideoFrameRate(15);

        Camera.Size size = CameraManager.getPreviewSize();
        if (size == null) {
            notifyUnableToPrepare();
            return;
        }
        mediaRecorder.setVideoSize(size.width, size.height);

        String ofile = Config.recordingFilePath(context);
        Log.i(TAG, "prepareMediaRecorder: mediaRecorder outfile: " + ofile);
        mediaRecorder.setOutputFile(ofile);

        mediaRecorder.setOrientationHint(CameraManager.getOrientation());

        // Step 6: Prepare configured MediaRecorder
        try {
            Log.i(TAG, "prepareMediaRecorder: mediaRecorder.prepare");
            mediaRecorder.prepare();
        } catch (IllegalStateException | IOException e) {
            Dispatch.dispatch(TAG + " ERROR: " + e.getClass().getSimpleName() + " preparing MediaRecorder: This should never happen" + e.getMessage());
            releaseMediaRecorder();
            notifyUnableToPrepare();
            return;
        }
        Log.i(TAG, "prepareMediaRecorder: Success");
        setExclusiveAudioMode(true, true);
    }

    private void notifyUnableToPrepare() {
        if (videoRecorderExceptionHandler != null)
            videoRecorderExceptionHandler.unableToPrepareMediaRecorder();
    }

    private void releaseMediaRecorder() {
        Log.i(TAG, "releaseMediaRecorder");
        if (mediaRecorder != null) {
            mediaRecorder.reset(); // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
        }
        setExclusiveAudioMode(false, false);
    }

    // -------------------
    // Recording indicator
    // -------------------
    private void showRecordingIndicator() {
        preview.setRecording(true);
    }

    private void hideRecordingIndicator() {
        if (preview != null) {
            preview.setRecording(false);
        }
    }

    public View getView() {
        if (preview == null) {
            preview = new PreviewTextureFrame(context);
            preview.setSurfaceTextureListener(this);
        }
        return preview;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture holder, int width, int height) {
        Log.i(TAG, "cameraPreviewSurfaceCreated " + width +"x" + height);
        isSurfaceAvailable = true;
        startPreview(holder);
    }

    private void adjustPreviewRatio() {
        Matrix matrix = new Matrix();
        Camera camera = CameraManager.getCamera(context);
        if (camera == null)
            return;
        Camera.Size realSize = camera.getParameters().getPreviewSize();
        float realAspect = realSize.width / (float) realSize.height;
        float viewAspect = preview.getPreviewHeight() / (float) preview.getPreviewWidth();
        float scaleX, scaleY;
        if (realAspect > viewAspect) {
            scaleX = 1f;
            scaleY = realAspect / viewAspect;
        } else {
            scaleX = viewAspect / realAspect;
            scaleY = 1f;
        }
        matrix.setScale(scaleX, scaleY, preview.getWidth() / 2, preview.getHeight() / 2);
        preview.setTransformMatrix(matrix);
    }

    private void startPreview(SurfaceTexture holder) {
        if (!isPreviewing && isSurfaceAvailable) {
            Log.i(TAG, "startPreview: starting preview");

            this.holder = holder;
            Camera camera = CameraManager.getCamera(context);
            if (camera == null){
                Log.w(TAG, "startPreview: could camera==nul");
                return;
            }
            adjustPreviewRatio();
            try {
                camera.setPreviewTexture(holder);
            } catch (IOException e) {
                Dispatch.dispatch("Error setting camera preview: " + e.getLocalizedMessage());
                if (videoRecorderExceptionHandler != null)
                    videoRecorderExceptionHandler.unableToSetPreview();
                return;
            }

            try {
                camera.startPreview();
            } catch (RuntimeException e){
                Dispatch.dispatch("Error camera.startPreview: " + e.getLocalizedMessage());
                if (videoRecorderExceptionHandler != null)
                    videoRecorderExceptionHandler.unableToSetPreview();
                return;
            }

            isPreviewing = true;
        } else {
            Log.w(TAG, "startPreview: Not starting preview: holder=" + holder + " isPreviewing=" + isPreviewing+ " isSurfaceAvailable=" + isSurfaceAvailable);
        }
    }

    private void stopPreview() {
        if (isPreviewing) {
            Camera camera = CameraManager.getCamera(context);
            if (camera != null) {
                try {
                    camera.stopPreview();
                } catch (RuntimeException e) {
                    Log.w(TAG, "Couldn't stop preview");
                }
            }
            isPreviewing = false;
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.i(TAG, "onSurfaceTextureDestroyed ");
        isSurfaceAvailable = false;
        isPreviewing = false;
        CameraManager.releaseCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        adjustPreviewRatio();
    }


    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public boolean isRecording() {
        return preview != null && preview.isRecording();
    }

    /**
     *
     * @param mode true to mute system sounds during recording
     * @param delayed true to make 1 s delay before applying exclusive mode
     */
    private void setExclusiveAudioMode(final boolean mode, boolean delayed) {
        if (turnOnExclusiveAudioMode != null) {
            turnOnExclusiveAudioMode.cancel();
        }
        turnOnExclusiveAudioMode = new CancelableTask() {
            @Override
            protected void doTask() {
                if (audioController != null) {
                    audioController.setExclusive(mode);
                }
            }
        };
        if (delayed && preview != null) {
            preview.postDelayed(turnOnExclusiveAudioMode, 1000);
        } else {
            turnOnExclusiveAudioMode.run();
        }
    }

    public void setAudioController(AudioController ac) {
        audioController = ac;
    }
}