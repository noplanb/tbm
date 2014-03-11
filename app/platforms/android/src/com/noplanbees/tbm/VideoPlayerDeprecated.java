package com.noplanbees.tbm;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

public class VideoPlayerDeprecated {
	private Activity activity;
	private Context context;
	private String TAG = this.getClass().getSimpleName(); 
	private MediaPlayer mediaPlayer;
	private String dataSourceFileName = "vid.mp4";
	private String dataSourcePath;
	private PlaySurfaceView playSurfaceView;
	private SurfaceHolder playSurfaceHolder;
	private FrameLayout playerFrame;
	
	public VideoPlayerDeprecated(Activity a) {
		activity = a;
		context = activity.getApplicationContext();
		playSurfaceView = new PlaySurfaceView(context);
		playerFrame = (FrameLayout) activity.findViewById(R.id.VideoView0);
		playerFrame.addView(playSurfaceView);
		getMediaPlayer();
        getDataSourcePath();
        try {
			mediaPlayer.setDataSource(dataSourcePath);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		} catch (SecurityException e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		} catch (IllegalStateException e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		}
        try {
			mediaPlayer.prepare();
		} catch (IllegalStateException e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		}

	}
	
	public void start(){
		mediaPlayer.start();
	}
	
	public void stop(){
		mediaPlayer.stop();
	}
	
	public void pause(){
		mediaPlayer.pause();
	}
	
	private void getMediaPlayer (){
		if (mediaPlayer == null)
			mediaPlayer = new MediaPlayer();
	}
	
	private void getDataSourcePath (){
		File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "tbm");
		if (!dir.exists())
			Log.e(TAG, "getDataSourcePath: Directory " + dir.getPath() + " does not exist.");
		
		File data_source_file = new File(dir, dataSourceFileName);
		if (!data_source_file.exists())
			Log.e(TAG,"getDataSourcePath: File " + data_source_file.getPath() + " does not exist.");
		
		dataSourcePath = data_source_file.getPath();
	}
	
	private class PlaySurfaceView extends SurfaceView implements SurfaceHolder.Callback {
		String TAG = this.getClass().getSimpleName();

		public PlaySurfaceView(Context context) {
			super(context);
			playSurfaceHolder = getHolder();
			playSurfaceHolder.addCallback(this);
			// deprecated setting, but required on Android versions prior to 3.0
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
				playSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			Log.i(TAG, "surfaceCreated.");
			mediaPlayer.setDisplay(holder);
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			Log.i(TAG, "surfaceChanged.");
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			Log.i(TAG, "surfaceDestroyed.");
		}
		
	}

}
