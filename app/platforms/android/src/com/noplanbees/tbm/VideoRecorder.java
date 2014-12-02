package com.noplanbees.tbm;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import android.content.Context;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

import com.noplanbees.tbm.utilities.AsyncTaskManager;

public class VideoRecorder {

	// Even though I set up this exception handling interface to be complete I
	// probably wont use it the failure cause of failure
	// for all of these errors will not be something that the user can do
	// anything about. And the result of the failure will
	// be that the VideoRecorder wont work. I do need a general system in the
	// app though whereby we can report caught exceptions that
	// make the app unusable back to our servers for analysis.
	public interface VideoRecorderExceptionHandler {
		public void unableToSetPrievew();

		public void unableToPrepareMediaRecorder();

		public void recordingAborted();

		public void recordingTooShort();

		public void illegalStateOnStart();

		public void runntimeErrorOnStart();
	}

	private final String TAG = this.getClass().getSimpleName();

	// Allow registration of a single delegate to handle exceptions.
	private VideoRecorderExceptionHandler videoRecorderExceptionHandler;

	private Context context;
	private MediaRecorder mediaRecorder;

	private Friend currentFriend;

	private MediaPrepareTask preparationTask;
	private MediaRecordingTask recordingTask;
	
	//need to notify recording thread to wait until recorder is preparing
	private CountDownLatch syncObject;

	public VideoRecorder(Context context) {
		this.context = context;
	}

	public Friend getCurrentFriend() {
		return currentFriend;
	}

	public void addExceptionHandlerDelegate(VideoRecorderExceptionHandler handler) {
		videoRecorderExceptionHandler = handler;
	}

	public boolean stopRecording() {
		Log.i(TAG, "stopRecording");
		boolean rval = false;
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
				if (currentFriend != null)
					moveRecordingToFriend(currentFriend);
			} catch (IllegalStateException e) {
				Log.e(TAG, "stopRecording: IllegalStateException: ", e);
				rval = false;
			} catch (RuntimeException e) {
				Log.e(TAG, "stopRecording: Recording to short. No output file ", e);
				if (videoRecorderExceptionHandler != null) {
					videoRecorderExceptionHandler.recordingTooShort();
				}
				rval = false;
			}
			releaseMediaRecorder();
			prepare();
		}
		return rval;
	}

	public boolean startRecording(Friend f) {
		Log.i(TAG, "startRecording");

		this.currentFriend = f;

		if (recordingTask != null)
			recordingTask.cancel(true);

		recordingTask = new MediaRecordingTask();
		AsyncTaskManager.executeAsyncTask(recordingTask, new Void[] {});

		return true;
	}
	
	public void prepare(){
		if (preparationTask != null)
			preparationTask.cancel(true);

		syncObject = new CountDownLatch(1);
		preparationTask = new MediaPrepareTask();
		AsyncTaskManager.executeAsyncTask(preparationTask, new Void[] {});
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
		releaseMediaRecorder();
		if (abortedRecording && videoRecorderExceptionHandler != null)
			videoRecorderExceptionHandler.recordingAborted();
	}

	// ---------------
	// Private Methods
	// ---------------
	private void moveRecordingToFriend(Friend friend) {
		File ed = friend.videoToFile();
		File ing = Config.recordingFile(context);
		ing.renameTo(ed);
	}

	// ---------------------------------
	// Prepare and release MediaRecorder
	// ---------------------------------

	@SuppressWarnings("deprecation")
	private void prepareMediaRecorder() {
		if (mediaRecorder == null)
			mediaRecorder = new MediaRecorder();

		Camera camera = CameraManager.getPreparedCamera(context);
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
		prepare();
	}

	private void releaseMediaRecorder() {
		Log.i(TAG, "releaseMediaRecorder");
		if (mediaRecorder != null) {
			mediaRecorder.reset(); // clear recorder configuration
			mediaRecorder.release(); // release the recorder object
			mediaRecorder = null;
		}
	}

	/**
	 * Asynchronous task for preparing the {@link android.media.MediaRecorder}
	 * since it's a long blocking operation.
	 */
	private class MediaPrepareTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... voids) {
			Thread.currentThread().setName("MediaPrepareTask");
			long l = System.currentTimeMillis();
			prepareMediaRecorder();
			long l2 = System.currentTimeMillis();
			Log.d(TAG, "delta for preparation: " + (l2 - l));
			syncObject.countDown();
			return null;
		}
	}

	private class MediaRecordingTask extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Void... params) {
			Thread.currentThread().setName("MediaRecordingTask");
			try {
				Log.d(TAG, "waiting for preparation...");
				syncObject.await();
				Log.d(TAG, "preparation is complete. starting record...");
				try {
					mediaRecorder.start();
				} catch (IllegalStateException e) {
					Log.e(TAG, "startRecording: called in illegal state.", e);
					releaseMediaRecorder();
					if (videoRecorderExceptionHandler != null)
						videoRecorderExceptionHandler.illegalStateOnStart();
					return false;
				} catch (RuntimeException e) {
					// Since this seems to get the media recorder into a wedged
					// state I will just finish the app here.
					Log.e(TAG,
							"ERROR: RuntimeException: this should never happen according to google. But I have seen it. "
									+ e.toString());
					releaseMediaRecorder();
					if (videoRecorderExceptionHandler != null)
						videoRecorderExceptionHandler.runntimeErrorOnStart();
					return false;
				}
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return false;
			}
			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			//if able to launch record try to prepare
			if(!result)
				prepare();

		}
		
	}

}
