package com.noplanbees.tbm;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class HomeActivity extends Activity {

	final String TAG = this.getClass().getSimpleName();
	final Float ASPECT = 144F/176F;

	public static HomeActivity instance;

	private FriendFactory friendFactory;
	private UserFactory userFactory;
	private User user;

	private FrameLayout cameraPreviewFrame;
	public VideoRecorder videoRecorder;
	private GcmHandler gcmHandler;

	private ArrayList<VideoView> videoViews = new ArrayList<VideoView>(8);
	private ArrayList<TextView> plusTexts = new ArrayList<TextView>(8);
	private ArrayList<FrameLayout> frames = new ArrayList<FrameLayout>(8);
	private ArrayList<TextView> nameTexts = new ArrayList<TextView>(8);

	private HashMap<String, VideoPlayer> videoPlayers = new HashMap<String, VideoPlayer>(8);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
		setContentView(R.layout.home);
		//Note Boot.boot must complete successfully before we continue the home activity. 
		//Boot will start the registrationActivity and return false if needed. 
		if (!Boot.boot(this)){
			Log.i(TAG,"Finish HomeActivity");
			finish();
			return;
		}
		initModels();
		init_page();
		runTests();
	}

	private void initModels() {
		instance = this;
		gcmHandler = new GcmHandler(this);
		friendFactory = FriendFactory.getFactoryInstance();
		userFactory = UserFactory.getFactoryInstance();
		user = userFactory.makeInstance();
	}

	private void runTests() {
		// new DrawTest(this);
		// ConfigTest.run();
		// FriendTest.run();
		// new ServerTest().run();
		// new FileDownload.BgDownload().execute();
		// Friend f = (Friend) friendFactory.findWhere("firstName", "Farhad");
		// new FileDownload.BgDownloadFromFriendId().execute(f.get("id"));
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
		if (userFactory != null)
			userFactory.save();
		if (friendFactory != null)
			friendFactory.save();
	}

	private void ulTest(){
		Log.i(TAG, "ulTest");
		Intent i = new Intent(this, FileUploadService.class);
		i.putExtra("filePath", "/storage/sdcard0/Movies/tbm/last.mp4");
		i.putExtra("userId", "1");
		i.putExtra("receiverId", "3");
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

		nameTexts.add((TextView) findViewById(R.id.nameText0));
		nameTexts.add((TextView) findViewById(R.id.nameText1));
		nameTexts.add((TextView) findViewById(R.id.nameText2));
		nameTexts.add((TextView) findViewById(R.id.nameText3));
		nameTexts.add((TextView) findViewById(R.id.nameText4));
		nameTexts.add((TextView) findViewById(R.id.nameText5));
		nameTexts.add((TextView) findViewById(R.id.nameText6));		
		nameTexts.add((TextView) findViewById(R.id.nameText6));

		for (Integer i=0; i<friendFactory.count(); i++){
			Integer viewId = videoViews.get(i).getId();
			Friend f = (Friend) friendFactory.findWhere("viewIndex", i.toString());
			f.set("viewId", viewId.toString());
			plusTexts.get(i).setVisibility(View.INVISIBLE);
			videoViews.get(i).setVisibility(View.VISIBLE);
			nameTexts.get(i).setText(f.get("firstName"));
			videoPlayers.put(f.get("id"), new VideoPlayer( this, videoViews.get(i) ));
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
		Friend f = FriendFactory.getFriendFromVew(v);
		if (videoRecorder.startRecording()) {
			Log.i(TAG, "onRecordStart: START RECORDING. view = " +f.get("firstName"));
		} else {
			Log.e(TAG, "onRecordStart: unable to start recording" + f.get("firstName"));
		}	
	}

	private void onRecordStop(View v){
		Friend f = FriendFactory.getFriendFromVew(v);
		Log.i(TAG, "onRecordStop: STOP RECORDING. to " + f.get("firstName"));
		if ( videoRecorder.stopRecording(f.get("id")) ){
			upload(v);
		} else {
			toast("Not sent. Too short.");
		}
	}

	private void onRecordCancel(View v){
		Friend f = FriendFactory.getFriendFromVew(v);
		Log.i(TAG, "onRecordCancel: CANCEL RECORDING." + f.get("firstName"));
		videoRecorder.stopRecording(f.get("id"));
	}

	private void onPlayClick(View v) {
		Friend f = FriendFactory.getFriendFromVew(v);
		Log.i(TAG, "onPlayClick" + f.get("firstName"));
		videoPlayers.get(f.get("id")).click();
	}

	private void upload(View v) {
		Log.i(TAG, "upload");
		Friend f = FriendFactory.getFriendFromVew(v);
		String receiverId = f.get("id");

		Intent i = new Intent(this, FileUploadService.class);
		i.putExtra("filePath", videoRecorder.getRecordedFilePath(receiverId));
		i.putExtra("userId", user.get("id"));
		i.putExtra("receiverId", receiverId);
		startService(i);
	}

	private void addListeners() {

		Button btnUpload = (Button) findViewById(R.id.btnUpload);
		btnUpload.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ulTest();
			}
		});

		Button btnReset = (Button) findViewById(R.id.btnReset);
		btnReset.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				friendFactory.destroyAll();
				userFactory.destroyAll();
				Boot.boot(HomeActivity.instance);
			}
		});

		for (VideoView vv : videoViews){
			Friend f = FriendFactory.getFriendFromVew(vv);
			Integer vvId = vv.getId();
			Log.i(TAG, "Adding LongPressTouchHandler for vv" + vvId.toString());
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

	private void toast(String msg){
		Toast toast=Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}
};
