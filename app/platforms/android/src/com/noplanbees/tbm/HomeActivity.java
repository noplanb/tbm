package com.noplanbees.tbm;

import java.util.ArrayList;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.VideoView;

public class HomeActivity extends Activity {

	final String TAG = this.getClass().getSimpleName();
	final Float ASPECT = 144F/176F;

	public static HomeActivity instance;

	private FriendFactory friendFactory;
	private ConfigFactory configFactory;
	private Config config;
	
	private FrameLayout cameraPreviewFrame;
	public VideoRecorder videoRecorder;
	private GcmHandler gcmHandler;
	
	private ArrayList<VideoView> videoViews = new ArrayList<VideoView>(8);
	private ArrayList<TextView> plusTexts = new ArrayList<TextView>(8);
	private ArrayList<FrameLayout> frames = new ArrayList<FrameLayout>(8);
	private HashMap<Integer, Integer> indexOfView = new HashMap<Integer, Integer>(8);
	private HashMap<Integer, Integer> indexOfText = new HashMap<Integer, Integer>(8);
	private ArrayList<VideoPlayer> videoPlayers = new ArrayList<VideoPlayer>(8);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
		setContentView(R.layout.home);
		instance = this;
		boot();
		runTests();
		gcmHandler = new GcmHandler(this);
		if (gcmHandler.checkPlayServices()){
			gcmHandler.registerGcm();
			init_page();
		} else {
			Log.e(TAG, "No valid Google Play Services APK found.");
		}
	}

	private void boot() {
		Boot.boot(this); //Note Boot.boot must happen first as it restores friend and config if necessary.
		friendFactory = FriendFactory.getFactoryInstance();
		configFactory = ConfigFactory.getFactoryInstance();
		config = configFactory.makeInstance();
	}

	private void runTests() {
		new DrawTest(this);
		// ConfigTest.run();		
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "onPause");
		videoRecorder.dispose();
		videoRecorder = null;
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume");
		if (gcmHandler.checkPlayServices()){
			getVideoRecorder();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		configFactory.save();
		friendFactory.save();
	}

	private void ulTest(){
		Log.i(TAG, "ulTest");
		Intent i = new Intent(this, FileUploadService.class);
		i.putExtra("filePath", "http:www.myurl.com");
		i.putExtra("toId", "1");
		startService(i);
	}

	private void init_page() {
		getVideoViewsAndPlayers();
		cameraPreviewFrame = (FrameLayout) findViewById(R.id.camera_preview_frame);
		cameraPreviewFrame.addView(new ViewSizeGetter(this));
		addListeners();
	}

	private void getVideoViewsAndPlayers() {
		videoViews.add((VideoView) findViewById(R.id.VideoView0));
		videoViews.add((VideoView) findViewById(R.id.VideoView1));
		videoViews.add((VideoView) findViewById(R.id.VideoView2));
		videoViews.add((VideoView) findViewById(R.id.VideoView3));
		videoViews.add((VideoView) findViewById(R.id.VideoView4));
		videoViews.add((VideoView) findViewById(R.id.VideoView5));
		videoViews.add((VideoView) findViewById(R.id.VideoView6));
		videoViews.add((VideoView) findViewById(R.id.VideoView7));
		
		plusTexts.add((TextView) findViewById(R.id.PlusText0));
		plusTexts.add((TextView) findViewById(R.id.PlusText1));
		plusTexts.add((TextView) findViewById(R.id.PlusText2));
		plusTexts.add((TextView) findViewById(R.id.PlusText3));
		plusTexts.add((TextView) findViewById(R.id.PlusText4));
		plusTexts.add((TextView) findViewById(R.id.PlusText5));
		plusTexts.add((TextView) findViewById(R.id.PlusText6));
		plusTexts.add((TextView) findViewById(R.id.PlusText7));
		
		frames.add((FrameLayout) findViewById(R.id.Frame0));
		frames.add((FrameLayout) findViewById(R.id.Frame1));
		frames.add((FrameLayout) findViewById(R.id.Frame2));
		frames.add((FrameLayout) findViewById(R.id.Frame3));
		frames.add((FrameLayout) findViewById(R.id.Frame4));
		frames.add((FrameLayout) findViewById(R.id.Frame5));
		frames.add((FrameLayout) findViewById(R.id.Frame6));
		frames.add((FrameLayout) findViewById(R.id.Frame7));
		
		for (int i=0; i<8; i++){
			indexOfText.put(plusTexts.get(i).getId(), i);
			indexOfView.put(videoViews.get(i).getId(), i);
		}
		
		for (int i=0; i<friendFactory.count(); i++){
			plusTexts.get(i).setVisibility(View.INVISIBLE);
			videoViews.get(i).setVisibility(View.VISIBLE);
			videoPlayers.add( i, new VideoPlayer( this, videoViews.get(i) ) );
		}
	}

	private void setVideoViewHeights(int width, int height) {
		int h = (int) ((float) width / ASPECT);
		LayoutParams lp = cameraPreviewFrame.getLayoutParams();
		lp.height = h;
		cameraPreviewFrame.setLayoutParams(lp);
		Log.i(TAG, String.format("setVideoViewHeights %d  %d", height, lp.height));

		lp = frames.get(0).getLayoutParams();
		lp.height = h;
		for (FrameLayout f: frames)
			f.setLayoutParams(lp);
	}

	private void getVideoRecorder() {
		if (videoRecorder == null)
			Log.i(TAG, "getVideoHandler: new VideoHandler");
		videoRecorder = new VideoRecorder(this);
	}

	private void onRecordStart(View v){
		if (videoRecorder.startRecording()) {
			Log.i(TAG, "onRecordStart: START RECORDING. view = " + indexOfView.get(v.getId()));
		} else {
			Log.e(TAG, "onRecordStart: unable to start recording" + indexOfView.get(v.getId()));
		}	
	}

	private void onRecordStop(View v){
		Log.i(TAG, "onRecordStop: STOP RECORDING." + indexOfView.get(v.getId()));
		videoRecorder.stopRecording();
	}

	private void onRecordCancel(View v){
		Log.i(TAG, "onRecordCancel: CANCEL RECORDING." + indexOfView.get(v.getId()));
		videoRecorder.stopRecording();
	}

	private void onPlayClick(View v) {
		int i = indexOfView.get(v.getId());
		Log.i(TAG, "onPlayClick" + Integer.toString(i));
		videoPlayers.get(i).setVideoSourcePath(videoRecorder.getRecordedFilePath());
		videoPlayers.get(i).click();
	}

	private void addListeners() {

		Button btnUpload = (Button) findViewById(R.id.btnUpload);
		btnUpload.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ulTest();
			}
		});

		for (VideoView vv : videoViews){
			Log.i(TAG, String.format("Adding LongPressTouchHandler for vv: %d", indexOfView.get(vv.getId())));
			new LongpressTouchHandler(vv) {

				@Override
				public void click(View v) {
					super.click(v);
					onPlayClick(v);
				}

				@Override
				public void startLongpress(View v) {
					super.startLongpress(v);
					onRecordStart(v);
				}

				@Override
				public void endLongpress(View v) {
					super.endLongpress(v);
					onRecordStop(v);
				}

				@Override
				public void bigMove(View v) {
					super.bigMove(v);
					onRecordCancel(v);
				}
			};
		};
	}


	private class ViewSizeGetter extends View{
		int width;
		int height;

		public ViewSizeGetter(Context context) {
			super(context);
		}

		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			width = w;
			height = h;
		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			setVideoViewHeights(width, height);
		}
	}
};
