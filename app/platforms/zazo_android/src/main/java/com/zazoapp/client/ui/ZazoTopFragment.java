package com.zazoapp.client.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.KeyEvent;

/**
 * Created by skamenkovych@codeminders.com on 3/24/2016.
 */
public class ZazoTopFragment extends Fragment {
    public interface OnBackListener {
        void onBack();
    }

    private OnBackListener onBackListener;

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (onBackListener != null) {
                    onBackListener.onBack();
                }
                onBackPressed();
                FragmentManager fm = getFragmentManager();
                if (fm != null) {
                    fm.popBackStack();
                }
                return true;
        }
        return false;
    }

    protected void onBackPressed() {
    }

    public void setOnBackListener(OnBackListener listener) {
        onBackListener = listener;
    }
}
