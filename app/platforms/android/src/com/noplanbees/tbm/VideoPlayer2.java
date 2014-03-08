package com.noplanbees.tbm;
import java.io.File;

import android.app.Activity;
import android.os.Environment;
import android.util.Log;
import android.widget.MediaController;
import android.widget.VideoView;

public class VideoPlayer2 {
	String TAG = this.getClass().getSimpleName();
	Activity activity;

    String dataSourcePath;
    String dataSourceFileName = "vid.mp4";
	VideoView videoView;

	public VideoPlayer2(Activity a) {
		activity = a;
		getDataSourcePath();
		videoView = (VideoView) activity.findViewById(R.id.play_video_view);
		videoView.setVideoPath(dataSourcePath);
//		videoView.setMediaController(new MediaController(activity));
		videoView.requestFocus();
		videoView.start();
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
