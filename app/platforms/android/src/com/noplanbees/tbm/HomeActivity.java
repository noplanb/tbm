package com.noplanbees.tbm;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources.Theme;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.noplanbees.tbm.CameraManager.CameraExceptionHandler;
import com.noplanbees.tbm.DataHolderService.LocalBinder;
import com.noplanbees.tbm.Friend.VideoStatusChangedCallback;
import com.noplanbees.tbm.GridManager.GridEventNotificationDelegate;
import com.noplanbees.tbm.VideoRecorder.VideoRecorderExceptionHandler;

public class HomeActivity extends Activity implements CameraExceptionHandler, VideoStatusChangedCallback,
		VideoRecorderExceptionHandler, GridEventNotificationDelegate {

	final String TAG = this.getClass().getSimpleName();
	final Float ASPECT = 240F / 320F;

	public LongpressTouchHandler longpressTouchHandler;
	public VideoRecorder videoRecorder;
	public GcmHandler gcmHandler;
	public VersionHandler versionHandler;
	private BenchController benchController;
	private SmsStatsHandler smsStatsHandler;

	private FrameLayout cameraPreviewFrame;

	private ArrayList<VideoView> videoViews = new ArrayList<VideoView>(8);
	private ArrayList<ImageView> thumbViews = new ArrayList<ImageView>(8);
	private ArrayList<TextView> plusTexts = new ArrayList<TextView>(8);
	private ArrayList<FrameLayout> frames = new ArrayList<FrameLayout>(8);
	private ArrayList<TextView> nameTexts = new ArrayList<TextView>(8);
	private ArrayList<VideoPlayer> videoPlayers = new ArrayList<VideoPlayer>(8);

	private Handler handler = new Handler(Looper.getMainLooper());
	
	private ActiveModelsHandler activeModelsHandler;

	private ServiceConnection conn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			activeModelsHandler = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			activeModelsHandler = ((LocalBinder) service).getActiveModelsHandler();

			onLoadComplete();
		}
	};

	private ProgressDialog pd;
	private VideoPlayer videoPlayer;

	// --------------
	// App lifecycle
	// --------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.e(TAG, "onCreate state " + getFilesDir().getAbsolutePath());
		super.onCreate(savedInstanceState);
		setupWindow();
		setContentView(R.layout.home);
		
