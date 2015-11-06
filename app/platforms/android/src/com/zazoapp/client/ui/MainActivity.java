package com.zazoapp.client.ui;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import com.zazoapp.client.R;
import com.zazoapp.client.bench.InviteManager.InviteDialogListener;
import com.zazoapp.client.core.IntentHandlerService;
import com.zazoapp.client.core.PreferencesHelper;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.core.VersionHandler;
import com.zazoapp.client.debug.ZazoGestureListener;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.model.ActiveModelsHandler;
import com.zazoapp.client.model.Contact;
import com.zazoapp.client.network.aws.S3CredentialsGetter;
import com.zazoapp.client.notification.NotificationAlertManager;
import com.zazoapp.client.notification.gcm.GcmHandler;
import com.zazoapp.client.ui.dialogs.AbstractDialogFragment;
import com.zazoapp.client.ui.dialogs.ActionInfoDialogFragment.ActionInfoDialogListener;
import com.zazoapp.client.ui.dialogs.DoubleActionDialogFragment;
import com.zazoapp.client.ui.dialogs.InviteIntent;
import com.zazoapp.client.ui.dialogs.ProgressDialogFragment;
import com.zazoapp.client.ui.dialogs.SelectPhoneNumberDialog;
import com.zazoapp.client.ui.dialogs.SendLinkThroughDialog;
import com.zazoapp.client.ui.helpers.UnexpectedTerminationHelper;
import com.zazoapp.client.ui.view.MainMenuPopup;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.DialogShower;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements ActionInfoDialogListener, VersionHandler.Callback, UnexpectedTerminationHelper.TerminationCallback,
        InviteDialogListener, SelectPhoneNumberDialog.Callbacks, DoubleActionDialogFragment.DoubleActionDialogListener, MainMenuPopup.MenuItemListener {

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
    private boolean isStopped = true;

    private final BroadcastReceiver serviceReceiver = new MainActivityReceiver();

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
        TbmApplication.getInstance().initManagerProvider(managerHolder);
        TbmApplication.getInstance().addTerminationCallback(this);
        setupActionBar();
        setupFragment();
        new S3CredentialsGetter(this);
        startService(new Intent(this, IntentHandlerService.class));
    }

    private void setupActionBar() {
        findViewById(R.id.action_bar_icon).setOnTouchListener(new ZazoGestureListener(this));
        findViewById(R.id.friends_menu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (managerHolder.getPlayer().isPlaying()) {
                    managerHolder.getPlayer().stop();
                }
                if (managerHolder.getRecorder().isRecording()) {
                    return;
                }
                toggleBench();
            }
        });
        findViewById(R.id.overflow_menu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (managerHolder.getBenchViewManager().isBenchShowed()) {
                    managerHolder.getBenchViewManager().hideBench();
                }
                if (managerHolder.getPlayer().isPlaying()) {
                    managerHolder.getPlayer().stop();
                }
                if (managerHolder.getRecorder().isRecording()) {
                    return;
                }
                List<Integer> disabledItems = new ArrayList<Integer>();
                if (!managerHolder.getFeatures().isUnlocked(Features.Feature.DELETE_FRIEND)) {
                    disabledItems.add(R.id.menu_manage_friends);
                }
                MainMenuPopup popup = new MainMenuPopup(MainActivity.this, disabledItems);
                popup.setAnchorView(v);
                popup.setMenuItemListener(MainActivity.this);
                popup.show();
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
        isStopped = false;
        versionHandler.checkVersionCompatibility();
        checkPlayServices();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Convenience.ON_NOT_ENOUGH_SPACE_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceReceiver, filter);
        Convenience.checkAndNotifyNoSpace(this);
        NotificationAlertManager.init(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        isStopped = true;
        NotificationAlertManager.cleanUp();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        managerHolder.registerManagers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (managerHolder.getBenchViewManager().isBenchShowed()) {
            managerHolder.getBenchViewManager().hideBench();
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
        mainFragment = (GridViewFragment) getSupportFragmentManager().findFragmentByTag("main");
        if (mainFragment == null) {
            mainFragment = new GridViewFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.content_frame, mainFragment, "main").commit();
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
                managerHolder.getInviteHelper().moveFriendToGrid();
                break;
            case NUDGE_DIALOG:
                managerHolder.getInviteHelper().showSmsDialog();
                break;
            case SMS_DIALOG:
                managerHolder.getInviteHelper().inviteNewFriend();
                break;
            case NO_SIM_DIALOG:
                managerHolder.getInviteHelper().finishInvitation();
                break;
        }
    }

    @Override
    public void onDialogActionClicked(int id, int button, Bundle params) {
        if (id == SENDLINK_DIALOG) {
            switch (button) {
                case BUTTON_POSITIVE:
                    if (params != null) {
                        if (params.getBoolean(SendLinkThroughDialog.SEND_SMS_KEY, false)) {
                            managerHolder.getInviteHelper().sendInvite(AbstractDialogFragment.getEditedMessage(params), this);
                        } else {
                            Intent invite = params.getParcelable(SendLinkThroughDialog.INTENT_KEY);
                            String name = params.getString(SendLinkThroughDialog.APP_NAME_KEY);
                            if (invite != null && !TextUtils.isEmpty(name)) {
                                try {
                                    startActivityForResult(invite, InviteIntent.INVITATION_REQUEST_ID);
                                    managerHolder.getInviteHelper().notifyInviteVector(name, true);
                                } catch (ActivityNotFoundException e) {
                                    managerHolder.getInviteHelper().notifyInviteVector(name, false);
                                }
                            }
                        }
                    }
                    break;
                case BUTTON_NEGATIVE:
                    managerHolder.getInviteHelper().failureNoSimDialog();
                    break;
            }
        }
    }

    @Override
    public void phoneSelected(Contact contact, int phoneIndex) {
        managerHolder.getInviteHelper().invite(contact, phoneIndex);
    }

    @Override
    public void onShowInfoDialog(String title, String msg) {
        if (!isStopped) {
            DialogShower.showInfoDialog(this, title, msg);
        }
    }

    @Override
    public void onShowActionInfoDialog(String title, String msg, String actionTitle, boolean isNeedCancel, boolean editable, int actionId) {
        if (!isStopped) {
            DialogShower.showActionInfoDialog(this, title, msg, actionTitle, isNeedCancel, editable, actionId, this);
        }
    }

    @Override
    public void onShowDoubleActionDialog(String title, String msg, String posText, String negText, int id, boolean editable) {
        if (!isStopped) {
            DialogShower.showDoubleActionDialog(this, title, msg, posText, negText, id, editable, this);
        }
    }

    @Override
    public void onShowSendLinkDialog(int id, String phone, String msg) {
        if (!isStopped) {
            DialogShower.showSendLinkDialog(this, id, phone, msg, this);
        }
    }

    @Override
    public void onShowProgressDialog(String title, String msg) {
        dismissProgressDialog();
        pd = ProgressDialogFragment.getInstance(title, msg);
        DialogShower.showDialog(this, pd, null);
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
        if (!isStopped) {
            DialogShower.showVersionHandlerDialog(this, message, negativeButton);
        }
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

    private void toggleBench() {
        if (managerHolder.getBenchViewManager() != null) {
            if (managerHolder.getBenchViewManager().isBenchShowed()) {
                managerHolder.getBenchViewManager().hideBench();
            } else {
                managerHolder.getBenchViewManager().showBench();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == InviteIntent.INVITATION_REQUEST_ID) {
            managerHolder.getInviteHelper().finishInvitation();
        }
    }

    @Override
    public void onMenuItemSelected(int id) {
        switch (id) {
            case R.id.menu_manage_friends: {
                FragmentTransaction tr = getSupportFragmentManager().beginTransaction();
                tr.setCustomAnimations(R.anim.slide_left_fade_in, R.anim.slide_right_fade_out, R.anim.slide_left_fade_in, R.anim.slide_right_fade_out);
                tr.add(R.id.top_frame, new ManageFriendsFragment());
                tr.addToBackStack(null);
                tr.commit();
                break;
            }
            case R.id.menu_send_feedback: {
                Bundle bundle = new Bundle();
                bundle.putString(InviteIntent.EMAIL_KEY, "feedback@zazoapp.com");
                bundle.putString(InviteIntent.SUBJECT_KEY, getString(R.string.feedback_subject));
                bundle.putString(InviteIntent.MESSAGE_KEY, "");
                Intent feedback = InviteIntent.EMAIL.getIntent(bundle);
                try {
                    startActivity(feedback);
                } catch (ActivityNotFoundException e) {
                    DialogShower.showToast(this, R.string.feedback_send_fails);
                }
                break;
            }
        }
    }

    private void showNotEnoughSpaceDialog() {
        DialogShower.showBlockingDialog(this, R.string.alert_not_enough_space_title, R.string.alert_not_enough_space_message);
    }

    private class MainActivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Convenience.ON_NOT_ENOUGH_SPACE_ACTION.equals(intent.getAction())) {
                showNotEnoughSpaceDialog();
            }
        }
    }
}
