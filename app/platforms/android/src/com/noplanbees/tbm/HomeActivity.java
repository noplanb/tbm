package com.noplanbees.tbm;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class HomeActivity extends Activity implements CameraExceptionHandler{

	final String TAG = this.getClass().getSimpleName();
	final Float ASPECT = 240F/320F;

	public static HomeActivity instance;
	public Boolean isForeground = false;
	public VideoRecorder videoRecorder;
	public GcmHandler gcmHandler;
	
	private FriendFactory friendFactory;
	private UserFactory userFactory;

	private FrameLayout cameraPreviewFrame;
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
		super.onCreate(savedInstanceState);
		
		//Note Boot.boot must complete successfully before we continue the home activity. 
		//Boot will start the registrationActivity and return false if needed. 
		if (!Boot.boot(this)){
			Log.i(TAG,"Finish HomeActivity");
			finish();
			return;
		}

		// If activity was destroyed and activity was created due to an intent for videoReceived or videoStatus keep task in the background.
		Integer intentResult = new IntentHandler(this, getIntent()).handle(true);
		if (intentResult == IntentHandler.RESULT_RUN_IN_BACKGROUND){
			Log.i(TAG, "onCreate: moving activity to background.");
			moveTaskToBack(true);
			isForeground = false;
		} else if (intentResult == IntentHandler.RESULT_FINISH){
			Log.i(TAG, "onCreate: finishing.");
			finish();
		} else {			
			Log.i(TAG, "onCreate: moving activity to foreground.");
			isForeground = true;
		}
		
		setupWindow();
		setContentView(R.layout.home);
		lastState = "onCreate";
	}
	
	private void setupWindow(){
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
	}

	private void initModels() {
		Log.i(TAG, "initModels");
		instance = this;
		CameraManager.addCameraExceptionHandlerDelegate(this);
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
		Convenience.printOurTaskInfo(this);
		// NotificationAlertManager.alert(this, (Friend) FriendFactory.getFactoryInstance().find("3")); 
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
		if (isForeground){
			ensureModels();
			initViews();
			ensureListeners();
			runTests();
		}
		lastState = "onStart";
	}

	private Boolean screenIsOff(){
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		return !pm.isScreenOn();
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		Log.i(TAG, "onRestart: state");
		
		// To handle the fucked up Android (bug in my view) that when we are launched from the task manager 
		// as opposed to from any other vector we dont go through new onNewIntent. We transition directly 
		// from onStop() to onRestart(). In this case we need to set isForeground explicitly here.
		// We also have to handle another fucked up Android bug where if the screen is off it takes us through:
		// restart, start, resume, pause, then onNewIntent. 
		if (lastState.startsWith("onStop") && !screenIsOff()){
			Log.i(TAG, "onRestart: moving to foreground because last state was stop and screen was on.");
			isForeground = true;
		}
		
		if (isForeground && videoRecorder != null)
			videoRecorder.restore();
		lastState = "onRestart";
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.i(TAG, "onStop: state");
		isForeground = false;
		if (videoRecorder != null)
			videoRecorder.dispose(); // Probably redundant since the preview surface will have been destroyed by the time we get here.
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
		
		if (lastState.startsWith("onPause") && !screenIsOff()){
			Log.i(TAG, "onNewIntent: preliminary setting to foreground because came from pause and screen was on.");
			isForeground = true;
		} else {
			Log.i(TAG, "onNewIntent: preliminary setting to background because did not come from pause or screen was off.");
			isForeground = false;
		}

		Integer intentResult = new IntentHandler(this, intent).handle(false);
		if (intentResult == IntentHandler.RESULT_RUN_IN_BACKGROUND){
			Log.i(TAG, "onNewIntent: moving activity to background.");
			moveTaskToBack(true);
			isForeground = false;
		} else if (intentResult == IntentHandler.RESULT_FINISH){
			Log.i(TAG, "onNewIntent: finishing.");
			finish();
		} else {
			Log.i(TAG, "onNewIntent: moving activity to foreground.");
			isForeground = true;
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
		if (gcmHandler != null)
			gcmHandler.checkPlayServices();
		if (isForeground)
			NotificationAlertManager.cancelNativeAlerts(this);
	}

	@Override
	protected void onDestroy() {
		Log.i(TAG, "onDestroy: state");
		super.onDestroy();
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
		VideoPlayer.setAllVideoViews(videoViews);

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
		VideoPlayer.stopAll();
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

		Button btnCrash = (Button) findViewById(R.id.btnCrash);
		btnCrash.setOnClickListener(new View.OnClickListener() {
			@SuppressWarnings("null")
			@Override
			public void onClick(View v) {
				Camera c = null;
				c.cancelAutoFocus();
			}
		});

		// Friend box clicks.
		for (ActiveModel am : FriendFactory.getFactoryInstance().instances){
			Friend friend = (Friend) am;

			Integer frameId = Integer.parseInt( friend.get("frameId") );
			Log.i(TAG, "Adding LongPressTouchHandler for frame" + frameId.toString());
			FrameLayout frame = (FrameLayout) findViewById(frameId);
			new LongpressTouchHandler(this, frame) {

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

	// -------------------------------
	// CameraExceptionHandler delegate
	// -------------------------------
	@Override
	public void noCameraHardware() {	
		showCameraExceptionDialog("No Camera", "Your device does not seem to have a camera. This app requires a camera.", "Quit", "Try Again");
	}

	@Override
	public void noFrontCamera() {		
		showCameraExceptionDialog("No Front Camera", "Your device does not seem to have a front facing camera. This app requires a front facing camera.", "Quit", "Try Again");
	}

	@Override
	public void cameraInUseByOtherApplication() {
		showCameraExceptionDialog("Camera in Use", "Your camera seems to be in use by another application. Please close that app and try again. You may also need to restart your device.", "Quit", "Try Again");
	}

	@Override
	public void unableToSetCameraParams() {
	}

	@Override
	public void unableToFindAppropriateVideoSize() {		
	}

	private void showCameraExceptionDialog(String title, String message, String negativeButton, String positiveButton){
		AlertDialog.Builder builder = new AlertDialog.Builder(instance);
		builder.setTitle(title)
		.setMessage(message)
		.setNegativeButton(negativeButton, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				instance.finish();
			}
		})
		.setPositiveButton(positiveButton, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				videoRecorder.dispose();
				videoRecorder.restore();
			}
		})
		.create().show();
	}

};