//		CameraManager.addExceptionHandlerDelegate(this);
//		VideoRecorder.addExceptionHandlerDelegate(this);
//		videoRecorder = new VideoRecorder(this);
//		videoRecorder.registerListeners();
		
		gcmHandler = new GcmHandler(this);
		benchController = new BenchController(this);
		
		videoPlayer = new VideoPlayer(this);
	}

	@Override
	protected void onApplyThemeResource(Theme theme, int resid, boolean first) {
		Log.i(TAG, "onApplyThemeResource" + theme.toString() + " resid=" + resid);
		super.onApplyThemeResource(theme, resid, first);
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Log.e(TAG, "onStart: state");

		pd = ProgressDialog.show(this, "Data", "retrieving data...");
		bindService(new Intent(this, DataHolderService.class), conn, Service.BIND_IMPORTANT);

		NotificationAlertManager.cancelNativeAlerts(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.e(TAG, "onResume: state");
		// setupVersionHandler onResume because may cause a dialog which would
		// crash the app before onResume.
		setupVersionHandler();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.e(TAG, "onPause: state");
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (pd != null)
			pd.dismiss();
		
		unbindService(conn);
		
		Log.e(TAG, "onStop: state");
		abortAnyRecording(); // really as no effect when called here since the
								// surfaces will have been destroyed and the
								// recording already stopped.

//		if (longpressTouchHandler != null)
//			longpressTouchHandler.disable(true);

		videoPlayer.release(this);
	}

	@Override
	protected void onDestroy() {
		Log.e(TAG, "onDestroy: state");
		super.onDestroy();
	}

	public void onLoadComplete() {
		// Note Boot.boot must complete successfully before we continue the home
		// activity.
		// Boot will start the registrationActivity and return false if needed.
		if (!Boot.boot(this)) {
			Log.i(TAG, "Finish HomeActivity");
			finish();
			return;
		} else {

			setupGrid();
			getVideoViewsAndPlayers();
			initViews();
			activeModelsHandler.getFf().addVideoStatusChangedCallbackDelegate(this);
			runTests();
			setupLongPressTouchHandler();
			ensureListeners();

			//Boot.initGCM(this);

			gcmHandler.checkPlayServices();

			handleIntentAction();
			
			benchController.onDataLoaded();

		}

		pd.dismiss();
	}

	// -------------------
	// HandleIntentAction
	// -------------------
	private void handleIntentAction() {
		// Right now the only actions are
		// 1) to automatically start playing the appropriate video if the user
		// got here by clicking a notification.
		// 2) to notify the user if there was a problem sending Sms invite to a
		// friend. (Not used as decided this is unnecessarily disruptive)
		Intent currentIntent = getIntent();
		Log.i(TAG, "handleIntentAction: " + currentIntent .toString());
		if (currentIntent == null) {
			Log.i(TAG, "handleIntentAction: no intent. Exiting.");
			return;
		}
		String action = currentIntent.getAction();
		Uri data = currentIntent.getData();
		if (action == null || data == null) {
			Log.i(TAG, "handleIntentAction: no ation or data. Exiting.");
			return;
		}

		String friendId = currentIntent.getData().getQueryParameter(IntentHandler.IntentParamKeys.FRIEND_ID);
		if (action == null || friendId == null) {
			Log.i(TAG, "handleIntentAction: no friendId or action. Exiting." + currentIntent.toString());
			return;
		}

		if (action.equals(IntentHandler.IntentActions.PLAY_VIDEO) && !NotificationAlertManager.screenIsLocked(this)) {
			currentIntent.setAction(IntentHandler.IntentActions.NONE);
			videoPlayer.play(friendId);
		}

		// Not used as I decided pending intent coming back from sending sms is
		// to disruptive. Just assume
		// sms's sent go through.
		if (action.equals(IntentHandler.IntentActions.SMS_RESULT) && !NotificationAlertManager.screenIsLocked(this)) {
			currentIntent.setAction(IntentHandler.IntentActions.NONE);
			Log.i(TAG, currentIntent.toString());
		}
	}

	// ---------------
	// Initialization
	// ---------------
	private void setupWindow() {
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	private void setupGrid() {
		GridManager.setGridEventNotificationDelegate(this);

		if (activeModelsHandler.getGf().all().size() == 8)
			return;

		activeModelsHandler.getGf().destroyAll(this);
		ArrayList<Friend> allFriends = activeModelsHandler.getFf().all();
		for (Integer i = 0; i < 8; i++) {
			GridElement g = activeModelsHandler.getGf().makeInstance(this);
			if (i < allFriends.size()) {
				Friend f = allFriends.get(i);
				g.set(GridElement.Attributes.FRIEND_ID, f.getId());
			}
		}
	}

	private void runTests() {
		// Log.e(TAG, getFilesDir().getAbsolutePath());
		// Convenience.printOurTaskInfo(this);
		// NotificationAlertManager.alert(this, (Friend)
		// FriendFactory.getFactoryInstance().find("3"));
		// new CamcorderHelper();
		// testService();
		// ConfigTest.run();
		// FriendTest.run();
		// new FileDownloadDeprecated.BgDownload().execute();
		// Friend f = (Friend)
		// friendFactory.findWhere(Friend.Attributes.FIRST_NAME, "Farhad");
		// new
		// FileDownloadDeprecated.BgDownloadFromFriendId().execute(f.getId());
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

//		Integer i = 0;
//		for (GridElement ge : activeModelsHandler.getGf().all()) {
//			ge.frame = frames.get(i);
//			ge.videoView = videoViews.get(i);
//			ge.thumbView = thumbViews.get(i);
//			ge.nameText = nameTexts.get(i);
//			//ge.videoPlayer = new VideoPlayer(ge);
//			i++;
//		}
	}

	private void initViews() {
		Integer i = 0;
		for (GridElement ge : activeModelsHandler.getGf().all()) {
			Friend f = ge.friend();
			if (f != null) {
				plusTexts.get(i).setVisibility(View.INVISIBLE);
				Log.i(TAG, plusTexts.get(i).getText().toString());
				Log.i(TAG, plusTexts.get(i).toString());
				videoViews.get(i).setVisibility(View.VISIBLE);
				nameTexts.get(i).setText(f.getStatusString());
			} else {
				plusTexts.get(i).setVisibility(View.VISIBLE);
				videoViews.get(i).setVisibility(View.INVISIBLE);
				nameTexts.get(i).setText("");
			}
			i++;
		}
		//videoPlayer.showAllThumbs();
	}

	private void setVideoViewHeights(int width, int height) {
		int h = (int) ((float) width / ASPECT);
		ViewGroup.LayoutParams lp = cameraPreviewFrame.getLayoutParams();
		lp.height = h;
		cameraPreviewFrame.setLayoutParams(lp);
		Log.i(TAG, String.format("setVideoViewHeights %d  %d", height, lp.height));

		lp = frames.get(0).getLayoutParams();
		lp.height = h;
		for (FrameLayout f : frames)
			f.setLayoutParams(lp);
	}

	private void setupLongPressTouchHandler() {
		longpressTouchHandler = new LongpressTouchHandler(this, findViewById(R.id.homeTable)) {
			@Override
			public void startLongpress(View v) {
				onRecordStart(v);
			}

			@Override
			public void endLongpress(View v) {
				onRecordStop(v);
			}

			@Override
			public void click(View v) {
				onPlayClick(v);
			}

			@Override
			public void bigMove(View v) {
				onRecordCancel();
			}

			@Override
			public void abort(View v) {
				onRecordCancel();
			}

			@Override
			public void flingRight() {
				//benchController.hide();
			}

			@Override
			public void flingLeft() {
				//benchController.show();
			}

			@Override
			public void flingUp() {
				Log.i(TAG, "flingUp");
			}

			@Override
			public void flingDown() {
				Log.i(TAG, "flingDown");
			}
		};

//		longpressTouchHandler.enable();

		// Add gridElement boxes as valid targets.
		for (GridElement ge : activeModelsHandler.getGf().all()) {
			//longpressTouchHandler.addTargetView(ge.frame);
		}
	}

	// -------
	// Events
	// -------
	private void onRecordStart(View v) {
		videoPlayer.stop();
		hideAllCoveringViews();
//		GridElement ge = activeModelsHandler.getGf().getGridElementByFriendId(v);
//		if (!ge.hasFriend())
//			return;
//
//		Friend f = ge.friend();
//		GridManager.rankingActionOccurred(f);
//		if (videoRecorder.startRecording(f)) {
//			Log.i(TAG, "onRecordStart: START RECORDING: " + f.get(Friend.Attributes.FIRST_NAME));
//		} else {
//			Log.e(TAG, "onRecordStart: unable to start recording" + f.get(Friend.Attributes.FIRST_NAME));
//			longpressTouchHandler.cancelGesture(true);
//		}
	}

	private void onRecordCancel() {
		// Different from abortAnyRecording becuase we always toast here.
		videoRecorder.stopRecording();
		toast("Not sent.");
	}

	private void onRecordStop(View v) {
//		GridElement ge = activeModelsHandler.getGf().getGridElementByFriendId(v);
//		if (!ge.hasFriend())
//			return;
//
//		Friend f = ge.friend();
//		Log.i(TAG, "onRecordStop: STOP RECORDING. to " + f.get(Friend.Attributes.FIRST_NAME));
//		if (videoRecorder.stopRecording()) {
//			f.setAndNotifyOutgoingVideoStatus(Friend.OutgoingVideoStatus.NEW);
//			f.uploadVideo();
//		}
	}

	private void onPlayClick(View v) {
//		hideAllCoveringViews();
//		GridElement ge = activeModelsHandler.getGf().getGridElementByFriendId(v);
//		if (!ge.hasFriend())
//			return;
//
//		Friend f = ge.friend();
//		Log.i(TAG, "onPlayClick" + f.get(Friend.Attributes.FIRST_NAME));
//		GridManager.rankingActionOccurred(f);
		//ge.videoPlayer.click();
	}

	@Override
	public void onVideoStatusChanged(final Friend friend) {
		handler.post(new Runnable(){
			@Override
			public void run() {
				GridManager.moveFriendToGrid(HomeActivity.this,friend);

				GridElement ge = activeModelsHandler.getGf().findWithFriendId(friend.getId());
				if (ge != null){
//					ge.nameText.setText(ge.friend().getStatusString());
					//ge.videoPlayer.refreshThumb();
				}
			}
		});
	}

	// Since the call for this had to be moved from onPause to onStop this
	// really never has any effect since
	// the surfaces disappear by that time and the mediaRecorder in
	// videoRecorder is disposed.
	private void abortAnyRecording() {
		Log.i(TAG, "abortAnyRecording");
		if (videoRecorder != null)
			videoRecorder.stopRecording();
	}

	// ----------------
	// Setup Listeners
	// ----------------
	private void ensureListeners() {
		if (cameraPreviewFrame == null) {
			addListeners();
		}
	}

	private void addListeners() {
		// Attache ViewSizeGetter
		cameraPreviewFrame = (FrameLayout) findViewById(R.id.camera_preview_frame);
		cameraPreviewFrame.addView(new ViewSizeGetter(this));

		for (TextView p : plusTexts) {
			p.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					//benchController.show();
				}
			});
		}
	}

	private class ViewSizeGetter extends View {
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

	private void toast(String msg) {
		Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}

	// -------------------------------
	// CameraExceptionHandler delegate
	// -------------------------------
	@Override
	public void noCameraHardware() {
		showCameraExceptionDialog("No Camera",
				"Your device does not seem to have a camera. This app requires a camera.", "Quit", "Try Again");
	}

	@Override
	public void noFrontCamera() {
		showCameraExceptionDialog("No Front Camera",
				"Your device does not seem to have a front facing camera. This app requires a front facing camera.",
				"Quit", "Try Again");
	}

	@Override
	public void cameraInUseByOtherApplication() {
		showCameraExceptionDialog(
				"Camera in Use",
				"Your camera seems to be in use by another application. Please close that app and try again. You may also need to restart your device.",
				"Quit", "Try Again");
	}

	@Override
	public void unableToSetCameraParams() {
	}

	@Override
	public void unableToFindAppropriateVideoSize() {
	}

	private void showCameraExceptionDialog(String title, String message, String negativeButton, String positiveButton) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title).setMessage(message)
				.setNegativeButton(negativeButton, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						finish();
					}
				}).setPositiveButton(positiveButton, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						//videoRecorder.dispose();
						//videoRecorder.restore();
						// ????
					}
				});
		AlertDialog alertDialog = builder.create();
		alertDialog.setCanceledOnTouchOutside(false);
		alertDialog.show();
	}

	// ---------------------------------------
	// Video Recorder ExceptionHandler delegate
	// ----------------------------------------
	@Override
	public void unableToSetPrievew() {
		toast("unable to set preview");
	}

	@Override
	public void unableToPrepareMediaRecorder() {
		toast("Unable to prepare MediaRecorder");
	}

	@Override
	public void recordingAborted() {
		toast("Recording Aborted due to Release before Stop.");
	}

	@Override
	public void recordingTooShort() {
		toast("Not sent. Too short.");
	}

	@Override
	public void illegalStateOnStart() {
		toast("Runntime exception on MediaRecorder.start. Quitting app.");
		finish();
	}

	@Override
	public void runntimeErrorOnStart() {
		toast("Unable to start recording. Try again.");
	}

	// -----------------
	// Version Handling
	// -----------------
	private void setupVersionHandler() {
		// Only check the version when the app is started fresh.
		if (versionHandler == null)
			versionHandler = new VersionHandler(this);
	}

	// ------------
	// Grid Events
	// ------------
	@Override
	public void gridDidChange() {
		initViews();
	}

	// ----------
	// ActionBar
	// ----------

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.home_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@SuppressWarnings("null")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.action_bench:
			//benchController.toggle();
			return true;
		case R.id.action_get_contacts:
			UserFactory.current_user().getCountryCode();
			return true;
		case R.id.action_get_sms:
			benchController.callSms();
			return true;
		case R.id.action_reset:
			ActiveModelsHandler.getInstance(this).destroyAll();
			finish();
			return true;
		case R.id.action_crash:
			throw new NullPointerException("simulate exception");
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// -----------------------------------------
	// Views that may be covering the home page
	// -----------------------------------------
	private void hideAllCoveringViews() {
		//benchController.hideAllViews();
	}

	//--------------------------
	// Handle user launch intent 
	//--------------------------
	private void handleUserLaunchIntent(Context context) {
		Log.i(TAG, "handleUserLaunchIntent");
		FileUploadService.restartTransfersPendingRetry(context);
		FileDownloadService.restartTransfersPendingRetry(context);
		NotificationAlertManager.cancelNativeAlerts(context);
		(new Poller(context)).pollAll();
	}
	
}
