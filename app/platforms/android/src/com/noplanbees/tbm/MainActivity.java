package com.noplanbees.tbm;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.noplanbees.tbm.DataHolderService.LocalBinder;
import com.noplanbees.tbm.ui.GridViewFragment;

public class MainActivity extends Activity {
	private final static String TAG = "MainActivity";

	private ProgressDialog pd;
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

	private GcmHandler gcmHandler;
	private BenchController benchController;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		
		gcmHandler = new GcmHandler(this);

		benchController = new BenchController(this);

	};

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}
	
	@Override
	protected void onStart() {
		super.onStart();

		pd = ProgressDialog.show(this, "Data", "retrieving data...");
		bindService(new Intent(this, DataHolderService.class), conn, Service.BIND_IMPORTANT);

		NotificationAlertManager.cancelNativeAlerts(this);
	}
	@Override
	protected void onStop() {
		super.onStop();
		if (pd != null)
			pd.dismiss();
		
		unbindService(conn);
	}
	
	private void onLoadComplete() {
		// Note Boot.boot must complete successfully before we continue the home
		// activity.
		// Boot will start the registrationActivity and return false if needed.
		if (!Boot.boot(this)) {
			Log.i(TAG, "Finish HomeActivity");
			finish();
			return;
		} else {
			Fragment mainFragment = new GridViewFragment();
			getFragmentManager().beginTransaction().add(R.id.content_frame, mainFragment).commit();
			
			if (gcmHandler.checkPlayServices()){
				gcmHandler.registerGcm();
			} else {
				Log.e(TAG, "No valid Google Play Services APK found.");
			}
		}
		if (pd != null)
			pd.dismiss();

	}
}
