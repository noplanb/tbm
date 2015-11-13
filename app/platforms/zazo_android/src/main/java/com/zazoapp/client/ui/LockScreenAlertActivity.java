package com.zazoapp.client.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.zazoapp.client.R;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.notification.NotificationAlertManager;

public class LockScreenAlertActivity extends Activity {

	private static final String TAG = LockScreenAlertActivity.class.getSimpleName();

    private boolean isStopped = true;
	//-------------------
	// Activity lifecycle
	//-------------------
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
		setupWindow();
		setContentView(R.layout.lock_screen_alert);
		setupViews(getIntent());
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.i(TAG, "onRestart");
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.i(TAG, "onNewIntent");
        setIntent(intent);
        if (isStopped) {
            recreate();
        } else {
            setupViews(intent);
        }
    }

	@Override
	protected void onPause() {
		Log.i(TAG, "onPause");
		super.onPause();
	}

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");
        isStopped = true;
    }

	@Override
	protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
	}

	@Override
	protected void onStart() {
		super.onStart();
        isStopped = false;
        Log.i(TAG, "onStart");
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

	private void setupViews(Intent i){
		((TextView) this.findViewById(R.id.titleTextView)).setText(i.getStringExtra(NotificationAlertManager.TITLE_KEY));
		((TextView) this.findViewById(R.id.subtitleTextView)).setText(i.getStringExtra(NotificationAlertManager.SUB_TITLE_KEY));
		((ImageView) this.findViewById(R.id.logoImage)).setBackgroundResource(i.getIntExtra(NotificationAlertManager.SMALL_ICON_KEY, 0));
		Friend friend = FriendFactory.getFactoryInstance().getFriendFromIntent(i);
		ImageView thumbImage = (ImageView) this.findViewById(R.id.thumbImage);
		if (friend.thumbExists()) {
			thumbImage.setImageBitmap(friend.thumbBitmap());
		} else {
			thumbImage.setImageResource(R.drawable.ic_no_pic_z);
		}
	}


	private void startHomeActivity(){
		Intent i = new Intent(this, MainActivity.class);
		i.setAction(getIntent().getAction());
		i.setData(getIntent().getData());
		Log.i(TAG, "startHomeActivity: ");
		// FLAG_DISMISS_KEYGUARD is put here rather than in setupWindow becuase of an Android bug I found and reported: 
		// SingleInstance type activity with layoutParams FLAG_DISMISS_KEYGUARD does not receive onNewIntent callback.
		// https://code.google.com/p/android/issues/detail?id=72242&thanks=72242&ts=1403540783
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
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
