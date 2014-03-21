package com.noplanbees.tbm;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraOverlay extends SurfaceView implements SurfaceHolder.Callback {

	private final String TAG = this.getClass().getSimpleName(); 
	public SurfaceHolder holder;

	public CameraOverlay(Context context) {
		super(context);
		Log.i(TAG, "constructor");
		init();
	}

	public CameraOverlay(Context context, AttributeSet attrs){
		super(context, attrs);
		Log.i(TAG, "constructor");
		init();
	}

	public CameraOverlay(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		Log.i(TAG, "constructor");
		init();
	}


	private void init(){
		holder = getHolder();
		holder.addCallback(this);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {	
		Log.i(TAG, "surfaceCreated");
		HomeActivity.instance.videoRecorder.overlaySurfaceCreated(holder);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.i(TAG, "surfaceChanged");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {		
		Log.i(TAG, "surfaceDestroyed");
	}

}