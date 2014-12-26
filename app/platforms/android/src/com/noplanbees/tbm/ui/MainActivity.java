package com.noplanbees.tbm.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageView;

import com.noplanbees.tbm.BenchController;
import com.noplanbees.tbm.BenchObject;
import com.noplanbees.tbm.Contact;
import com.noplanbees.tbm.DataHolderService;
import com.noplanbees.tbm.Friend;
import com.noplanbees.tbm.GcmHandler;
import com.noplanbees.tbm.InviteManager;
import com.noplanbees.tbm.NotificationAlertManager;
import com.noplanbees.tbm.R;
import com.noplanbees.tbm.RegisterActivity;
import com.noplanbees.tbm.User;
import com.noplanbees.tbm.VersionHandler;
import com.noplanbees.tbm.ui.dialogs.ActionInfoDialogFragment;
import com.noplanbees.tbm.ui.dialogs.InfoDialogFragment;

public class MainActivity extends Activity implements GridViewFragment.Callbacks, 
BenchController.Callbacks, ActionInfoDialogFragment.Callbacks{
	private final static String TAG = "MainActivity";
	
	public static final int CONNECTED_DIALOG = 0;
	public static final int NUDGE_DIALOG = 1;
	public static final int SMS_DIALOG = 2;
	public static final int SENDLINK_DIALOG = 3;

	private ServiceConnection conn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			onLoadComplete();
		}
	};

	private GcmHandler gcmHandler;
	private BenchController benchController;
	private VersionHandler versionHandler;
	private Fragment mainFragment;
	private DrawerLayout body;
	private InviteManager inviteManager;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		
		body = (DrawerLayout)findViewById(R.id.drawer_layout);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
		gcmHandler = new GcmHandler(this);
		versionHandler = new VersionHandler(this);
		
		inviteManager = InviteManager.getInstance(this);
		
		setupActionBar();
	}

	private void setupActionBar() {
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setDisplayUseLogoEnabled(false);
		actionBar.setDisplayShowTitleEnabled(false);
		getActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent))); 
		ImageView v = new ImageView(this);
		v.setImageResource(R.drawable.zazo_type);
		actionBar.setCustomView(v);
	};

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		versionHandler.checkVersionCompatibility();		
		bindService(new Intent(this, DataHolderService.class), conn, Service.BIND_IMPORTANT);
		NotificationAlertManager.cancelNativeAlerts(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		unbindService(conn);
	}
	
	private void onLoadComplete() {
		Log.i(TAG, "onLoadComplete");

		if (!User.isRegistered(this)) {
			Log.i(TAG, "Not registered. Starting RegisterActivty");
			Intent i = new Intent(this, RegisterActivity.class);
			startActivity(i);
			finish();
			return;
		} else {

			mainFragment = (GridViewFragment) getFragmentManager().findFragmentByTag("main");
			if(mainFragment == null){
				mainFragment = new GridViewFragment();
				getFragmentManager().beginTransaction().add(R.id.content_frame, mainFragment, "main").commit();
			}
			
			if (gcmHandler.checkPlayServices()){
				gcmHandler.registerGcm();
			} else {
				Log.e(TAG, "No valid Google Play Services APK found.");
			}

			benchController = new BenchController(this);
			benchController.onDataLoaded();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.home_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.action_bench:
			if(body.isDrawerOpen(Gravity.RIGHT))
				body.closeDrawers();
			else
				body.openDrawer(Gravity.RIGHT);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onFinish() {
		finish();
	}

	@Override
	public void onBenchRequest(int pos) {
		body.openDrawer(Gravity.RIGHT);
	}

	@Override
	public void onHide() {
		body.closeDrawers();
	}

	@Override
	public void showNoValidPhonesDialog(Contact contact) {
		InfoDialogFragment info = new InfoDialogFragment();
		Bundle args = new Bundle();
		args.putString(InfoDialogFragment.TITLE, "No Mobile Number");
		args.putString(InfoDialogFragment.MSG, "I could not find a valid mobile number for " + contact.getDisplayName()
				+ ".\n\nPlease add a mobile number for " + contact.getFirstName()
				+ " in your device contacts and try again.");
		info.setArguments(args );
		info.show(getFragmentManager(), null);
	}

	@Override
	public void onActionClicked(int id) {
		switch(id){
		case CONNECTED_DIALOG:
			inviteManager.moveFriendToGrid();
			break;
		case NUDGE_DIALOG:
			inviteManager.showSms();
			break;
		case SMS_DIALOG: 
			inviteManager.showSms();
			break;
		case SENDLINK_DIALOG:
			inviteManager.sendLink();
			break;
		}
	}

	@Override
	public void inviteFriend(BenchObject bo) {
		inviteManager.invite(bo);
	}

	@Override
	public void onNudgeFriend(Friend f) {
		inviteManager.nudge(f);
	}

	@Override
	public void showRecordDialog() {
		InfoDialogFragment info = new InfoDialogFragment();
		Bundle args = new Bundle();
		args.putString(InfoDialogFragment.TITLE, "Hold to Record");
		args.putString(InfoDialogFragment.MSG, "Press and hold the RECORD button to record.");
		info.setArguments(args );
		info.show(getFragmentManager(), null);
	}
}
