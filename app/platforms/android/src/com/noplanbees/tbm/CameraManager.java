package com.noplanbees.tbm;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.util.Log;


// Queries, gets, and sets parameters for the camera we use.


@SuppressWarnings("deprecation")
public class CameraManager {

public static interface CameraExceptionHandler{
	public void noCameraHardware();
	public void noFrontCamera();
	public void cameraInUseByOtherApplication();
	public void unableToSetCameraParams();
	public void unableToFindAppropriateVideoSize();
}

	private static String TAG = CameraManager.class.getSimpleName();

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
		Log.i(TAG, "releaseCamera");
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
			Log.e(TAG, String.format("unlockCamera: ERROR: %s this should never happen!", e.toString()));
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
			Log.e(TAG, String.format("lockCamera: ERROR: %s this should never happen!", e.toString()));
			return false;
		}
		return true;
	}
	
	// ---------------
	// Private methods
	// ---------------
	private static Camera setupFrontCamera(Context context){
		Log.i(TAG, "getFrontCamera:");
		releaseCamera();
		
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
			Log.e(TAG, "getFrontCamera: ERROR: camera not available.");
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
	private static int frontCameraNum(){
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
		Log.i(TAG, String.format("setCameraParams: setPreviewSize %d %d", videoSize.width, videoSize.height));
		selectedPreviewSize = videoSize;
		cparams.setPreviewSize(videoSize.width, videoSize.height);
		
		// Set antibanding
		String ab = getAppropriateAntibandingSetting(cparams);
		if (ab != null)
			cparams.setAntibanding(ab);
		
//		if(cparams.)
		
//		THIS FUCKING BREAKS RECORDING ON MOTOG RUNNING 4.4.2 What a pain to find this. Need to get into the driver code to report a bug to android / moto!
//		if (Build.VERSION.SDK_INT >= 14)
//			cparams.setRecordingHint(true);
		
		//cparams.setPreviewFpsRange( 30000, 30000 ); // 30 fps
		//if ( cparams.isAutoExposureLockSupported() )
		//    cparams.setAutoExposureLock( true );
		
//		int minExpComp = cparams.getMinExposureCompensation();
//		int maxExpComp = cparams.getMaxExposureCompensation();
//		Log.d(TAG, "before minExpComp: " + minExpComp);
//		Log.d(TAG, "before maxExpComp: " + maxExpComp);
//		
//		if( minExpComp != 0 || maxExpComp != 0 ) {
//			Vector<String> exposures = new Vector<String>();
//			for(int i=minExpComp;i<=maxExpComp;i++) {
//				exposures.add("" + i);
//			}
//
//			int exposure = 0;
//			if( exposure < minExpComp || exposure > maxExpComp ) {
//				exposure = 0;
//					Log.d(TAG, "saved exposure not supported, reset to 0");
//				if( exposure < minExpComp || exposure > maxExpComp ) {
//						Log.d(TAG, "zero isn't an allowed exposure?! reset to min " + minExpComp);
//					exposure = minExpComp;
//				}
//			}
//			cparams.setExposureCompensation(exposure);
//		}
		
//		CamcorderProfile profile = CamcorderProfile.get(frontCameraNum(), CamcorderProfile.QUALITY_HIGH);//getCamcorderProfile();
//		List<int []> fps_ranges = cparams.getSupportedPreviewFpsRange();
//		int selected_min_fps = -1, selected_max_fps = -1, selected_diff = -1;
//        for(int [] fps_range : fps_ranges) {
//    			Log.d(TAG, "    supported fps range: " + fps_range[0] + " to " + fps_range[1]);
//	    	
//			int min_fps = fps_range[0];
//			int max_fps = fps_range[1];
//			if( min_fps <= profile.videoFrameRate*1000 && max_fps >= profile.videoFrameRate*1000 ) {
//    			int diff = max_fps - min_fps;
//    			if( selected_diff == -1 || diff < selected_diff ) {
//    				selected_min_fps = min_fps;
//    				selected_max_fps = max_fps;
//    				selected_diff = diff;
//    			}
//			}
//        }
//        if( selected_min_fps == -1 ) {
//        	selected_diff = -1;
//        	int selected_dist = -1;
//            for(int [] fps_range : fps_ranges) {
//    			int min_fps = fps_range[0];
//    			int max_fps = fps_range[1];
//    			int diff = max_fps - min_fps;
//    			int dist = -1;
//    			if( max_fps < profile.videoFrameRate*1000 )
//    				dist = profile.videoFrameRate*1000 - max_fps;
//    			else
//    				dist = min_fps - profile.videoFrameRate*1000;
//        			Log.d(TAG, "    supported fps range: " + min_fps + " to " + max_fps + " has dist " + dist + " and diff " + diff);
//    			if( selected_dist == -1 || dist < selected_dist || ( dist == selected_dist && diff < selected_diff ) ) {
//    				selected_min_fps = min_fps;
//    				selected_max_fps = max_fps;
//    				selected_dist = dist;
//    				selected_diff = diff;
//    			}
//            }
//	    		Log.d(TAG, "    can't find match for fps range, so choose closest: " + selected_min_fps + " to " + selected_max_fps);
//	        cparams.setPreviewFpsRange(selected_min_fps, selected_max_fps);
//        }
//        else {
//    			Log.d(TAG, "    chosen fps range: " + selected_min_fps + " to " + selected_max_fps);
//	    	cparams.setPreviewFpsRange(selected_min_fps, selected_max_fps);
//        }
//		
//		camera.setParameters(cparams);
		return true;
	}
	
	private static String getAppropriateAntibandingSetting(Camera.Parameters cparams){
		// Order of our preference is: OFF, AUTO, NULL as available.
		String r = null;
		List <String> abSettings = cparams.getSupportedAntibanding();
		if(abSettings == null)
			return null;
		for (String setting : abSettings){
			if (setting.equals(Camera.Parameters.ANTIBANDING_AUTO))
				r = Camera.Parameters.ANTIBANDING_AUTO;
		}
		for (String setting : abSettings){
			if (setting.equals(Camera.Parameters.ANTIBANDING_OFF))
				r = Camera.Parameters.ANTIBANDING_OFF;
		}
		Log.i(TAG, String.format("Setting antibanding to: %s", r));
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
			Log.i(TAG, String.format("%s,  %f", stringWithCameraSize(size), aspectRation(size)));
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
