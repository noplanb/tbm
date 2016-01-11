package com.zazoapp.client.ui;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import com.zazoapp.client.R;
import com.zazoapp.client.core.PreferencesHelper;
import com.zazoapp.client.model.ActiveModelsHandler;
import com.zazoapp.client.notification.NotificationAlertManager;

public class MainActivity extends FragmentActivity implements TaskFragmentListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String CURRENT_FRAGMENT_KEY = "current_zazo_fragment";

    private ZazoFragment currentFragment;
    private int currentFragmentId;

    private static final int MAIN_FRAGMENT = 0;
    private static final int REGISTER_FRAGMENT = 1;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        setupFragment();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupWindowParams();
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
        PreferencesHelper preferences = new PreferencesHelper(this);
        if (!preferences.getBoolean(ActiveModelsHandler.USER_REGISTERED, false)) {
            currentFragmentId = REGISTER_FRAGMENT;
        } else {
            currentFragmentId = MAIN_FRAGMENT;
        }
        currentFragment = (ZazoFragment) getSupportFragmentManager().findFragmentByTag("main" + currentFragmentId);
        if (currentFragment == null) {
            switch (currentFragmentId) {
                case MAIN_FRAGMENT:
                    currentFragment = new MainFragment();
                    break;
                case REGISTER_FRAGMENT:
                    currentFragment = new RegisterFragment();
                    break;
                default:
                    throw new NullPointerException("Current fragment isn't set");
            }
            currentFragment.setFragmentId(currentFragmentId);
            getSupportFragmentManager().beginTransaction().add(R.id.main_frame, currentFragment, "main" + currentFragmentId).commit();
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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (currentFragment instanceof MainFragment) {
            ((MainFragment) currentFragment).onWindowFocusChanged(hasFocus && !NotificationAlertManager.screenIsLocked(this));
        }
    }

    @Override
    public void onFragmentResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REGISTER_FRAGMENT:
                currentFragmentId = MAIN_FRAGMENT;
                currentFragment = new MainFragment();
                currentFragment.setFragmentId(currentFragmentId);
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.main_frame, currentFragment, "main" + currentFragmentId)
                        .commitAllowingStateLoss();
                setupWindowParams();
                break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        outState.putInt(CURRENT_FRAGMENT_KEY, currentFragmentId);
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentFragmentId = savedInstanceState.getInt(CURRENT_FRAGMENT_KEY, MAIN_FRAGMENT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setupWindowParams() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            Resources res = getResources();
            switch (currentFragmentId) {
                case MAIN_FRAGMENT:
                    window.setStatusBarColor(res.getColor(R.color.status_bar_main_color));
                    break;
                case REGISTER_FRAGMENT:
                    window.setStatusBarColor(res.getColor(R.color.status_bar_register_color));
                    break;
            }
        }
        switch (currentFragmentId) {
            case MAIN_FRAGMENT:
                window.setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                break;
            case REGISTER_FRAGMENT:
                window.setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                break;
        }
    }
}
