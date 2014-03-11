package com.noplanbees.tbm;
import java.io.File;

import android.app.Activity;
import android.os.Environment;
import android.util.Log;
import android.widget.MediaController;
import android.widget.VideoView;

public class VideoPlayer {
	String TAG = this.getClass().getSimpleName();
	Activity activity;

    String dataSourcePath;
    String dataSourceFileName = "vid.mp4";
	VideoView videoView;
	String currentVideoPath;

	public VideoPlayer(Activity a, VideoView vv) {
		activity = a;
		videoView = vv;
//		videoView.setMediaController(new MediaController(activity));
		videoView.requestFocus();
	}
	
	public String getCurrentVideoPath(){
		return currentVideoPath;
	}
	
	public void setVideoSourcePath(String path){
		if (path == currentVideoPath) 
			return;
		currentVideoPath = path;
		videoView.setVideoPath(path);
	}
	
	public void click(){
		if (videoView.isPlaying()){
			videoView.pause();
		} else {
			videoView.start();
		}
	}
	
	public void start(){
		videoView.start();
	}
	
	public void pause(){
		videoView.pause();
	}
	
	public void stop(){
		videoView.stopPlayback();
	}
	
	public void release(){
//		videoView.r
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
	
	
}
