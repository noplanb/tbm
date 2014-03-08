package com.noplanbees.tbm;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class HomeActivity extends Activity {
	final String TAG = this.getClass().getSimpleName();

	private VideoHandler videoHandler;
	private VideoPlayer videoPlayer;
	private Boolean isRecording;
	private Button recordButton;
	private Button playButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
		setContentView(R.layout.home);
		init_page();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "onPause");
		videoHandler.dispose();
		videoHandler = null;
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume");
		getVideoHandler();
//		getVideoPlayer();
	}

	private void init_page() {
		recordButton = (Button) findViewById(R.id.button_record);
		playButton = (Button) findViewById(R.id.button_play);
		isRecording = false;
		addListeners();
	}
	
	private void getVideoHandler() {
		if (videoHandler == null)
			Log.i(TAG, "getVideoHandler: new VideoHandler");
			videoHandler = new VideoHandler(this);
	}
	
	private void getVideoPlayer() {
		if (videoPlayer == null){
			Log.i(TAG, "getVideoPlayer: new getVideoPlayer");
			videoPlayer = new VideoPlayer(this);
		} else {
			Log.i(TAG, "getVideoPlayer: using existing videoPlayer");
		}
	}


	private void onRecordClick() {
		if (isRecording) {
			videoHandler.stopRecording();
			recordButton.setText("Record");
			isRecording = false;
		} else {
			if (videoHandler.startRecording()) {
				recordButton.setText("Stop");
				isRecording = true;
			} else {
				recordButton.setText("Broken");
			}
		}
	}
	
	private void onPlayClick() {
//		videoPlayer.start();
		new VideoPlayer2(this);
	}

	private void addListeners() {
		recordButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onRecordClick();
			}
		});
		
		playButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onPlayClick();
			}
		});
	}
};
