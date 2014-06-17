package com.noplanbees.tbm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class LockScreenAlertActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupWindow();
		setupViews();
		setContentView(R.layout.lock_screen_alert);
		setupListeners();
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
	
	private void setupListeners(){
		Button btnDismiss = (Button) findViewById(R.id.btnDismiss);
		btnDismiss.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
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
