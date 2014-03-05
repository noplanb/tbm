package com.noplanbees.tbm;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraPreview";
	private SurfaceHolder surfaceHolder;
	private VideoHandler videoHandler;

    @SuppressWarnings("deprecation")
	public CameraPreview(Context context, VideoHandler vh) {
        super(context);
        videoHandler = vh;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
    	Log.i(TAG, "surfaceCreated called in CameraPreview2.");
        videoHandler.cameraPreviewSurfaceCreated(holder);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        videoHandler.cameraPreviewSurfaceDestroyed(holder);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    	videoHandler.cameraPreviewSurfaceChanged(holder, format, w, h);
    }

}
