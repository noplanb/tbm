package com.noplanbees.tbm;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;

import com.noplanbees.tbm.ui.PreviewTextureView;


public class VideoRecorder implements SurfaceTextureListener {

	// Even though I set up this exception handling interface to be complete I probably wont use it the failure cause of failure
	// for all of these errors will not be something that the user can do anything about. And the result of the failure will 
	// be that the VideoRecorder wont work. I do need a general system in the app though whereby we can report caught exceptions that 
	// make the app unusable back to our servers for analysis.
	public interface VideoRecorderExceptionHandler{
		public void unableToSetPrievew();
		public void unableToPrepareMediaRecorder();
		public void recordingAborted();
		public void recordingTooShort();
		public void illegalStateOnStart();
		public void runntimeErrorOnStart();
	}

	private final String TAG = this.getClass().getSimpleName();

	private Context context;
	private MediaRecorder mediaRecorder;

	private Friend currentFriend;
	
	private PreviewTextureView preview;
	private boolean isRecording;


	public VideoRecorder(Context c) {
		context = c;
	}

	public Friend getCurrentFriend() {
		return currentFriend;
	}

	// --------------
	// Public Methods
	// --------------
	
	// Allow registration of a single delegate to handle exceptions.
	private VideoRecorderExceptionHandler videoRecorderExceptionHandler;
	public void addExceptionHandlerDelegate(VideoRecorderExceptionHandler handler){
		videoRecorderExceptionHandler = handler;
	}
	
	public boolean stopRecording() {
		Log.i(TAG, "stopRecording");
		boolean rval = false;
		hideRecordingIndicator(); // It should be safe to call this even if the sufraces have already been destroyed.
		if (mediaRecorder !=null){
			// hideRecordingIndicator is in the if statement because if VideoRecorder was disposed due to an external event such as a 
			// phone call while the user was still pressing record when he releases his finger
			// we will get a stopRecording even though our app has been paused. If we try to hideRecordingIndicator at this point the surface
			// will have already been disposed of and app will crash. 
			try {
				mediaRecorder.stop();
				rval = true;
				Log.i(TAG, String.format("Recorded file %s : %d",Config.recordingFilePath(context), Config.recordingFile(context).length()));
				if (currentFriend != null)
					moveRecordingToFriend(currentFriend);
			} catch (IllegalStateException e) {
				Log.e(TAG, "stopRecording: IllegalStateException: " + e.toString());
 				rval = false;
				releaseMediaRecorder();
			} catch (RuntimeException e) {
				Log.e(TAG, "stopRecording: Recording to short. No output file " + e.toString());
				if (videoRecorderExceptionHandler != null){
					videoRecorderExceptionHandler.recordingTooShort();
				}
				rval = false;
				releaseMediaRecorder();
			}
			prepareMediaRecorder();
		}
		return rval;
	}

	public boolean startRecording(Friend f) {
		Log.i(TAG, "startRecording");
		
		this.currentFriend = f;

		if (mediaRecorder == null){
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
		} catch (RuntimeException e){
			// Since this seems to get the media recorder into a wedged state I will just finish the app here.
			Log.e(TAG, "ERROR: RuntimeException: this should never happen according to google. But I have seen it. " + e.toString());
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
		if (mediaRecorder !=null){
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
	
//	public void dispose(){
//		previewSurface.setVisibility(View.GONE);
//		overlaySurface.setVisibility(View.GONE);
//	}

//	public void restore(){
//		Log.i(TAG, "restore");
//		overlaySurface.setVisibility(View.VISIBLE);
//		previewSurface.setVisibility(View.VISIBLE);
//	}
	
	
	// ---------------
	// Private Methods
	// ---------------
	private void moveRecordingToFriend(Friend friend){
		File ed = friend.videoToFile();
		File ing = Config.recordingFile(context);
		ing.renameTo(ed);
	}
	
	// ---------------------------------
	// Prepare and release MediaRecorder
	// ---------------------------------
	
	private void prepareMediaRecorder() {
		if (mediaRecorder == null)
			mediaRecorder = new MediaRecorder();
		
		Camera camera = CameraManager.getCamera(context);
		if (camera == null){
			Log.e(TAG, "prepareMediaRecorde: ERROR: No camera this should never happen!");
			return;
		}
		
		if (!CameraManager.unlockCamera()){
			Log.e(TAG, "prepareMediaRecorde: ERROR: cant unlock camera this should never happen!");
			return;
		}
		
		mediaRecorder.setCamera(camera);

		// Set sources
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		// mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_QVGA));

		// Set format and encoder see tbm-ios/docs/video_recorder.txt for the research that lead to these settings for compatability with IOS.
		mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); // Very tinny but plays on ios
		mediaRecorder.setAudioChannels(2);
		mediaRecorder.setAudioEncodingBitRate(96000);
		mediaRecorder.setAudioSamplingRate(48000);

		mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		mediaRecorder.setVideoEncodingBitRate(150000);
		mediaRecorder.setVideoFrameRate(15);
		
		Camera.Size size = CameraManager.getPreviewSize();
		if (size == null){
			notifyUnableToPrepare();
			return;
		}
		mediaRecorder.setVideoSize(size.width, size.height);

		String ofile = Config.recordingFilePath(context);
		Log.i(TAG, "prepareMediaRecorder: mediaRecorder outfile: " + ofile);
		mediaRecorder.setOutputFile(ofile);
		
		mediaRecorder.setOrientationHint(270);
		
		// Step 5: Set the preview output
		//Log.i(TAG, "prepareMediaRecorder: mediaRecorder.setPreviewDisplay");
		//mediaRecorder.setPreviewDisplay(previewSurfaceHolder.getSurface());

		// Step 6: Prepare configured MediaRecorder
		try {
			Log.i(TAG, "prepareMediaRecorder: mediaRecorder.prepare");
			mediaRecorder.prepare();
		} catch (IllegalStateException e) {
			Log.e(TAG,"ERROR: IllegalStateException preparing MediaRecorder: This should never happen" + e.getMessage());
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
	
	private void notifyUnableToPrepare(){
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

	
	// -------------------
	// Recording indicator
	// -------------------
	private void showRecordingIndicator(){
		preview.setRecording(true);
	}

	private void hideRecordingIndicator(){
		preview.setRecording(false);
	}
	
	public View getView(){
		if(preview == null){
			preview = new PreviewTextureView(context);
			preview.setSurfaceTextureListener(this);
		}
		preview.setRecording(isRecording);
		return preview;

	}

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture holder, int width, int height) {
		Log.i(TAG, "cameraPreviewSurfaceCreated + " + holder);
		
		Camera camera = CameraManager.getCamera(context);
		if (camera == null)
			return;
		
		try {
			camera.setPreviewTexture(holder);
		} catch (IOException e) {
			Log.e(TAG, "Error setting camera preview: " + e.getMessage());
			if (videoRecorderExceptionHandler != null)
				videoRecorderExceptionHandler.unableToSetPrievew();
			return;
		}
       	
		camera.startPreview();
		prepareMediaRecorder();
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		stopRecording();
		Camera camera = CameraManager.getCamera(context);
		if (camera == null)
			return false; 
		camera.stopPreview();
		release();
		return true;
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {	}


}
