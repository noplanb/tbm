package com.zazoapp.client.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

/**
 * Created by skamenkovych@codeminders.com on 11/10/2015.
 */
public class ZazoFragment extends Fragment {
    private static final String ZAZO_FRAGMENT_ID = "zf_id";
    private int fragmentId;

    public int getFragmentId() {
        return fragmentId;
    }

    public void setFragmentId(int id) {
        this.fragmentId = id;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(ZAZO_FRAGMENT_ID, fragmentId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(ZAZO_FRAGMENT_ID)) {
                fragmentId = savedInstanceState.getInt(ZAZO_FRAGMENT_ID, 0);
            }
        }
    }

    /**
     * Publish result to host activity
     * Host activity must implement {@link TaskFragmentListener} interface to support this method
     */
    protected void publishResult(int resultCode, Intent data) {
        TaskFragmentListener listener;
        try {
            listener = (TaskFragmentListener) getActivity();
        } catch (ClassCastException e) {
            throw new RuntimeException("Host activity must implement " + TaskFragmentListener.class.getSimpleName()
                    + " interface to support this method");
        }

        if (listener != null) {
            listener.onFragmentResult(getFragmentId(), resultCode, data);
        }
    }

    public void onWindowFocusChanged(Activity activity, boolean hasFocus) {
    }
}
