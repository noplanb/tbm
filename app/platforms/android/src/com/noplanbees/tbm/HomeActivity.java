package com.noplanbees.tbm;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
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
	final Float ASPECT = 144F/176F;

	public static HomeActivity instance;

	private FriendFactory friendFactory;
	private UserFactory userFactory;

	private FrameLayout cameraPreviewFrame;
	public VideoRecorder videoRecorder;
	private GcmHandler gcmHandler;

	private ArrayList<VideoView> videoViews = new ArrayList<VideoView>(8);
	private ArrayList<ImageView> thumbViews = new ArrayList<ImageView>(8);
	private ArrayList<TextView> plusTexts = new ArrayList<TextView>(8);
	private ArrayList<FrameLayout> frames = new ArrayList<FrameLayout>(8);
	private ArrayList<TextView> nameTexts = new ArrayList<TextView>(8);

	private HashMap<String, VideoPlayer> videoPlayers = new HashMap<String, VideoPlayer>(8);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
		
		// If activity was destroyed and we got an intent due to a new video download
		// don't start up the activity. Send a notification instead and let the user 
		// click on the notification if he wants to start tbm.
		if (intentWasForNewVideo()){
			sendNotification( getFriendFromIntent(this.getIntent()) );
			Log.i(TAG, "aborting home_activity becuase intent was for new video");
			finish();
			return;
		}

		//Note Boot.boot must complete successfully before we continue the home activity. 
		//Boot will start the registrationActivity and return false if needed. 
		if (!Boot.boot(this)){
			Log.i(TAG,"Finish HomeActivity");
			finish();
			return;
		}
		
		setContentView(R.layout.home);

		initModels();
		init_page();
		runTests();
	}

	private boolean intentWasForNewVideo() {
		Boolean r = false;
		Intent i = this.getIntent();
		Bundle extras = i.getExtras();
		if (extras != null){
			String type = extras.getString("type");
			r = type != null && type.startsWith("new_video");
		}
		return r;
	}

	private void initModels() {
		instance = this;
		videoRecorder = new VideoRecorder(this);
		gcmHandler = new GcmHandler(this);
		friendFactory = FriendFactory.getFactoryInstance();
		userFactory = UserFactory.getFactoryInstance();
	}

	private void runTests() {
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
		Log.i(TAG, "onStart:");
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.i(TAG, "onStop:");
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "onPause");
		videoRecorder.dispose();
		ActiveModelsHandler.saveAll();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.i(TAG, "onNewIntent");
		ActiveModelsHandler.retrieveFriend();
		Friend friend = getFriendFromIntent(intent);
		if (friend != null){
			getVideoPlayerForFriend(friend).refreshThumb();
			playNotificationTone();
		}
	}

	private VideoPlayer getVideoPlayerForFriend(Friend friend) {
		return videoPlayers.get(friend.getId());
	}

	private Friend getFriendFromIntent(Intent intent) {
		Friend f = null;
		Bundle extras = intent.getExtras();
		if (extras != null){
			String friendId = extras.getString("friendId");
			f = (Friend) FriendFactory.getFactoryInstance().find(friendId);
		}
		return f;
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume");
		videoRecorder.restore();
		if (!gcmHandler.checkPlayServices()){
			Log.e(TAG, "onResume: checkPlayServices = false");
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy");
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
			
			plusTexts.get(i).setVisibility(View.INVISIBLE);
			videoViews.get(i).setVisibility(View.VISIBLE);
			nameTexts.get(i).setText(f.get("firstName"));
			
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
	}

	private void onPlayClick(View v) {
		Friend f = FriendFactory.getFriendFromFrame(v);
		Log.i(TAG, "onPlayClick" + f.get("firstName"));
		videoPlayers.get(f.get("id")).click();
	}

	private void upload(View v) {
		Log.i(TAG, "upload");
		Friend f = FriendFactory.getFriendFromFrame(v);
		f.uploadVideo(this);
	}

	private void addListeners() {

		Button btnReset = (Button) findViewById(R.id.btnReset);
		btnReset.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				friendFactory.destroyAll();
				userFactory.destroyAll();
				Boot.boot(HomeActivity.instance);
			}
		});

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
	
    private void sendNotification(Friend friend) {
        final int NOTIFICATION_ID = 1;
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, HomeActivity.class), 0);
        
        String msg = "Message from " + friend.get("firstName") + "!";
        
        Bitmap sqThumbBmp = friend.sqThumbBitmap();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        .setLargeIcon(sqThumbBmp)
        .setSmallIcon(R.drawable.ic_stat_gcm)
        .setContentTitle(msg)
        .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
        .setContentText("Three By Me");

        Log.i(TAG, "sendNotification: Sending notification");
        mBuilder.setContentIntent(contentIntent);
        notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
    
    private void playNotificationTone(){
    	Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
        r.play();
    }
};
