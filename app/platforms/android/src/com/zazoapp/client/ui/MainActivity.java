package com.zazoapp.client.ui;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import com.zazoapp.client.core.IntentHandlerService;
import com.zazoapp.client.core.PreferencesHelper;
import com.zazoapp.client.R;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.core.VersionHandler;
import com.zazoapp.client.bench.BenchViewManager;
import com.zazoapp.client.bench.InviteHelper;
import com.zazoapp.client.bench.InviteManager.InviteDialogListener;
import com.zazoapp.client.debug.ZazoGestureListener;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.model.ActiveModelsHandler;
import com.zazoapp.client.model.Contact;
import com.zazoapp.client.multimedia.AudioController;
import com.zazoapp.client.multimedia.Player;
import com.zazoapp.client.multimedia.Recorder;
import com.zazoapp.client.network.aws.S3CredentialsGetter;
import com.zazoapp.client.notification.NotificationAlertManager;
import com.zazoapp.client.notification.gcm.GcmHandler;
import com.zazoapp.client.tutorial.Tutorial;
import com.zazoapp.client.ui.dialogs.AbstractDialogFragment;
import com.zazoapp.client.ui.dialogs.ActionInfoDialogFragment.ActionInfoDialogListener;
import com.zazoapp.client.ui.dialogs.DoubleActionDialogFragment;
import com.zazoapp.client.ui.dialogs.ProgressDialogFragment;
import com.zazoapp.client.ui.dialogs.SelectPhoneNumberDialog;
import com.zazoapp.client.ui.helpers.UnexpectedTerminationHelper;
import com.zazoapp.client.utilities.DialogShower;

public class MainActivity extends Activity implements ActionInfoDialogListener, VersionHandler.Callback, UnexpectedTerminationHelper.TerminationCallback,
        InviteDialogListener, ZazoManagerProvider, SelectPhoneNumberDialog.Callbacks, DoubleActionDialogFragment.DoubleActionDialogListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final int CONNECTED_DIALOG = 0;
    public static final int NUDGE_DIALOG = 1;
    public static final int SMS_DIALOG = 2;
    public static final int SENDLINK_DIALOG = 3;
    public static final int NO_SIM_DIALOG = 4;

    private GcmHandler gcmHandler;
    private VersionHandler versionHandler;
    private GridViewFragment mainFragment;
    private DialogFragment pd;
    private ManagerHolder managerHolder;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferencesHelper preferences = new PreferencesHelper(this);
        if (!preferences.getBoolean(ActiveModelsHandler.USER_REGISTERED, false)) {
            startRegisterActivity();
            return;
        }
        setContentView(R.layout.main_activity);

        gcmHandler = new GcmHandler(this);
        versionHandler = new VersionHandler(this);

        managerHolder = new ManagerHolder();
        managerHolder.init(this);
        TbmApplication.getInstance().addTerminationCallback(this);
        setupActionBar();
        setupFragment();
        new S3CredentialsGetter(this);
        startService(new Intent(this, IntentHandlerService.class));
    }

    private void setupActionBar() {
        findViewById(R.id.action_bar_icon).setOnTouchListener(new ZazoGestureListener(this));
        findViewById(R.id.home_menu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleBench();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if ("_FINISH".equals(intent.getAction())) {
            finish();
            return;
        }
        setIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        versionHandler.checkVersionCompatibility();
        checkPlayServices();
        NotificationAlertManager.init(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        NotificationAlertManager.cleanUp();
    }

    @Override
    protected void onResume() {
        super.onResume();
        managerHolder.registerManagers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (getBenchViewManager().isBenchShowed()) {
            getBenchViewManager().hideBench();
        }
        releaseManagers();
    }

    private void checkPlayServices() {
        Log.i(TAG, "checkPlayServices");
        if (gcmHandler.checkPlayServices()) {
            gcmHandler.registerGcm();
        } else {
            Dispatch.dispatch("No valid Google Play Services APK found.");
        }
    }

    private void setupFragment() {
        mainFragment = (GridViewFragment) getFragmentManager().findFragmentByTag("main");
        if (mainFragment == null) {
            mainFragment = new GridViewFragment();
            getFragmentManager().beginTransaction().add(R.id.content_frame, mainFragment, "main").commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_MENU:
                toggleBench();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onActionClicked(int id, Bundle bundle) {
        switch (id) {
            case CONNECTED_DIALOG:
                getInviteHelper().moveFriendToGrid();
                break;
            case NUDGE_DIALOG:
                getInviteHelper().showSmsDialog();
                break;
            case SMS_DIALOG:
                getInviteHelper().inviteNewFriend();
                break;
            case NO_SIM_DIALOG:
                getInviteHelper().finishInvitation();
                break;
        }
    }

    @Override
    public void onDialogActionClicked(int id, int button, Bundle params) {
        if (id == SENDLINK_DIALOG) {
            switch (button) {
                case BUTTON_POSITIVE:
                    getInviteHelper().sendInvite(AbstractDialogFragment.getEditedMessage(params));
                    break;
                case BUTTON_NEGATIVE:
                    getInviteHelper().failureNoSimDialog();
                    break;
            }
        }
    }

    @Override
    public void phoneSelected(Contact contact, int phoneIndex) {
        getInviteHelper().invite(contact, phoneIndex);
    }

    @Override
    public void onShowInfoDialog(String title, String msg) {
        DialogShower.showInfoDialog(this, title, msg);
    }

    @Override
    public void onShowActionInfoDialog(String title, String msg, String actionTitle, boolean isNeedCancel, boolean editable, int actionId) {
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
            pd.dismissAllowingStateLoss();
    }

    @Override
    public void showVersionHandlerDialog(String message, boolean negativeButton) {
        DialogShower.showVersionHandlerDialog(this, message, negativeButton);
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

    @Override
    public BenchViewManager getBenchViewManager() {
        return managerHolder.getBenchController();
    }

    @Override
    public AudioController getAudioController() {
        return managerHolder.getAudioManager();
    }

    @Override
    public Recorder getRecorder() {
        return managerHolder.getVideoRecorder();
    }

    @Override
    public Player getPlayer() {
        return managerHolder.getVideoPlayer();
    }

    @Override
    public InviteHelper getInviteHelper() {
        return managerHolder.getInviteManager();
    }

    @Override
    public Tutorial getTutorial() {
        return managerHolder.getTutorial();
    }

    private void releaseManagers() {
        managerHolder.unregisterManagers();
    }

    @Override
    public void onTerminate() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                releaseManagers();
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        //TODO eliminate blocking touches for 2.3.x release
        //if (audioManager.isSpeakerPhoneOn()) {
            return super.dispatchTouchEvent(ev);
        //}
        //return true;
    }

    private void toggleBench() {
        if (getBenchViewManager() != null) {
            if (getBenchViewManager().isBenchShowed()) {
                getBenchViewManager().hideBench();
            } else {
                getBenchViewManager().showBench();
            }
        }
    }

}
