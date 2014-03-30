package com.noplanbees.tbm;

import java.io.File;
import java.io.IOException;
import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

public class VideoRecorder {

	private final String TAG = this.getClass().getSimpleName();

	private Context context;
	private Activity activity;
	private Camera camera;
	private SurfaceHolder previewSurfaceHolder; 
	private SurfaceHolder overlaySurfaceHolder;
	private MediaRecorder mediaRecorder;
	private SurfaceView previewSurface;
	private SurfaceView overlaySurface;
	private TextView previewText;

	public VideoRecorder(Activity a) {
		activity = a;
		context = activity.getApplicationContext();
		previewSurface = (SurfaceView) activity.findViewById(R.id.camera_preview_surface);
		overlaySurface = (SurfaceView) activity.findViewById(R.id.camera_overlay_surface);
		previewText = (TextView) activity.findViewById(R.id.previewText);
	}

	public boolean stopRecording(Friend friend) {
		Log.i(TAG, "stopRecording");
		boolean rval = true;
		hideRecordingIndicator();
		if (mediaRecorder !=null){
			try {
				mediaRecorder.stop();
				Log.i(TAG, String.format("Recorded file %s : %d",Config.recordingFilePath(), Config.recordingFile().length()));
				moveRecordingToFriend(friend);
			} catch (IllegalStateException e) {
				Log.e(TAG, "stopRecording: called in illegal state.");
				rval = false;
				releaseMediaRecorder();
			} catch (RuntimeException e) {
				Log.e(TAG, "stopRecording: Recording to short. No output file");
				rval = false;
				releaseMediaRecorder();
			}
			prepareMediaRecorder();
		}
		return rval;
	}

