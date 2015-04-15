package com.zazoapp.client.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
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
import com.zazoapp.client.DispatcherService;
import com.zazoapp.client.PreferencesHelper;
import com.zazoapp.client.R;
import com.zazoapp.client.VersionHandler;
import com.zazoapp.client.bench.BenchController;
import com.zazoapp.client.bench.BenchViewManager;
import com.zazoapp.client.bench.InviteManager;
import com.zazoapp.client.bench.InviteManager.InviteDialogListener;
import com.zazoapp.client.debug.ZazoGestureListener;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.model.ActiveModelsHandler;
import com.zazoapp.client.model.Contact;
import com.zazoapp.client.network.aws.S3CredentialsGetter;
import com.zazoapp.client.notification.NotificationAlertManager;
import com.zazoapp.client.notification.gcm.GcmHandler;
import com.zazoapp.client.ui.dialogs.AbstractDialogFragment;
import com.zazoapp.client.ui.dialogs.ActionInfoDialogFragment.ActionInfoDialogListener;
import com.zazoapp.client.ui.dialogs.DoubleActionDialogFragment;
import com.zazoapp.client.ui.dialogs.ProgressDialogFragment;
import com.zazoapp.client.ui.dialogs.SelectPhoneNumberDialog;
import com.zazoapp.client.utilities.DialogShower;

public class MainActivity extends Activity implements ActionInfoDialogListener, VersionHandler.Callback,
        InviteDialogListener, BenchViewManager.Provider, SelectPhoneNumberDialog.Callbacks, DoubleActionDialogFragment.DoubleActionDialogListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final int CONNECTED_DIALOG = 0;
    public static final int NUDGE_DIALOG = 1;
    public static final int SMS_DIALOG = 2;
    public static final int SENDLINK_DIALOG = 3;
    public static final int NO_SIM_DIALOG = 4;

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
	private GridViewFragment mainFragment;
	private InviteManager inviteManager;
    private DialogFragment pd;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        PreferencesHelper preferences = new PreferencesHelper(this);
        if (!preferences.getBoolean(ActiveModelsHandler.USER_REGISTERED, false)) {
            startRegisterActivity();
            return;
        }
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
		actionBar.setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
		ImageView v = new ImageView(this);
		v.setImageResource(R.drawable.zazo_type);
        v.setOnTouchListener(new ZazoGestureListener(this));
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
        bindService(new Intent(this, DispatcherService.class), conn, Service.BIND_IMPORTANT);
		versionHandler.checkVersionCompatibility();
        NotificationAlertManager.init(this);
    }

	@Override
	protected void onStop() {
		super.onStop();
        unbindService(conn);
        NotificationAlertManager.cleanUp();
	}

    // TODO: Serhii please clean up per our design guidelines.
    private void onLoadComplete() {
		Log.i(TAG, "onLoadComplete");
        mainFragment = (GridViewFragment) getFragmentManager().findFragmentByTag("main");
        if (mainFragment == null) {
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
    public void onActionClicked(int id, Bundle bundle) {
        switch (id) {
            case CONNECTED_DIALOG:
                inviteManager.moveFriendToGrid();
                break;
            case NUDGE_DIALOG:
                inviteManager.showSms();
                break;
            case SMS_DIALOG:
                inviteManager.inviteNewFriend();
                break;
            case NO_SIM_DIALOG:
                inviteManager.showConnectedDialog();
                break;
        }
    }

    @Override
    public void onDialogActionClicked(int id, int button, Bundle params) {
        if (id == SENDLINK_DIALOG) {
            switch (button) {
                case BUTTON_POSITIVE:
                    inviteManager.sendInvite(AbstractDialogFragment.getEditedMessage(params));
                    break;
                case BUTTON_NEGATIVE:
                    inviteManager.failureNoSimDialog();
                    break;
            }
        }
    }

    @Override
    public void phoneSelected(Contact contact, int phoneIndex) {
        inviteManager.invite(contact, phoneIndex);
    }

    @Override
    public void onShowInfoDialog(String title, String msg) {
        DialogShower.showInfoDialog(this, title, msg);
    }

    @Override
    public void onShowActionInfoDialog(String title, String msg, String actionTitle, boolean isNeedCancel, boolean editable, int actionId){
        DialogShower.showActionInfoDialog(this, title, msg, actionTitle, isNeedCancel, editable, actionId, this);
    }

    @Override
    public void onShowDoubleActionDialog(String title, String msg, String posText, String negText, int id, boolean editable) {
        DialogShower.showDoubleActionDialog(this, title, msg, posText, negText, id, editable, this);
    }

    @Override
    public void onShowProgressDialog(String title, String msg) {
        dismissProgressDialog();
        pd = ProgressDialogFragment.getInstance(title, msg);
        pd.show(getFragmentManager(), null);
    }

    @Override
    public void onShowSelectPhoneNumberDialog(Contact contact) {
        DialogShower.showSelectPhoneNumberDialog(this, contact, this);
    }

    @Override
    public void onDismissProgressDialog() {
        dismissProgressDialog();
    }

    private void dismissProgressDialog() {
        if (pd != null)
            pd.dismiss();
    }

    @Override
    public void showVersionHandlerDialog(String message, boolean negativeButton) {
        DialogShower.showVersionHandlerDialog(this, message, negativeButton);
    }

    @Override
    public BenchViewManager getBenchViewManager() {
        return benchController;
    }


    private void startRegisterActivity() {
        Log.i(TAG, "Not registered. Starting RegisterActivity");
        Intent i = new Intent(this, RegisterActivity.class);
        startActivity(i);
        finish();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (mainFragment != null) {
            mainFragment.onWindowFocusChanged(hasFocus && !NotificationAlertManager.screenIsLocked(this));
        }
    }
}
