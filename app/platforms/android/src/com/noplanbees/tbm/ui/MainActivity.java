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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageView;
import com.noplanbees.tbm.DataHolderService;
import com.noplanbees.tbm.R;
import com.noplanbees.tbm.VersionHandler;
import com.noplanbees.tbm.bench.BenchController;
import com.noplanbees.tbm.bench.BenchViewManager;
import com.noplanbees.tbm.bench.InviteManager;
import com.noplanbees.tbm.bench.InviteManager.InviteDialogListener;
import com.noplanbees.tbm.dispatch.Dispatch;
import com.noplanbees.tbm.model.User;
import com.noplanbees.tbm.network.aws.S3CredentialsGetter;
import com.noplanbees.tbm.notification.NotificationAlertManager;
import com.noplanbees.tbm.notification.gcm.GcmHandler;
import com.noplanbees.tbm.ui.dialogs.ActionInfoDialogFragment.ActionInfoDialogListener;
import com.noplanbees.tbm.utilities.DialogShower;

public class MainActivity extends Activity implements ActionInfoDialogListener, VersionHandler.Callback,
        InviteDialogListener, BenchViewManager.Provider {

    private static final String TAG = MainActivity.class.getSimpleName();

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
	private InviteManager inviteManager;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		gcmHandler = new GcmHandler(this);
		versionHandler = new VersionHandler(this);

        inviteManager = InviteManager.getInstance();
        inviteManager.init(this, this);
        benchController = new BenchController(this);

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
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
        bindService(new Intent(this, DataHolderService.class), conn, Service.BIND_IMPORTANT);
		versionHandler.checkVersionCompatibility();
		NotificationAlertManager.cancelNativeAlerts(this);
    }

	@Override
	protected void onStop() {
		super.onStop();
        unbindService(conn);
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // TODO: Serhii please clean up per our design guidelines.
    private void onLoadComplete() {
		Log.i(TAG, "onLoadComplete");

		if (!User.isRegistered(this)) {
			Log.i(TAG, "Not registered. Starting RegisterActivity");
			Intent i = new Intent(this, RegisterActivity.class);
			startActivity(i);
			finish();
			return;
		} 

		mainFragment = getFragmentManager().findFragmentByTag("main");
		if(mainFragment == null){
			mainFragment = new GridViewFragment();
			getFragmentManager().beginTransaction().add(R.id.content_frame, mainFragment, "main").commit();
		}

		if (gcmHandler.checkPlayServices()){
			gcmHandler.registerGcm();
		} else {
			Dispatch.dispatch("No valid Google Play Services APK found.");
		}

		benchController.onDataLoaded();
		new S3CredentialsGetter(this);
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
                if(benchController.isBenchShowed()) {
                    benchController.hideBench();
                } else {
                    benchController.showBench();
                }
            default:
                return super.onOptionsItemSelected(item);
        }
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
    public void onShowInfoDialog(String title, String msg) {
        DialogShower.showInfoDialog(this, title, msg);
    }

    public void onShowActionInfoDialog(String title, String msg, String actionTitle, boolean isNeedCancel, int actionId){
        DialogShower.showActionInfoDialog(this, title, msg, actionTitle, isNeedCancel, actionId);
    }

    @Override
    public void showVersionHandlerDialog(String message, boolean negativeButton) {
        DialogShower.showVersionHandlerDialog(this, message, negativeButton);
    }

    @Override
    public BenchViewManager getBenchViewManager() {
        return benchController;
    }
}
