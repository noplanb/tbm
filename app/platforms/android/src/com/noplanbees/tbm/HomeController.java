package com.noplanbees.tbm;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.Button;

public class HomeController {

	private Activity activity;
	private Context context;
	private VideoHandler videoHandler;
	private Button captureBtn;
	private Button recordButton;
	private Boolean isRecording;

	public HomeController (Activity a){
		activity = a;
		context = a.getApplicationContext();
		videoHandler = new VideoHandler(activity);
		
        isRecording = false;
        
		captureBtn = (Button) activity.findViewById(R.id.button_capture); 
		recordButton = (Button) activity.findViewById(R.id.button_record);
		addListeners();
	}
    
	public void pause (){
		videoHandler.pause();
	}
	
	private void onCaptureClick(){
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
		captureBtn.setOnClickListener(new View.OnClickListener() {
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
}
