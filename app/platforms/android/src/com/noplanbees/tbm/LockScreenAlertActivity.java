package com.noplanbees.tbm;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class LockScreenAlertActivity extends Activity {

	private final String TAG = getClass().getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
		setupWindow();
		setContentView(R.layout.lock_screen_alert);
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.i(TAG, "onStart");
		setupViews();
		setupListeners();
	}

	@SuppressLint("NewApi")
	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume");
		KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
		Log.i(TAG, "isKeyGuardRestrictedInputMode=" + ((Boolean) km.inKeyguardRestrictedInputMode()).toString());
		Log.i(TAG, "isKeyguardLocked=" + ((Boolean) km.isKeyguardLocked()).toString());
		Log.i(TAG, "isKeyguardSecure=" + ((Boolean) km.isKeyguardSecure()).toString());
	}

	private void setupWindow(){
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);
		LayoutParams lp = getWindow().getAttributes();
		lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
		lp.type = WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG;
		getWindow().setAttributes(lp);
	}

	private void setupViews(){
		Intent i = this.getIntent();
		((TextView) this.findViewById(R.id.titleTextView)).setText(i.getStringExtra(NotificationAlertManager.TITLE_KEY));
		((TextView) this.findViewById(R.id.subtitleTextView)).setText(i.getStringExtra(NotificationAlertManager.SUB_TITLE_KEY));
		((ImageView) this.findViewById(R.id.logoImage)).setBackgroundResource(i.getIntExtra(NotificationAlertManager.SMALL_ICON_KEY, 0));
		Bitmap thumb = Convenience.bitmapWithPath(i.getStringExtra(NotificationAlertManager.LARGE_IMAGE_PATH_KEY));
		((ImageView) this.findViewById(R.id.thumbImage)).setImageBitmap(thumb);
	}


	private void startHomeActivity(){
		Intent i = new Intent(this, HomeActivity.class);
		startActivity(i);
		finish();
	}
	
	private void dismiss(){
		// If you really want to know. This moveTaskToBack is so that after dismissing this activity if the user decides to unlock his screen 
		// or if he doesnt have a security lock he doesnt see us on top and launched when he opens the phone.
		moveTaskToBack(true); 
		finish();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		dismiss();
	}
	
	private void setupListeners(){
		Button btnDismiss = (Button) findViewById(R.id.btnDismiss);
		btnDismiss.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		Button btnView = (Button) findViewById(R.id.btnView);
		btnView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startHomeActivity();
			}
		});
	}
}
