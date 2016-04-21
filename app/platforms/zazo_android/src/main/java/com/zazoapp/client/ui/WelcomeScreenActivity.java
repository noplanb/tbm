package com.zazoapp.client.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import com.zazoapp.client.R;

/**
 * Created by skamenkovych@codeminders.com on 21.04.2016.
 */
public class WelcomeScreenActivity extends FragmentActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_screen);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.content_frame, new WelcomeMultipleFragment(), "welcomeScreen").commit();
    }
}
