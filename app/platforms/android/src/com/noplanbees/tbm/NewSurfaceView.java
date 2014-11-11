package com.noplanbees.tbm;

import java.io.File;
import java.io.IOException;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.noplanbees.tbm.CameraManager.CameraExceptionHandler;
import com.noplanbees.tbm.VideoRecorder.VideoRecorderExceptionHandler;

public class NewSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

	private final String TAG = this.getClass().getSimpleName();

	private SurfaceHolder holder;
	private MediaRecorder mediaRecorder;
	private TextView previewText;
	private Context context;

	// Allow registration of a single delegate to handle exceptions.
	private static VideoRecorderExceptionHandler videoRecorderExceptionHandler;

	public static void addVideoRecorderExceptionHandlerDelegate(VideoRecorderExceptionHandler handler) {
		videoRecorderExceptionHandler = handler;
	}

	// Allow registration of a single delegate to handle exceptions.
	private static CameraExceptionHandler cameraExceptionHandler;

	public static void addCameraExceptionHandlerDelegate(CameraExceptionHandler handler) {
		cameraExceptionHandler = handler;
	}

	public NewSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		Log.i(TAG, "constructor");
		init(context);
	}

	public NewSurfaceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		Log.i(TAG, "constructor");
		init(context);
	}

	private void init(Context context) {
		this.context = context;
		holder = getHolder();
		holder.addCallback(this);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.i(TAG, "surfaceCreated");
		// camera preview
		{
			Camera camera = CameraManager.getCamera(context);
			if (camera == null)
				return;

			try {
				camera.setPreviewDisplay(holder);
			} catch (IOException e) {
				Log.e(TAG, "Error setting camera preview: " + e.getMessage());
				if (videoRecorderExceptionHandler != null)
					videoRecorderExceptionHandler.unableToSetPrievew();
				return;
			}

			camera.startPreview();
			prepareMediaRecorder();
		}

		// playback
		{
			this.holder = holder;
			this.holder.setFormat(PixelFormat.TRANSPARENT);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.i(TAG, "surfaceChanged");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i(TAG, "surfaceDestroyed");
	}

	// ---------------------------------
	// Prepare and release MediaRecorder
	// ---------------------------------

	private void prepareMediaRecorder() {
		if (mediaRecorder == null)
			mediaRecorder = new MediaRecorder();

		Camera camera = CameraManager.getCamera(context);
		if (camera == null) {
			Log.e(TAG, "prepareMediaRecorde: ERROR: No camera this should never happen!");
			return;
		}

		if (!CameraManager.unlockCamera()) {
			Log.e(TAG, "prepareMediaRecorde: ERROR: cant unlock camera this should never happen!");
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
																		// tinny
																		// but
																		// plays
																		// on
																		// ios
		mediaRecorder.setAudioChannels(2);
		mediaRecorder.setAudioEncodingBitRate(96000);
		mediaRecorder.setAudioSamplingRate(48000);

		mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		mediaRecorder.setVideoEncodingBitRate(150000);
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

		mediaRecorder.setOrientationHint(270);

		// Step 5: Set the preview output
		Log.i(TAG, "prepareMediaRecorder: mediaRecorder.setPreviewDisplay");
		mediaRecorder.setPreviewDisplay(this.holder.getSurface());

		// Step 6: Prepare configured MediaRecorder
		try {
			Log.i(TAG, "prepareMediaRecorder: mediaRecorder.prepare");
			mediaRecorder.prepare();
		} catch (IllegalStateException e) {
			Log.e(TAG,
					"ERROR: IllegalStateException preparing MediaRecorder: This should never happen" + e.getMessage());
			releaseMediaRecorder();
			notifyUnableToPrepare();
			return;
		} catch (IOException e) {
			Log.e(TAG, "ERROR: IOException preparing MediaRecorder: This should never happen" + e.getMessage());
			releaseMediaRecorder();
			notifyUnableToPrepare();
			return;
		}
		Log.i(TAG, "prepareMediaRecorder: Success");
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
		CameraManager.lockCamera();
	}

	public boolean stopRecording(Friend friend) {
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
				Log.i(TAG,
						String.format("Recorded file %s : %d", Config.recordingFilePath(context),
								Config.recordingFile(context).length()));
				if (friend != null)
					moveRecordingToFriend(friend);
			} catch (IllegalStateException e) {
				Log.e(TAG, "stopRecording: IllegalStateException: " + e.toString());
				rval = false;
				releaseMediaRecorder();
			} catch (RuntimeException e) {
				Log.e(TAG, "stopRecording: Recording to short. No output file " + e.toString());
				if (videoRecorderExceptionHandler != null) {
					videoRecorderExceptionHandler.recordingTooShort();
				}
				rval = false;
				releaseMediaRecorder();
			}
			prepareMediaRecorder();
		}
		return rval;
	}

	public boolean startRecording() {
		Log.i(TAG, "startRecording");

		if (mediaRecorder == null) {
			Log.e(TAG, "startRecording: ERROR no mediaRecorder this should never happen.");
			prepareMediaRecorder();
			return false;
		}

		try {
			mediaRecorder.start();
		} catch (IllegalStateException e) {
			Log.e(TAG, "startRecording: called in illegal state.");
			releaseMediaRecorder();
			if (videoRecorderExceptionHandler != null)
				videoRecorderExceptionHandler.illegalStateOnStart();
			return false;
		} catch (RuntimeException e) {
			// Since this seems to get the media recorder into a wedged state I
			// will just finish the app here.
			Log.e(TAG, "ERROR: RuntimeException: this should never happen according to google. But I have seen it. "
					+ e.toString());
			releaseMediaRecorder();
			if (videoRecorderExceptionHandler != null)
				videoRecorderExceptionHandler.runntimeErrorOnStart();
			return false;
		}
		showRecordingIndicator();
		return true;
	}

	public void release() {
		Log.i(TAG, "dispose");
		Boolean abortedRecording = false;
		if (mediaRecorder != null) {
			try {
				mediaRecorder.stop();
				abortedRecording = true;
			} catch (IllegalStateException e) {
			} catch (RuntimeException e) {
			}
		}
		CameraManager.releaseCamera();
		releaseMediaRecorder();
		if (abortedRecording && videoRecorderExceptionHandler != null)
			videoRecorderExceptionHandler.recordingAborted();

	}

	public void dispose() {
		// previewSurface.setVisibility(View.GONE);
		// overlaySurface.setVisibility(View.GONE);
	}

	public void restore() {
		Log.i(TAG, "restore");
		// overlaySurface.setVisibility(View.VISIBLE);
		// previewSurface.setVisibility(View.VISIBLE);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void hideRecordingIndicator() {
		Runnable hri = new Runnable() {
			@Override
			public void run() {
				Log.i(TAG, "hideRecordingIndicator");
				// Catch runntime exceptions here because I want to be able to
				// call this
				// and not worry that the surfaces may have already been
				// destroyed because the
				// user hid our app for example.
				try {
					previewText.setVisibility(View.INVISIBLE);
					Canvas c = holder.lockCanvas();
					c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
					holder.unlockCanvasAndPost(c);
					invalidate();
				} catch (RuntimeException e) {
					Log.e(TAG, "hideRecordingIndicator: ERROR. Perhaps the surfaces have been destroyed");
				}
			}
		};
		// activity.runOnUiThread(hri);
	}

	// ---------------
	// Private Methods
	// ---------------
	private void moveRecordingToFriend(Friend friend) {
		File ed = friend.videoToFile();
		File ing = Config.recordingFile(context);
		ing.renameTo(ed);
	}

	// -------------------
	// Recording indicator
	// -------------------
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void showRecordingIndicator() {
		Runnable sri = new Runnable() {
			@Override
			public void run() {
				Log.i(TAG, "showRecordingIndicator");
				previewText.setText("Recording...");
				previewText.setTextColor(Color.RED);
				previewText.setVisibility(View.VISIBLE);
				Canvas c = holder.lockCanvas();
				Path borderPath = new Path();
				borderPath.lineTo(c.getWidth(), 0);
				borderPath.lineTo(c.getWidth(), c.getHeight());
				borderPath.lineTo(0, c.getHeight());
				borderPath.lineTo(0, 0);
				Paint paint = new Paint();
				paint.setColor(0xffCC171E);
				paint.setStrokeWidth(Convenience.dpToPx(context, 2));
				paint.setStyle(Paint.Style.STROKE);
				Paint cpaint = new Paint();
				cpaint.setColor(0xffCC171E);
				cpaint.setStyle(Paint.Style.FILL);
				c.drawPath(borderPath, paint);
				c.drawCircle(Convenience.dpToPx(context, 13), Convenience.dpToPx(context, 13),
						Convenience.dpToPx(context, 4), cpaint);
				holder.unlockCanvasAndPost(c);
			}
		};
		// activity.runOnUiThread(sri);
	}

}
