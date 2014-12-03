package com.noplanbees.tbm;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;


// Queries, gets, and sets parameters for the camera we use.


public class CameraManager {

public static interface CameraExceptionHandler{
	public void noCameraHardware();
	public void noFrontCamera();
	public void cameraInUseByOtherApplication();
	public void unableToSetCameraParams();
	public void unableToFindAppropriateVideoSize();
}

final String TAG = this.getClass().getSimpleName();
	private static String STAG = CameraManager.class.getSimpleName();

	// Allow registration of a single delegate to handle exceptions.
	private static CameraExceptionHandler cameraExceptionHandler;
	public static void addExceptionHandlerDelegate(CameraExceptionHandler handler){
		cameraExceptionHandler = handler;
	}
	
	private static Camera camera = null;
	private static Camera.Size selectedPreviewSize = null;
	
	// --------------
	// Public methods
	// --------------
	public static Camera getCamera(Context context){
		if(camera == null)
			setupFrontCamera(context);
		return camera;
	}
	
	public static Camera.Size getPreviewSize(){
		return selectedPreviewSize;
	}
	
	public static void releaseCamera(){
		Log.i(STAG, "releaseCamera");
		if (camera != null){
			lockCamera(); // lock camera in case it was unlocked by the VideoRecorder.
			camera.release();
		}
	    camera = null;
	}
	
	public static Boolean unlockCamera(){
		if (camera == null){
			notifyCameraInUse();
			return false;
		}
		
		try{
			camera.unlock();
		} catch (RuntimeException e) {
			Log.e(STAG, String.format("unlockCamera: ERROR: %s this should never happen!", e.toString()));
			notifyCameraInUse();
			return false;
		}
		return true;
	}
	
	public static Boolean lockCamera(){
		if (camera == null)
			return false;
		
		try{
			camera.lock();
		} catch (RuntimeException e) {
			Log.e(STAG, String.format("lockCamera: ERROR: %s this should never happen!", e.toString()));
			return false;
		}
		return true;
	}
	
	// ---------------
	// Private methods
	// ---------------
	private static Camera setupFrontCamera(Context context){
		Log.i(STAG, "getFrontCamera:");
		//releaseCamera();
		
		if (!hasCameraHardware(context)){
			if (cameraExceptionHandler != null)
				cameraExceptionHandler.noCameraHardware();
			return camera;
		}
		
		Integer cameraNum = frontCameraNum();
		if (cameraNum < 0)
			return camera;
		
		try {
			camera = Camera.open(cameraNum);
		} catch (Exception e) {
			Log.e(STAG, "getFrontCamera: ERROR: camera not available.");
			notifyCameraInUse();
		}	
		if (camera == null){
			notifyCameraInUse();
			return camera;
		}
		
		if ( !setCameraParams() )
			camera = null;
		
		return camera;
	}
	private static boolean hasCameraHardware(Context context) {
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			return true;
		} 
		return false;
	}

	// -1 if no camera else the number of the front camera.
	private static Integer frontCameraNum(){
		Integer r = -1;
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		for (int i=0; i<Camera.getNumberOfCameras(); i++){
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
				r = i;
		}
		if (r < 0  && cameraExceptionHandler != null)
			cameraExceptionHandler.noFrontCamera();
		return r;
	}
	
	private static void notifyCameraInUse(){
		if (cameraExceptionHandler != null)
			cameraExceptionHandler.cameraInUseByOtherApplication();
	}
	
	@SuppressLint("NewApi")
	private static Boolean setCameraParams(){
		if (camera == null){
			if (cameraExceptionHandler != null)
				cameraExceptionHandler.unableToSetCameraParams();
			return false;
		}
		
		camera.setDisplayOrientation(90);
		
		Parameters cparams = camera.getParameters();
		
		// Set the preview size
		Camera.Size videoSize = getAppropriateVideoSize(cparams);
		if (videoSize == null){
			if (cameraExceptionHandler != null){
				cameraExceptionHandler.unableToFindAppropriateVideoSize();
			}
			return false;
		}
		Log.i(STAG, String.format("setCameraParams: setPreviewSize %d %d", videoSize.width, videoSize.height));
		selectedPreviewSize = videoSize;
		cparams.setPreviewSize(videoSize.width, videoSize.height);
		
		// Set antibanding
		String ab = getAppropriateAntibandingSetting(cparams);
		if (ab != null)
			cparams.setAntibanding(ab);
		
//		THIS FUCKING BREAKS RECORDING ON MOTOG RUNNING 4.4.2 What a pain to find this. Need to get into the driver code to report a bug to android / moto!
//		if (Build.VERSION.SDK_INT >= 14)
//			cparams.setRecordingHint(true);
		
		camera.setParameters(cparams);
		return true;
	}
	
	private static String getAppropriateAntibandingSetting(Camera.Parameters cparams){
		// Order of our preference is: OFF, AUTO, NULL as available.
		String r = null;
		List <String> abSettings = cparams.getSupportedAntibanding();
		for (String setting : abSettings){
			if (setting.equals(Camera.Parameters.ANTIBANDING_AUTO))
				r = Camera.Parameters.ANTIBANDING_AUTO;
		}
		for (String setting : abSettings){
			if (setting.equals(Camera.Parameters.ANTIBANDING_OFF))
				r = Camera.Parameters.ANTIBANDING_OFF;
		}
		Log.i(STAG, String.format("Setting antibanding to: %s", r));
		return r;
	}
	
	private static Camera.Size getAppropriateVideoSize(Camera.Parameters cparams){
		Camera.Size r = null;
		List <Camera.Size> previewSizes = cparams.getSupportedPreviewSizes();

		// If 320x240 exists then use it
		r = getVideoSize(previewSizes, 320, 240);
		if (r != null)
			return r;
		
		// Otherwise find the smallest size with a 1.33 aspect ratio. 
		sortVideoSizeByAscendingWidth(previewSizes);
		for (Camera.Size size : previewSizes) {
			Log.i(STAG, String.format("%s,  %f", stringWithCameraSize(size), aspectRation(size)));
			if ( Math.abs(aspectRation(size) - 1.33) < 0.009){
				r = size;
				break;
			}
		}
		return r;
	}
	
	private static Camera.Size getVideoSize(List <Camera.Size> videoSizes, int width, int height){
		Camera.Size r = null;
		for (Camera.Size size : videoSizes){
			if (size.width == width && size.height == height){
				r = size;
				break;
			}
		}
		return r;
	}
	
	private static void sortVideoSizeByAscendingWidth(List <Camera.Size> videoSizes){
		  Collections.sort(videoSizes, new Comparator<Camera.Size>(){
             public int compare(Camera.Size s1,Camera.Size s2){
            	 return ((Integer) s1.width).compareTo((Integer) s2.width);
           }});
	}
	
	private static float aspectRation(Camera.Size size){
		return (float) size.width / (float) size.height;
	}
	
	private static void printCameraParams(Camera camera) {
		Parameters cparams = camera.getParameters();
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
	
	private static void printWH(String type, Camera.Size size) {
		System.out.print(type + ": ");
		System.out.print(size.width);
		System.out.print("x");
		System.out.print(size.height);
		System.out.print("\n");
	}
	
	private static String stringWithCameraSize(Camera.Size size){
		return size.width + "x" + size.height;
	}
}
