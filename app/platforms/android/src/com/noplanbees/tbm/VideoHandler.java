package com.noplanbees.tbm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.FrameLayout;

public class VideoHandler{

	private Context context;
	private Activity activity;
	private Camera camera;
	private CameraPreview cameraPreview;
	private MediaRecorder mediaRecorder;
	private FrameLayout preview_frame;


	public VideoHandler (Activity a){
		activity = a;
		context = activity.getApplicationContext();
		camera = getCameraInstance(1);
		printCameraParams(camera);
		setCameraParams();
		
		cameraPreview = new CameraPreview(context, this);
		preview_frame = (FrameLayout) activity.findViewById(R.id.camera_preview);
		preview_frame.addView(cameraPreview);
	}
    
	public void takePicture() {
		camera.takePicture(null, null, getPictureCallback());
	}

	public void stopRecording(){
		mediaRecorder.stop(); 							// stop the recording
		releaseMediaRecorder(mediaRecorder, camera); 	// release the MediaRecorder object
		camera.lock(); 									// take camera access back from MediaRecorder
	}

	public boolean startRecording(){
		if (mediaRecorder != null) {
			mediaRecorder.start();
			return true;
		} else {
			releaseMediaRecorder(mediaRecorder, camera);
			return false;
		}
	}

	public void pause() {
		releaseMediaRecorder(mediaRecorder, camera);       
		releaseCamera(camera);  		
	}

    
	public void cameraPreviewSurfaceCreated(SurfaceHolder holder){
		String TAG = "cameraPreviewSurfaceCreated";
    	Log.i(TAG, "called.");
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
            prepareMediaRecorder(camera, holder);
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
	}
    
	public void cameraPreviewSurfaceDestroyed(SurfaceHolder holder){
        // Take care of releasing the Camera preview.
	}
	
	public void cameraPreviewSurfaceChanged(SurfaceHolder holder, int format, int w, int h){
		String TAG = "cameraPreviewSurfaceCreated";
		Log.i(TAG, "called");
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (holder.getSurface() == null){
          // preview surface does not exist
          return;
        }

        // stop preview before making changes
        try {
            camera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
	}
	
	private void setCameraParams () {
		camera.setDisplayOrientation(90);
		Parameters cparams = camera.getParameters();
		cparams.setZoom(20);
		cparams.setPreviewSize(176, 144);
		cparams.setPictureSize(176, 144);
		camera.setParameters(cparams);
	}

	public boolean hasCameraHardware() {
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
			return true;
		} else {
			return false;
		}
	}

	private Camera getCameraInstance(int camera_id){
		Camera c = null;
		try {
			c = Camera.open(camera_id); 
		}
		catch (Exception e){
			System.err.print("getCameraInstance: camera not available");
		}
		return c;
	}

	private File getOutputMediaFile(int type) {
		String TAG = "getOutputMediaFile";
		File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "tbm");
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				Log.e(TAG, "Failed to create storage directory.");
				return null;
			}
		}
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
			return new File(dir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
		} else if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
			return new File(dir.getPath() + File.separator + "VID_" + timeStamp + "VIDEO.mp4");
		} else {
			return null;
		}
	}

	private PictureCallback getPictureCallback() {
		final String TAG = "takePhoto";
		PictureCallback pictureCB = new PictureCallback() {

			@Override
			public void onPictureTaken(byte[] data, Camera cam) {
				File picFile = getOutputMediaFile(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE);
				if (picFile == null) {
					Log.e(TAG, "Couldn't create media file; check storage permissions?");
					return;
				}

				try {
					FileOutputStream fos = new FileOutputStream(picFile);
					fos.write(data);
					fos.close();
					Log.i(TAG, "Pic Saved at: " + picFile.getPath());
				} catch (FileNotFoundException e) {
					Log.e(TAG, "File not found: " + e.getMessage());
					e.getStackTrace();
				} catch (IOException e) {
					Log.e(TAG, "I/O error writing file: " + e.getMessage());
					e.getStackTrace();
				}
			}
		};
		return pictureCB;
	}

	private void prepareMediaRecorder(Camera camera, SurfaceHolder surfaceHolder){
		String TAG = "prepareVideoRecorder";

		mediaRecorder = new MediaRecorder();

		// Step 1: Unlock and set camera to MediaRecorder
		camera.unlock();
		mediaRecorder.setCamera(camera);

		// Step 2: Set sources
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		// Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
		mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));

		// Step 4: Set output file
		String ofile = getOutputMediaFile(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO).toString();
		Log.i(TAG, "setting mediaRecorder outfile to: " + ofile);
		mediaRecorder.setOutputFile(ofile);

		// Step 5: Set the preview output
		mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());

		// Step 6: Prepare configured MediaRecorder
		try {
			mediaRecorder.prepare();
		} catch (IllegalStateException e) {
			Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder(mediaRecorder, camera);
		} catch (IOException e) {
			Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder(mediaRecorder, camera);
		}
	}

	private void releaseMediaRecorder(MediaRecorder mediaRecorder, Camera camera){
		if (mediaRecorder != null) {
			mediaRecorder.reset();   // clear recorder configuration
			mediaRecorder.release(); // release the recorder object
			mediaRecorder = null;
			camera.lock();           // lock camera for later use
		}
	}

	private void releaseCamera(Camera camera){
		if (camera != null){
			camera.release();        // release the camera for other applications
			camera = null;
		}
	}

	public void printCameraParams(Camera camera){
		Parameters cparams = camera.getParameters();
		List<Integer> pic_formats = cparams.getSupportedPictureFormats();
		List<Integer> prev_formats = cparams.getSupportedPreviewFormats();
		for (Camera.Size size : cparams.getSupportedPreviewSizes()){
			printWH("Preview", size);
		}
		for (Camera.Size size : cparams.getSupportedPictureSizes()){
			printWH("Picture", size);      
		}
		List<Size> video_sizes = cparams.getSupportedVideoSizes();
		if (video_sizes == null){
			System.out.print("Video sizes not supported separately from preview sizes or picture sizes.");
		} else {
			for (Camera.Size size : video_sizes){
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

	private void printWH (String type, Camera.Size size){
		System.out.print(type + ": ");
		System.out.print(size.width);
		System.out.print("x");
		System.out.print(size.height);
		System.out.print("\n");
	}


}
