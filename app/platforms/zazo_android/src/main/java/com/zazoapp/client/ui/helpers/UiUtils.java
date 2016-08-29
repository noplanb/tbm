package com.zazoapp.client.ui.helpers;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
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
        Drawable[] drawables;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            drawables = view.getCompoundDrawablesRelative();
        } else {
            drawables = view.getCompoundDrawables();
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            view.setCompoundDrawablesRelative(drawables[0], drawables[1], drawables[2], drawables[3]);
        } else {
            view.setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawables[3]);
        }
    }

    public static void showSoftKeyboard(View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, 0);
            }
        }
    }

    public static void hideSoftKeyboard(View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    /**
     * Transforms a motion event into the coordinate space of a particular child view
     */
    public static boolean dispatchTransformedTouchEvent(@NonNull ViewGroup parent, @NonNull MotionEvent event, @NonNull View child) {
        final boolean handled;

        // Canceling motions is a special case.  We don't need to perform any transformations
        // or filtering.  The important part is the action, not the contents.
        final int oldAction = event.getAction();
        if (oldAction == MotionEvent.ACTION_CANCEL) {
            event.setAction(MotionEvent.ACTION_CANCEL);
            handled = child.dispatchTouchEvent(event);
            event.setAction(oldAction);
            return handled;
        }

        // Perform any necessary transformations and dispatch.
        final float offsetX = parent.getScrollX() - child.getLeft();
        final float offsetY = parent.getScrollY() - child.getTop();
        event.offsetLocation(offsetX, offsetY);

        handled = child.dispatchTouchEvent(event);

        event.offsetLocation(-offsetX, -offsetY);

        // Done.
        return handled;
    }

    public static boolean dispatchOnClickOnChild(@NonNull ViewGroup parent, @NonNull View child, float x, float y) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        boolean handled = false;
        MotionEvent downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0);
        if (dispatchTransformedTouchEvent(parent, downEvent, child)) {
            MotionEvent upEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);
            handled = dispatchTransformedTouchEvent(parent, upEvent, child);
        }
        downEvent.recycle();
        return handled;
    }
}
