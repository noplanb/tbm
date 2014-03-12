package com.noplanbees.tbm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class HomeActivity extends Activity {



	final String TAG = this.getClass().getSimpleName();
	final Float ASPECT = 144F/176F;

	private FrameLayout cameraPreviewFrame;
	private VideoRecorder videoRecorder;
	private ArrayList<VideoView> videoViews = new ArrayList<VideoView>(8);
	@SuppressLint("UseSparseArrays")
	private HashMap<Integer, Integer> indexOfView = new HashMap<Integer, Integer>(8);
	private ArrayList<VideoPlayer> videoPlayers = new ArrayList<VideoPlayer>(8);
	private Context context;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
		context = getApplicationContext();
		setContentView(R.layout.home);
		if (checkPlayServices()){
			registerGCM();
			init_page();
		} else {
			Log.e(TAG, "No valid Google Play Services APK found.");
		}
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
		if (checkPlayServices()){
			getVideoRecorder();
		}
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
		videoViews.add((VideoView) this.findViewById(R.id.VideoView0));
		videoViews.add((VideoView) this.findViewById(R.id.VideoView1));
		videoViews.add((VideoView) this.findViewById(R.id.VideoView2));
		videoViews.add((VideoView) this.findViewById(R.id.VideoView3));
		videoViews.add((VideoView) this.findViewById(R.id.VideoView4));
		videoViews.add((VideoView) this.findViewById(R.id.VideoView5));
		videoViews.add((VideoView) this.findViewById(R.id.VideoView6));
		videoViews.add((VideoView) this.findViewById(R.id.VideoView7));
		for (int i=0; i<8; i++){
			indexOfView.put(videoViews.get(i).getId(), i);
			videoPlayers.add( i, new VideoPlayer( this, videoViews.get(i) ) );
		}
	}

	private void setVideoViewHeights(int width, int height) {
		int h = (int) ((float) width / ASPECT);
		LayoutParams lp = cameraPreviewFrame.getLayoutParams();
		lp.height = h;
		cameraPreviewFrame.setLayoutParams(lp);
		Log.i(TAG, String.format("setVideoViewHeights %d  %d", height, lp.height));

		lp = videoViews.get(0).getLayoutParams();
		lp.height = h;
		for (VideoView vv: videoViews)
			vv.setLayoutParams(lp);
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

	// -----------
	// GCM RELATED
	// -----------

	//	GCM attributes
	public static final String EXTRA_MESSAGE = "message";
	public static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";

	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	private String SENDER_ID = "550639704405";
	private GoogleCloudMessaging gcm;
	private AtomicInteger msgId = new AtomicInteger();
	private SharedPreferences prefs;
	private String regid;



	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, this,PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Log.i(TAG, "This device is not supported.");
				finish();
			}
			return false;
		}
		return true;
	}

	private void registerGCM(){
		gcm = GoogleCloudMessaging.getInstance(this);
		regid = getRegistrationId(context);

		if (regid.isEmpty()) {
			registerInBackground();
		}
	}

	/**
	 * Gets the current registration ID for application on GCM service.
	 * <p>
	 * If result is empty, the app needs to register.
	 *
	 * @return registration ID, or empty string if there is no existing
	 *         registration ID.
	 */
	private String getRegistrationId(Context context) {
		final SharedPreferences prefs = getGCMPreferences(context);
		String registrationId = prefs.getString(PROPERTY_REG_ID, "");
		if (registrationId.isEmpty()) {
			Log.i(TAG, "Registration not found.");
			return "";
		}
		// Check if app was updated; if so, it must clear the registration ID
		// since the existing regID is not guaranteed to work with the new
		// app version.
		int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
		int currentVersion = getAppVersion(context);
		if (registeredVersion != currentVersion) {
			Log.i(TAG, "App version changed.");
			return "";
		}
		return registrationId;
	}

	/**
	 * @return Application's {@code SharedPreferences}.
	 */
	private SharedPreferences getGCMPreferences(Context context) {
		// This sample app persists the registration ID in shared preferences, but
		// how you store the regID in your app is up to you.
		return getSharedPreferences(this.getClass().getSimpleName(),
				Context.MODE_PRIVATE);
	}

	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	private static int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration ID and app versionCode in the application's
	 * shared preferences.
	 */
	private void registerInBackground() {
		(new RIBAsync()).execute(null, null, null);
	}


	private class RIBAsync extends AsyncTask<Void, Void, Void>{
		@Override
		protected Void doInBackground(Void... params) {
			try {
				if (gcm == null) {
					gcm = GoogleCloudMessaging.getInstance(context);
				}
				regid = gcm.register(SENDER_ID);
				Log.i(TAG, "Device registered, registration ID=" + regid);

				// Need to send registration id to our server over HTTP.
				sendRegistrationIdToBackend();

				// For this demo: we don't need to send it because the device
				// will send upstream messages to a server that echo back the
				// message using the 'from' address in the message.

				// Persist the regID - no need to register again.
				storeRegistrationId(context, regid);
			} catch (IOException ex) {
				// If there is an error wait for the user to turn off the app to try again.
				Log.e(TAG, "Error :" + ex.getMessage());
			}
			return null;
		}
	}
	
	private void sendRegistrationIdToBackend() {
	    // GARF Need to implement this.
	}
	
	/**
	 * Stores the registration ID and app versionCode in the application's
	 * {@code SharedPreferences}.
	 *
	 * @param context application's context.
	 * @param regId registration ID
	 */
	private void storeRegistrationId(Context context, String regId) {
	    final SharedPreferences prefs = getGCMPreferences(context);
	    int appVersion = getAppVersion(context);
	    Log.i(TAG, "Saving regId on app version " + appVersion);
	    SharedPreferences.Editor editor = prefs.edit();
	    editor.putString(PROPERTY_REG_ID, regId);
	    editor.putInt(PROPERTY_APP_VERSION, appVersion);
	    editor.commit();
	}


};
