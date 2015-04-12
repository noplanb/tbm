package com.zazoapp.client.multimedia;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.dispatch.Dispatch;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;


// Queries, gets, and sets parameters for the camera we use.


@SuppressWarnings("deprecation")
public class CameraManager {

    public static interface CameraExceptionHandler {
        void onCameraException(CameraException exception);
    }

    private static final String TAG = CameraManager.class.getSimpleName();

    // Allow registration of a single delegate to handle exceptions.
    private static CameraExceptionHandler cameraExceptionHandler;
    public static void addExceptionHandlerDelegate(CameraExceptionHandler handler){
        cameraExceptionHandler = handler;
    }

    private static Camera camera = null;
    private static Camera.Size selectedPreviewSize = null;
    private static int orientation;                        //used in videoRecorder to set video recording orientation
    private static int recalculatedOrientation;            //used in cameraParams to set camera parameters properly
    private static boolean is15FramesAvailable;

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
        if (camera != null){
            Log.i(TAG, "releaseCamera");
            camera.stopPreview();
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
            Dispatch.dispatch(String.format("unlockCamera: ERROR: %s this should never happen!", e.getLocalizedMessage()));
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
            Dispatch.dispatch(String.format("lockCamera: ERROR: %s this should never happen!", e.getLocalizedMessage()));
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
                cameraExceptionHandler.onCameraException(CameraException.NO_HARDWARE);
            return camera;
        }

        int cameraNum;
        if (DebugConfig.getInstance(context).shouldUseRearCamera()) {
            cameraNum = rearCameraNum();
        } else {
            cameraNum = frontCameraNum();
        }
        if (cameraNum < 0) {
            return camera;
        }

        try {
            camera = Camera.open(cameraNum);
        } catch (Exception e) {
            Dispatch.dispatch("getFrontCamera: ERROR: camera not available.");
            notifyCameraInUse();
        }
        if (camera == null){
            notifyCameraInUse();
            return camera;
        }
        recalculatedOrientation = getRecalculatedCameraOrientation(context, cameraNum);
        if ( !setCameraParams() )
            camera = null;

        //Check that 15 frame rate value is available in camera. Will use this parameter to
        //set frame rate in a VideoRecorder
        if(camera!=null) {
            List<Integer> supportedPreviewFrameRates = camera.getParameters().getSupportedPreviewFrameRates();
            for (int i = 0; i < supportedPreviewFrameRates.size(); i++) {
                if (supportedPreviewFrameRates.get(i) == 15) {
                    is15FramesAvailable = true;
                    break;
                }
            }
        }

        return camera;
    }

    private static boolean hasCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        }
        return false;
    }

    // -1 if no camera else the number of the front camera.
    private static int frontCameraNum() {
        int r = -1;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                r = i;
        }
        if (r < 0 && cameraExceptionHandler != null)
            cameraExceptionHandler.onCameraException(CameraException.NO_FRONT_CAMERA);
        return r;
    }

    // -1 if no camera else the number of the rear camera.
    private static int rearCameraNum() {
        int r = -1;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
                r = i;
        }
        return r;
    }

    private static void notifyCameraInUse(){
        if (cameraExceptionHandler != null)
            cameraExceptionHandler.onCameraException(CameraException.CAMERA_IN_USE);
    }

    @SuppressLint("NewApi")
    private static Boolean setCameraParams(){
        if (camera == null){
            if (cameraExceptionHandler != null)
                cameraExceptionHandler.onCameraException(CameraException.UNABLE_TO_SET_PARAMS);
            return false;
        }

        camera.setDisplayOrientation(recalculatedOrientation);

        Parameters cparams = camera.getParameters();

        // Set the preview size
        Camera.Size videoSize = getAppropriateVideoSize(cparams);
        if (videoSize == null){
            if (cameraExceptionHandler != null){
                cameraExceptionHandler.onCameraException(CameraException.UNABLE_TO_FIND_APPROPRIATE_VIDEO_SIZE);
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


        //http://stackoverflow.com/questions/22639336/android-mediacodec-and-camera-how-to-achieve-a-higher-frame-rate-to-get-frame-r/22645327#22645327
        List<int[]> fpsRanges = cparams.getSupportedPreviewFpsRange();
        boolean isSupportedMaxPreviewFpsRange = false;
        for (int[] fpsRange : fpsRanges) {
            if(fpsRange[0] == 30 && fpsRange[1] == 30){
                isSupportedMaxPreviewFpsRange = true;
                break;
            }
        }

        if(isSupportedMaxPreviewFpsRange)
            cparams.setPreviewFpsRange(30, 30);
        else{
            List<Integer> suppFrameRates = cparams.getSupportedPreviewFrameRates();
            for (Integer suppFrameRate : suppFrameRates) {
                if(suppFrameRate.equals(30)){
                    cparams.setPreviewFrameRate(30);
                    //THIS FUCKING BREAKS RECORDING ON MOTOG RUNNING 4.4.2 What a pain to find this. Need to get into the driver code to report a bug to android / moto!
//                    if (Build.VERSION.SDK_INT >= 14)
//                        cparams.setRecordingHint(true);
//                    break;
                }
            }
        }

        camera.setParameters(cparams);
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

    /**
     * This function recalculates initial camera angle, and gives angle delta that we can use to
     * properly set camera property
     * @param context - app or activity context
     * @param cameraNum - number of camera that need to be recalculated
     * @return
     */
    private static int getRecalculatedCameraOrientation(Context context, int cameraNum){
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();

        android.hardware.Camera.getCameraInfo(cameraNum, info);
        int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        orientation = info.orientation;

        return result;
    }

    public static int getOrientation() {
        return orientation;
    }

    public static boolean is15FramesAvailable() {
        return is15FramesAvailable;
    }
}
