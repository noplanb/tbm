package com.zazoapp.client.ui.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.zazoapp.client.R;
import com.zazoapp.client.ui.MainActivity;

/**
 * Created by skamenkovych@codeminders.com on 7/22/2015.
 */
public class NextFeatureDialogFragment extends DialogFragment implements View.OnClickListener, View.OnTouchListener {

    private boolean resumed;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.base_dialog);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.feature_unlock_another, container, false);
        v.findViewById(R.id.body).setOnClickListener(this);
        v.setOnTouchListener(this);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        resumed = true;
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dismissAnimated();
            }
        }, 3000);
    }

    @Override
    public void onPause() {
        super.onPause();
        resumed = false;
        dismiss();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.body:
                dismiss();
                ((MainActivity) getActivity()).getBenchViewManager().showBench();
                break;
        }
    }

    private void dismissAnimated() {
        if (!isHidden() && resumed) {
            getFragmentManager().beginTransaction().setCustomAnimations(R.animator.slide_up, R.animator.slide_down).hide(this).commitAllowingStateLoss();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        dismissAnimated();
        return false;
    }
}