	public boolean startRecording() {
		if (mediaRecorder != null) {
			Log.i(TAG, "startRecording");
			mediaRecorder.start();	
			showRecordingIndicator();
			return true;
		} else {
			Log.e(TAG, "startRecording: Error no mediaRecorder");
			releaseMediaRecorder();
			return false;
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void showRecordingIndicator(){
		Runnable sri = new Runnable() {
			@Override
			public void run() {
				Log.i(TAG, "showRecordingIndicator");
				previewText.setText("Recording...");
				previewText.setTextColor(Color.RED);
				previewText.setVisibility(View.VISIBLE);
				Canvas c = overlaySurfaceHolder.lockCanvas();
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
				c.drawCircle(35, 35, 10, cpaint);
				overlaySurfaceHolder.unlockCanvasAndPost(c);
			}
		};
		activity.runOnUiThread(sri);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void hideRecordingIndicator(){
		Runnable hri = new Runnable(){
			@Override
			public void run() {
				Log.i(TAG, "hideRecordingIndicator");
				previewText.setVisibility(View.INVISIBLE);
				Canvas c = overlaySurfaceHolder.lockCanvas();
				c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
				overlaySurfaceHolder.unlockCanvasAndPost(c);
				overlaySurface.invalidate();
			}
		};
		activity.runOnUiThread(hri);
	}

	public void dispose() {
		Log.i(TAG, "dispose");
		previewSurface.setVisibility(View.GONE);
		overlaySurface.setVisibility(View.GONE);
	}

	public void restore(){
		Log.i(TAG, "restore");
		overlaySurface.setVisibility(View.VISIBLE);
		previewSurface.setVisibility(View.VISIBLE);
	}

	public void previewSurfaceCreated(SurfaceHolder holder) {
		Log.i(TAG, "cameraPreviewSurfaceCreated");
		previewSurfaceHolder = holder;
		try {
			getCameraInstance(1);
			// printCameraParams(camera);
			setCameraParams();
			camera.setPreviewDisplay(holder);
			camera.startPreview();
			prepareMediaRecorder();
			overlaySurface.bringToFront();
		} catch (IOException e) {
			Log.e(TAG, "Error setting camera preview: " + e.getMessage());
		}
	}

	public void previewSurfaceDestroyed(SurfaceHolder holder){
		releaseMediaRecorder();
		releaseCamera();
	}

	public void overlaySurfaceCreated(SurfaceHolder holder){
		Log.i(TAG, "overlaySurfaceCreated");
		overlaySurfaceHolder = holder;
		holder.setFormat(PixelFormat.TRANSPARENT);
	}

	@SuppressLint("NewApi")
	private void setCameraParams() {
		camera.setDisplayOrientation(90);
		Parameters cparams = camera.getParameters();
		//		cparams.setZoom(20);
		//cparams.setPreviewSize(176, 144);
		int width = 320;
		int height = 240;
		Log.i(TAG, String.format("setCameraParams: setPreviewSize %d %d", width, height) );
		cparams.setPreviewSize(width, height);
		//		cparams.setPictureSize(176, 144);
		//if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			//cparams.setRecordingHint(true);
		camera.setParameters(cparams);
	}

	public boolean hasCameraHardware() {
		if (context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA)) {
			return true;
		} else {
			return false;
		}
	}

	private void getCameraInstance(int camera_id) {
		Log.i(TAG, "getCameraInstance");
		releaseCamera();
		try {
			camera = Camera.open(camera_id);
		} catch (Exception e) {
			System.err.print("getCameraInstance: camera not available");
		}
		if (camera == null)
			Log.e(TAG, "getCameraInstance: got null for camera" + camera_id);
	}

	private void moveRecordingToFriend(Friend friend){
		File ed = friend.videoToFile();
		File ing = Config.recordingFile();
		ing.renameTo(ed);
	}

	private void prepareMediaRecorder() {
		if (mediaRecorder == null)
			mediaRecorder = new MediaRecorder();

		// Unlock and set camera to MediaRecorder
		camera.unlock();
		mediaRecorder.setCamera(camera);

		// Set sources
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		// Set format and encoder
//		mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//		mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//		mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//		mediaRecorder.setVideoSize(176, 144);

		mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_QVGA));

		String ofile = Config.recordingFilePath();

		Log.i(TAG, "prepareMediaRecorder: mediaRecorder outfile: " + ofile);
		mediaRecorder.setOutputFile(ofile);
		mediaRecorder.setOrientationHint(270);
		// Step 5: Set the preview output
		Log.i(TAG, "prepareMediaRecorder: mediaRecorder.setPreviewDisplay");
		mediaRecorder.setPreviewDisplay(previewSurfaceHolder.getSurface());


		// Step 6: Prepare configured MediaRecorder
		try {
			Log.i(TAG, "prepareMediaRecorder: mediaRecorder.prepare");
			mediaRecorder.prepare();
		} catch (IllegalStateException e) {
			Log.d(TAG,"IllegalStateException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
		} catch (IOException e) {
			Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
		}
	}

	private void releaseMediaRecorder() {
		Log.i(TAG, "releaseMediaRecorder");
		if (mediaRecorder != null) {
			mediaRecorder.reset(); // clear recorder configuration
			mediaRecorder.release(); // release the recorder object
			mediaRecorder = null;
			camera.lock(); // lock camera for later use
		}
	}

	private void releaseCamera() {
		Log.i(TAG, "releaseCamera");
		if (camera != null) {
			camera.release(); // release the camera for other applications
			camera = null;
		}
	}

	public void printCameraParams(Camera camera) {
		Parameters cparams = camera.getParameters();
		List<Integer> pic_formats = cparams.getSupportedPictureFormats();
		List<Integer> prev_formats = cparams.getSupportedPreviewFormats();
		for (Camera.Size size : cparams.getSupportedPreviewSizes()) {
			printWH("Preview", size);
		}
		for (Camera.Size size : cparams.getSupportedPictureSizes()) {
			printWH("Picture", size);
		}
		List<Size> video_sizes = cparams.getSupportedVideoSizes();
		if (video_sizes == null) {
			System.out
			.print("Video sizes not supported separately from preview sizes or picture sizes.");
		} else {
			for (Camera.Size size : video_sizes) {
				printWH("Video", size);
			}
		}
		boolean zoom_supported = cparams.isZoomSupported();
		System.out.print("\nZoom supported = ");
		System.out.print(zoom_supported);

		int max_zoom = cparams.getMaxZoom();
		System.out.print("\nMax zoom = ");
		System.out.print(max_zoom);
	}

	private void printWH(String type, Camera.Size size) {
		System.out.print(type + ": ");
		System.out.print(size.width);
		System.out.print("x");
		System.out.print(size.height);
		System.out.print("\n");
	}
}
