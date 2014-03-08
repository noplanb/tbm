package com.noplanbees.tbm;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

public class VideoHandler {

	private String TAG = this.getClass().getSimpleName();
	private Context context;
	private Activity activity;
	private Camera camera;
	private CameraPreview cameraPreview; // The SurfaceView for the preview from
											// the camera.
	private SurfaceHolder surfaceHolder; // The surface holder holding the above
											// SurfaceView
	private MediaRecorder mediaRecorder; // The video recorder that attaches to
											// the above SurfaceView
	private FrameLayout preview_frame; // The view element that we add the
										// SurfaceView to.

	public VideoHandler(Activity a) {
		activity = a;
		context = activity.getApplicationContext();
		getCameraInstance(1);
		printCameraParams(camera);
		setCameraParams();

		cameraPreview = new CameraPreview(context);
		preview_frame = (FrameLayout) activity.findViewById(R.id.camera_preview_frame);
		preview_frame.addView(cameraPreview);
	}

	public void stopRecording() {
		mediaRecorder.stop(); // stop the recording
		prepareMediaRecorder();
	}

	public boolean startRecording() {
		if (mediaRecorder != null) {
			mediaRecorder.start();
			return true;
		} else {
			releaseMediaRecorder();
			return false;
		}
	}

	public void dispose() {
		releaseMediaRecorder();
		releaseCamera();
	}

	public void cameraPreviewSurfaceCreated(SurfaceHolder holder) {
		Log.i(TAG, "cameraPreviewSurfaceCreated");
		// The Surface has been created, now tell the camera where to draw the
		// preview.
		try {
			camera.setPreviewDisplay(holder);
			camera.startPreview();
			prepareMediaRecorder();
		} catch (IOException e) {
			Log.d(TAG, "Error setting camera preview: " + e.getMessage());
		}
	}

	public void cameraPreviewSurfaceDestroyed(SurfaceHolder holder) {
		Log.i(TAG, "cameraPreviewSurfaceDestroyed");
		// Take care of releasing the Camera preview.
	}

	public void cameraPreviewSurfaceChanged(SurfaceHolder holder, int format,
			int w, int h) {
		Log.i(TAG, "cameraPreviewSurfaceChanged");
		// If your preview can change or rotate, take care of those events here.
		// Make sure to stop the preview before resizing or reformatting it.

		if (holder.getSurface() == null) {
			// preview surface does not exist
			return;
		}

		// stop preview before making changes
		try {
			// camera.stopPreview();
		} catch (Exception e) {
			Log.d(TAG,
					"cameraPreviewSurfaceChanged: Error camera.stopPreview(): "
							+ e.getMessage());
		}

		// set preview size and make any resize, rotate or
		// reformatting changes here

		// start preview with new settings
		try {
			// camera.setPreviewDisplay(holder);
			// camera.startPreview();

		} catch (Exception e) {
			Log.d(TAG,
					"cameraPreviewSurfaceChanged: Error camera.startPreview(): "
							+ e.getMessage());
		}
	}

	private void setCameraParams() {
		camera.setDisplayOrientation(90);
		Parameters cparams = camera.getParameters();
		cparams.setZoom(20);
		cparams.setPreviewSize(176, 144);
		cparams.setPictureSize(176, 144);
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
		try {
			camera = Camera.open(camera_id);
		} catch (Exception e) {
			System.err.print("getCameraInstance: camera not available");
		}
		if (camera == null)
			Log.e(TAG, "getCameraInstance: got null for camera" + camera_id);
	}

	private File getOutputMediaFile(int type) {
		String TAG = "getOutputMediaFile";
		File dir = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
				"tbm");
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				Log.e(TAG, "Failed to create storage directory.");
				return null;
			}
		}
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
			return new File(dir.getPath() + File.separator + "img" + timeStamp
					+ ".jpg");
		} else if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
			return new File(dir.getPath() + File.separator + "vid_" + timeStamp
					+ ".mp4");
		} else {
			return null;
		}
	}

	private void prepareMediaRecorder() {
		if (mediaRecorder == null)
			mediaRecorder = new MediaRecorder();

		// Step 1: Unlock and set camera to MediaRecorder
		camera.unlock();
		mediaRecorder.setCamera(camera);

		// Step 2: Set sources
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		
//		mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_CIF));

		mediaRecorder.setVideoSize(176, 144);
		
		String ofile = getOutputMediaFile(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO).toString();
		
		
		Log.i(TAG, "prepareMediaRecorder: mediaRecorder outfile: " + ofile);
		mediaRecorder.setOutputFile(ofile);
		mediaRecorder.setOrientationHint(270);
		// Step 5: Set the preview output
		Log.i(TAG, "prepareMediaRecorder: mediaRecorder.setPreviewDisplay");
		mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());


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
		if (preview_frame != null)
			preview_frame.removeView(cameraPreview);
		if (camera != null) {
			camera.release(); // release the camera for other applications
			camera = null;
		}
		cameraPreview = null;
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

	private class CameraPreview extends SurfaceView implements
			SurfaceHolder.Callback {
		private String TAG = this.getClass().getSimpleName();

		public CameraPreview(Context context) {
			super(context);
			Log.i(TAG, "Instantiating CameraPreview");
			if (camera == null) {
				Log.i(TAG, "CameraPreview constructor camera is null");
			} else {
				Log.i(TAG, "CameraPreview constructor camera good");
			}
			// Install a SurfaceHolder.Callback so we get notified when the
			// underlying surface is created and destroyed.
			surfaceHolder = getHolder();
			surfaceHolder.addCallback(this);
			// deprecated setting, but required on Android versions prior to 3.0
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
				surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			if (camera == null) {
				Log.i(TAG, "CameraPreview surfaceCreated camera is null");
			} else {
				Log.i(TAG, "CameraPreview surfaceCreated camera good");
			}
			cameraPreviewSurfaceCreated(holder);
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			cameraPreviewSurfaceDestroyed(holder);
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int w,
				int h) {
			cameraPreviewSurfaceChanged(holder, format, w, h);
		}

	}
}
