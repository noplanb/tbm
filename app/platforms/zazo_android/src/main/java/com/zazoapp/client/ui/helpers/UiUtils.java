package com.zazoapp.client.ui.helpers;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

    public static void applyTint(TextView view, @ColorRes int color) {
        Drawable[] drawables = view.getCompoundDrawables();
        Context context = view.getContext();
        for (int i = 0; i < drawables.length; i++) {
            Drawable drawable = drawables[i];
            if (drawable != null) {
                drawable = DrawableCompat.wrap(drawable);
                DrawableCompat.setTint(drawable, context.getResources().getColor(color));
                DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);
                drawables[i] = drawable;
            }
        }
        view.setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawables[3]);
    }
}
