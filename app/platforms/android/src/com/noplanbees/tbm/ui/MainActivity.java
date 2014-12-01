package com.noplanbees.tbm.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;

import com.noplanbees.tbm.ActiveModelsHandler;
import com.noplanbees.tbm.BenchController;
import com.noplanbees.tbm.Boot;
import com.noplanbees.tbm.DataHolderService;
import com.noplanbees.tbm.GcmHandler;
import com.noplanbees.tbm.NotificationAlertManager;
import com.noplanbees.tbm.R;
import com.noplanbees.tbm.UserFactory;
import com.noplanbees.tbm.VersionHandler;

public class MainActivity extends Activity implements GridViewFragment.Callbacks {
	private final static String TAG = "MainActivity";

	private ProgressDialog pd;

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
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		
		body = (DrawerLayout)findViewById(R.id.drawer_layout);
		
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
		gcmHandler = new GcmHandler(this);
		benchController = new BenchController(this);
		versionHandler = new VersionHandler(this);
	};

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "onStart");
		pd = ProgressDialog.show(this, "Data", "retrieving data...");
		versionHandler.checkVersionCompatibility();		
		bindService(new Intent(this, DataHolderService.class), conn, Service.BIND_IMPORTANT);
		NotificationAlertManager.cancelNativeAlerts(this);
	}
	
	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG, "onStop");
		if (pd != null)
			pd.dismiss();
		
		unbindService(conn);
	}
	
	private void onLoadComplete() {
		Log.i(TAG, "onLoadComplete");
		// Note Boot.boot must complete successfully before we continue the home
		// activity.
		// Boot will start the registrationActivity and return false if needed.
		if (!Boot.boot(this)) {
			Log.i(TAG, "Finish HomeActivity");
			finish();
			return;
		} else {
			if (pd != null)
				pd.dismiss();

			mainFragment = (GridViewFragment) getFragmentManager().findFragmentByTag("main");
			if(mainFragment == null){
				mainFragment = new GridViewFragment();
				//mainFragment = new TestF();
				getFragmentManager().beginTransaction().add(R.id.content_frame, mainFragment, "main").commit();
			}
			
			if (gcmHandler.checkPlayServices()){
				gcmHandler.registerGcm();
			} else {
				Log.e(TAG, "No valid Google Play Services APK found.");
			}

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

	@SuppressWarnings("null")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.action_bench:
			//benchController.toggle();
			if(body.isDrawerOpen(Gravity.RIGHT))
				body.closeDrawers();
			else
				body.openDrawer(Gravity.RIGHT);
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

	@Override
	public void onFinish() {
		finish();
	}
	
}
