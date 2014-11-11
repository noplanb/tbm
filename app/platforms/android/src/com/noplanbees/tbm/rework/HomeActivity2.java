package com.noplanbees.tbm.rework;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.GridView;

import com.noplanbees.tbm.ActiveModelsHandler;
import com.noplanbees.tbm.DataHolderService;
import com.noplanbees.tbm.NewSurfaceView;
import com.noplanbees.tbm.R;
import com.noplanbees.tbm.DataHolderService.LocalBinder;
import com.noplanbees.tbm.R.id;
import com.noplanbees.tbm.R.layout;

public class HomeActivity2 extends Activity  {

	final String TAG = this.getClass().getSimpleName();

	private ActiveModelsHandler activeModelsHandler;

	private ServiceConnection conn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			activeModelsHandler = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			activeModelsHandler = ((LocalBinder) service).getDataManager();

			onLoadComplete();
		}
	};
	private ProgressDialog pd;
	private NewSurfaceView surfaceView;

	// --------------
	// App lifecycle
	// --------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.e(TAG, "onCreate state " + getFilesDir().getAbsolutePath());
		super.onCreate(savedInstanceState);

		setContentView(R.layout.home2);
		
		setupView();

		pd = ProgressDialog.show(this, "Data", "retrieving data...");
		bindService(new Intent(this, DataHolderService.class), conn, Service.BIND_IMPORTANT);
	}
	
	private void setupView() {
		GridView gridView = (GridView)findViewById(R.id.main_grid);
		
	}


	private void onLoadComplete() {

		pd.dismiss();
	}

};
