package com.noplanbees.tbm;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class HomeActivity extends Activity {
	final String TAG = this.getClass().getSimpleName();

	private VideoHandler videoHandler;
	private Boolean isRecording;
	private Button recordButton;
	private Button captureButton;

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
	}

	private void init_page() {
		recordButton = (Button) findViewById(R.id.button_record);
		captureButton = (Button) findViewById(R.id.button_capture);
		isRecording = false;
		addListeners();
	}
	
	private void getVideoHandler() {
		if (videoHandler == null)
			Log.i(TAG, "getVideoHandler: new VideoHandler");
			videoHandler = new VideoHandler(this);
	}

	private void onCaptureClick() {
		videoHandler.takePicture();
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

	private void addListeners() {
		captureButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onCaptureClick();
			}
		});

		recordButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onRecordClick();
			}
		});
	}
};
