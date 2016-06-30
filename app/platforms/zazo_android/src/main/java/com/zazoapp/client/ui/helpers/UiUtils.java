package com.zazoapp.client.ui.helpers;

import android.view.View;
import android.view.ViewGroup;

/**
 * Created by skamenkovych@codeminders.com on 6/29/2016.
 */
public final class UiUtils {
    private UiUtils() {}

    public static void setEnabledAll(View v, boolean enabled) {
        v.setEnabled(enabled);
        v.setFocusable(enabled);

        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++)
                setEnabledAll(vg.getChildAt(i), enabled);
        }
    }
}
