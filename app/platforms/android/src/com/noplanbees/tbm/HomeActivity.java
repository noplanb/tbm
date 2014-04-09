package com.noplanbees.tbm;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class HomeActivity extends Activity {

	final String TAG = this.getClass().getSimpleName();
	final Float ASPECT = 240F/320F;

	public static HomeActivity instance;

	private FriendFactory friendFactory;
	private UserFactory userFactory;

	private FrameLayout cameraPreviewFrame;
	public VideoRecorder videoRecorder;
	private GcmHandler gcmHandler;
	public LocalBroadcastManager localBroadcastManger;
	private String lastState;

	private ArrayList<VideoView> videoViews = new ArrayList<VideoView>(8);
	private ArrayList<ImageView> thumbViews = new ArrayList<ImageView>(8);
	private ArrayList<TextView> plusTexts = new ArrayList<TextView>(8);
	private ArrayList<FrameLayout> frames = new ArrayList<FrameLayout>(8);
	private ArrayList<TextView> nameTexts = new ArrayList<TextView>(8);

	public HashMap<String, VideoPlayer> videoPlayers = new HashMap<String, VideoPlayer>(8);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate state");

		// If activity was destroyed and we got an intent due to a new video download
		// don't start up the activity. Send a notification instead and let the user 
		// click on the notification if he wants to start tbm.
		Integer intentResult = new IntentHandler(this, getIntent()).handle(IntentHandler.STATE_SHUTDOWN);
		if (intentResult != null && intentResult == IntentHandler.RESULT_FINISH){
			Log.i(TAG, "aborting home_activity becuase intent was for new video");
			super.onCreate(savedInstanceState);
			finish();
			return;
		}
		super.onCreate(savedInstanceState);

		//Note Boot.boot must complete successfully before we continue the home activity. 
		//Boot will start the registrationActivity and return false if needed. 
		if (!Boot.boot(this)){
			Log.i(TAG,"Finish HomeActivity");
			finish();
			return;
		}
		setContentView(R.layout.home);
		lastState = "onCreate";
	}

	private void initModels() {
		instance = this;
		videoRecorder = new VideoRecorder(this);
		gcmHandler = new GcmHandler(this);
		friendFactory = FriendFactory.getFactoryInstance();
		userFactory = UserFactory.getFactoryInstance();
		getVideoViewsAndPlayers();
	}

	private void ensureModels() {
		if ( instance == null ||
				videoRecorder == null ||
				gcmHandler == null ||
				friendFactory == null ||
				userFactory == null
				){
			initModels();
		}
	}

	private void runTests() {
		// new CamcorderHelper();
		//testService();
		// ConfigTest.run();
		// FriendTest.run();
		// new ServerTest().run();
		// new FileDownload.BgDownload().execute();
		// Friend f = (Friend) friendFactory.findWhere("firstName", "Farhad");
		// new FileDownload.BgDownloadFromFriendId().execute(f.get("id"));
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.i(TAG, "onStart: state");
		ensureModels();
		initViews();
		ensureListeners();
		videoRecorder.restore();
		//runTests();
		lastState = "onStart";
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.i(TAG, "onRestart: state");
		lastState = "onRestart";
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.i(TAG, "onStop: state");
		videoRecorder.dispose();
		lastState = "onStop";
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "onPause: state");
		ActiveModelsHandler.saveAll();
		lastState = "onPause";
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.i(TAG, "onNewIntent: state");
		int appState = (lastState.startsWith("onPause")) ? IntentHandler.STATE_FOREGROUND : IntentHandler.STATE_BACKGROUND;
		Integer intentResult = new IntentHandler(this, intent).handle(appState);
		if (intentResult != null && intentResult == IntentHandler.RESULT_FINISH){
			Log.i(TAG, "aborting home_activity on directive from intentHandler");
			finish();
			return;
		}
		lastState = "onNewIntent";
	}

	public VideoPlayer getVideoPlayerForFriend(Friend friend) {
		return videoPlayers.get(friend.getId());
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume: state");
		if (!gcmHandler.checkPlayServices()){
			Log.e(TAG, "onResume: checkPlayServices = false");
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy: state");
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
		nameTexts.add((TextView) findViewById(R.id.nameText7));

		thumbViews.add((ImageView) findViewById(R.id.ThumbView0));
		thumbViews.add((ImageView) findViewById(R.id.ThumbView1));
		thumbViews.add((ImageView) findViewById(R.id.ThumbView2));
		thumbViews.add((ImageView) findViewById(R.id.ThumbView3));
		thumbViews.add((ImageView) findViewById(R.id.ThumbView4));
		thumbViews.add((ImageView) findViewById(R.id.ThumbView5));
		thumbViews.add((ImageView) findViewById(R.id.ThumbView6));
		thumbViews.add((ImageView) findViewById(R.id.ThumbView7));

		for (Integer i=0; i<friendFactory.count(); i++){
			Friend f = (Friend) friendFactory.findWhere("viewIndex", i.toString());

			Integer frameId = frames.get(i).getId();
			f.set("frameId", frameId.toString());
			Integer viewId = videoViews.get(i).getId();
			f.set("viewId", viewId.toString());
			Integer thumbViewId = thumbViews.get(i).getId();
			f.set("thumbViewId", thumbViewId.toString());
			Integer nameTextId = nameTexts.get(i).getId();
			f.set("nameTextId", nameTextId.toString());
		}
		friendFactory.save();
	}
	
	private void initViews(){
		VideoStatusHandler vsh = new VideoStatusHandler(this);
		for (Integer i=0; i<friendFactory.count(); i++){
			Friend f = (Friend) friendFactory.findWhere("viewIndex", i.toString());
			plusTexts.get(i).setVisibility(View.INVISIBLE);
			videoViews.get(i).setVisibility(View.VISIBLE);
			nameTexts.get(i).setText(vsh.getStatusStr(f));
			videoPlayers.put(f.get("id"), new VideoPlayer( this, f.getId() ));
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

	private void onRecordStart(View v){
		Friend f = FriendFactory.getFriendFromFrame((View) v);
		getVideoPlayer(f).stop();
		if (videoRecorder.startRecording()) {
			Log.i(TAG, "onRecordStart: START RECORDING. view = " +f.get("firstName"));
		} else {
			Log.e(TAG, "onRecordStart: unable to start recording" + f.get("firstName"));
		}	
	}

	private void onRecordStop(View v){
		Friend f = FriendFactory.getFriendFromFrame(v);
		Log.i(TAG, "onRecordStop: STOP RECORDING. to " + f.get("firstName"));
		if ( videoRecorder.stopRecording(f) ){
			upload(v);
		} else {
			toast("Not sent. Too short.");
		}
	}

	private void onRecordCancel(View v){
		Friend f = FriendFactory.getFriendFromFrame(v);
		Log.i(TAG, "onRecordCancel: CANCEL RECORDING." + f.get("firstName"));
		videoRecorder.stopRecording(f);
		toast("Not Sent");
	}

	private void onPlayClick(View v) {
		Friend f = FriendFactory.getFriendFromFrame(v);
		Log.i(TAG, "onPlayClick" + f.get("firstName"));
		getVideoPlayer(f).click();
	}

	private void upload(View v) {
		Log.i(TAG, "upload");
		Friend f = FriendFactory.getFriendFromFrame(v);
		f.uploadVideo(this);
	}

	private VideoPlayer getVideoPlayer(Friend f){
		return videoPlayers.get(f.getId());
	}
	
	private void ensureListeners(){
		if (cameraPreviewFrame == null){
			addListeners();
		}
	}
	
	private void addListeners() {
		// Attache ViewSizeGetter
		cameraPreviewFrame = (FrameLayout) findViewById(R.id.camera_preview_frame);
		cameraPreviewFrame.addView(new ViewSizeGetter(this));
		
		// Reset button
		Button btnReset = (Button) findViewById(R.id.btnReset);
		btnReset.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				friendFactory.destroyAll();
				userFactory.destroyAll();
				finish();
			}
		});
		
		// Friend box clicks.
		for (ActiveModel am : FriendFactory.getFactoryInstance().instances){
			Friend friend = (Friend) am;

			Integer frameId = Integer.parseInt( friend.get("frameId") );
			Log.i(TAG, "Adding LongPressTouchHandler for frame" + frameId.toString());
			FrameLayout frame = (FrameLayout) findViewById(frameId);
			new LongpressTouchHandler(frame) {

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
