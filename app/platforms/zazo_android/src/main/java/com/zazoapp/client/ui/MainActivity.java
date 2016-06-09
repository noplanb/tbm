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
import com.zazoapp.client.core.IntentHandlerService;
import com.zazoapp.client.core.PreferencesHelper;
import com.zazoapp.client.dispatch.ZazoAnalytics;
import com.zazoapp.client.model.ActiveModelsHandler;
import com.zazoapp.client.notification.NotificationAlertManager;
import com.zazoapp.client.utilities.Logger;

public class MainActivity extends FragmentActivity implements TaskFragmentListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String CURRENT_FRAGMENT_KEY = "current_zazo_fragment";
    /** Set to true when onNewIntent is called right after onCreate */
    public static final String EXTRA_NEW_INTENT_AFTER_ON_CREATE = "new_intent_after_on_create";
    public static final String ACTION_SUGGESTIONS = "action_suggestions";

    private ZazoFragment currentFragment;
    private int currentFragmentId;

    private static final int MAIN_FRAGMENT = 0;
    private static final int REGISTER_FRAGMENT = 1;
    private static final int SUGGESTIONS_FRAGMENT = 2;
    private static final int SUGGESTIONS_INNER_FRAGMENT = 3;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.i(TAG, "onCreate " + getIntent());
        ZazoAnalytics.start();
        setContentView(R.layout.main_activity);
        setupFragment();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Logger.i(TAG, "onNewIntent " + intent);
        setIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Logger.i(TAG, "onStart");
        setupWindowParams();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Logger.i(TAG, "onStop");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.i(TAG, "onResume");
        ZazoAnalytics.onActivityResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Logger.i(TAG, "onPause");
        ZazoAnalytics.onActivityPause(this);
    }

    private void setupFragment() {
        PreferencesHelper preferences = new PreferencesHelper(this);
        if (!preferences.getBoolean(ActiveModelsHandler.USER_REGISTERED, false)) {
            currentFragmentId = REGISTER_FRAGMENT;
        } else {
            if (isSuggestionIntent()) {
                currentFragmentId = SUGGESTIONS_FRAGMENT;
            } else {
                currentFragmentId = MAIN_FRAGMENT;
            }
            ZazoAnalytics.setUser();
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
                case SUGGESTIONS_FRAGMENT:
                    NotificationAlertManager.cancelNativeAlert(this, NotificationAlertManager.NotificationType.FRIEND_JOINED.id());
                    currentFragment = SuggestionsFragment.getInstance(getIntent());
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
        boolean result = currentFragment.onKeyDown(keyCode, event);
        return result || super.onKeyDown(keyCode, event);
    }

    @Override
    public void onFragmentResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REGISTER_FRAGMENT:
                currentFragmentId = MAIN_FRAGMENT;
                ZazoAnalytics.setUser();
                currentFragment = new MainFragment();
                currentFragment.setFragmentId(currentFragmentId);
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.main_frame, currentFragment, "main" + currentFragmentId)
                        .commitAllowingStateLoss();
                setupWindowParams();
                break;
            case MAIN_FRAGMENT:
                if (resultCode == MainFragment.ACTION_CODE_SHOW_SUGGESTIONS) {
                    MainFragment mainFragment = (MainFragment) getSupportFragmentManager().findFragmentByTag("main" + MAIN_FRAGMENT);
                    currentFragmentId = SUGGESTIONS_INNER_FRAGMENT;
                    currentFragment = SuggestionsFragment.getInstance(getIntent());
                    currentFragment.setFragmentId(currentFragmentId);
                    mainFragment.showTopFragment(currentFragment, R.anim.fade_in, R.anim.fade_out);
                }
                break;
            case SUGGESTIONS_FRAGMENT:
                finish();
                break;
            case SUGGESTIONS_INNER_FRAGMENT:
                currentFragmentId = MAIN_FRAGMENT;
                currentFragment = (ZazoFragment) getSupportFragmentManager().findFragmentByTag("main" + currentFragmentId);
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
                default:
                    window.setStatusBarColor(res.getColor(R.color.primary_dark));
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
            default:
                window.setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                break;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Logger.i(TAG, "onWindowFocusChanged " + hasFocus);
        if (currentFragment != null) {
            currentFragment.onWindowFocusChanged(this, hasFocus);
        }
    }

    private boolean isSuggestionIntent() {
        Intent intent = getIntent();
        return intent != null && IntentHandlerService.IntentActions.SUGGESTIONS.equals(intent.getAction());
    }
}
