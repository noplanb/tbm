package com.zazoapp.client.ui;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import com.zazoapp.client.R;

/**
 * Created by skamenkovych@codeminders.com on 21.04.2016.
 */
public class WelcomeScreenActivity extends FragmentActivity {

    private ZazoTopFragment topFragment;
    private BaseManagerHolder managers;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_screen);
        managers = new BaseManagerHolder();
        managers.init(getApplicationContext(), this, null);
        topFragment = new WelcomeMultipleFragment();
        Bundle args = new Bundle();
        Intent intent = getIntent();
        if (intent != null) {
            args.putBoolean(SuggestionsFragment.FROM_APPLICATION, intent.getBooleanExtra(SuggestionsFragment.FROM_APPLICATION, false));
            args.putStringArrayList(SuggestionsFragment.ADDED_FRIENDS, intent.getStringArrayListExtra(SuggestionsFragment.ADDED_FRIENDS));
        }
        topFragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.content_frame, topFragment, "welcomeScreen").commit();
        setupWindowParams();
    }

    private void setupWindowParams() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            Resources res = getResources();
            window.setStatusBarColor(res.getColor(R.color.primary_dark));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        managers.registerManagers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        managers.unregisterManagers();
    }

    public BaseManagerProvider getManagerProvider() {
        return managers;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (topFragment != null) {
            return topFragment.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }
}
