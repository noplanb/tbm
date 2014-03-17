package com.noplanbees.tbm;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback{
	private final String TAG = this.getClass().getSimpleName();

	public static SurfaceHolder surfaceHolder;
	private Context context;

	public CameraPreview(Context context) {
		super(context);
		Log.i(TAG, "constructor");
		context = context;
		init();
	}

	public CameraPreview(Context context, AttributeSet attrs){
		super(context, attrs);
		Log.i(TAG, "constructor");
		context = context;
		init();
	}

	public CameraPreview(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		Log.i(TAG, "constructor");
		context = context;
		init();
	}

	private void init(){
		surfaceHolder = getHolder();
		surfaceHolder.addCallback(this);
		// deprecated setting, but required on Android versions prior to 3.0
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.i(TAG, "surfaceChanged");
//		HomeActivity.instance.videoRecorder.cameraPreviewSurfaceChanged(holder, format, width, height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {		
		Log.i(TAG, "surfaceCreated");
		HomeActivity.instance.videoRecorder.previewSurfaceCreated(holder);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {		
		Log.i(TAG, "surfaceDestroyed");
//		HomeActivity.instance.videoRecorder.cameraPreviewSurfaceDestroyed(holder);
	}

}
