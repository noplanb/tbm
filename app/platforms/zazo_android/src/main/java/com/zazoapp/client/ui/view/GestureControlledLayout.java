package com.zazoapp.client.ui.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import com.zazoapp.client.ui.ViewGroupGestureRecognizer;

/**
 * Created by skamenkovych@codeminders.com on 2/1/2016.
 */
public class GestureControlledLayout extends FrameLayout {

    private ViewGroupGestureRecognizer gestureRecognizer;

    public GestureControlledLayout(Context context) {
        super(context);
    }

    public GestureControlledLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GestureControlledLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public GestureControlledLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setGestureRecognizer(ViewGroupGestureRecognizer recognizer) {
        gestureRecognizer = recognizer;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean handled = false;
        if (gestureRecognizer != null) {
            handled = gestureRecognizer.dispatchTouchEvent(ev);
        }
        if (!handled) {
            super.dispatchTouchEvent(ev);
        }
        return handled;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestureRecognizer != null) {
            return gestureRecognizer.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }
}
