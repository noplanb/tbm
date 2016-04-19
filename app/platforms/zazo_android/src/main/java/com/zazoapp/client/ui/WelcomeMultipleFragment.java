package com.zazoapp.client.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.MaterialMenuView;
import com.zazoapp.client.R;

/**
 * Created by skamenkovych@codeminders.com on 4/18/2016.
 */
public class WelcomeMultipleFragment extends ZazoTopFragment {

    private static final String TAG = WelcomeMultipleFragment.class.getSimpleName();

    @InjectView(R.id.up) MaterialMenuView up;
    @InjectView(R.id.zazo_action_bar) View actionBar;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.welcome_multiple_layout, null);
        ButterKnife.inject(this, v);
        up.setState(MaterialMenuDrawable.IconState.ARROW);
        return v;
    }

    private void hideActionBar() {
        actionBar.animate().alpha(0).start();
    }

    private void showActionBar() {
        actionBar.animate().alpha(1).start();
    }

    @OnClick(R.id.home)
    public void onBackClicked() {
        Log.i(TAG, "onBackClicked");
        onKeyDown(KeyEvent.KEYCODE_BACK, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
    }

}
