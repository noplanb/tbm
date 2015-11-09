package com.zazoapp.client.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import com.zazoapp.client.R;
import com.zazoapp.client.core.PreferencesHelper;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.model.ActiveModelsHandler;
import com.zazoapp.client.notification.NotificationAlertManager;
import com.zazoapp.client.ui.dialogs.InviteIntent;

public class MainActivity extends FragmentActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Fragment currentFragment;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferencesHelper preferences = new PreferencesHelper(this);
        if (!preferences.getBoolean(ActiveModelsHandler.USER_REGISTERED, false)) {
            startRegisterActivity();
            return;
        }
        setContentView(R.layout.main_activity);
        setupFragment();
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
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void setupFragment() {
        currentFragment = getSupportFragmentManager().findFragmentByTag("main");
        if (currentFragment == null) {
            currentFragment = new MainFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.main_frame, currentFragment, "main").commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean result = false;
        if (currentFragment instanceof MainFragment) {
            result = ((MainFragment) currentFragment).onKeyDown(keyCode, event);
        }
        return result || super.onKeyDown(keyCode, event);
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
        if (currentFragment instanceof MainFragment) {
            ((MainFragment) currentFragment).onWindowFocusChanged(hasFocus && !NotificationAlertManager.screenIsLocked(this));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == InviteIntent.INVITATION_REQUEST_ID) {
            TbmApplication.getInstance().getManagerProvider().getInviteHelper().finishInvitation();
        }
    }

}
