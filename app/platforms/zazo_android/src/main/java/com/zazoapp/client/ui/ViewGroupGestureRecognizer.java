package com.zazoapp.client.ui;

import android.app.Activity;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.zazoapp.client.R;

import java.util.ArrayList;

public abstract class ViewGroupGestureRecognizer {

    //----------
    // Interface
    //----------
    public abstract boolean click(View v);

    public abstract boolean startLongpress(View v);

    public abstract boolean endLongpress(View v);

    public abstract boolean bigMove(View v);

    public abstract boolean abort(View v, int reason);

    public abstract void notifyMove(View target, double startX, double startY, double offsetX, double offsetY);

    public abstract void startMove(View target, double startX, double startY, double offsetX, double offsetY);

    public abstract void endMove(double startX, double startY, double offsetX, double offsetY);

    public abstract void onTouch(double startX, double startY);

    public abstract boolean isSliding();

    public abstract boolean isSlidingSupported();

    public static class Stub extends ViewGroupGestureRecognizer {

        public Stub(Activity a, ArrayList<View> tvs) {
            super(a, tvs);
        }

        @Override
        public boolean click(View v) {
            return false;
        }

        @Override
        public boolean startLongpress(View v) {
            return false;
        }

        @Override
        public boolean endLongpress(View v) {
            return false;
        }

        @Override
        public boolean bigMove(View v) {
            return false;
        }

        @Override
        public boolean abort(View v, int reason) {
            return false;
        }

        @Override
        public void notifyMove(View target, double startX, double startY, double offsetX, double offsetY) {

        }

        @Override
        public void startMove(View target, double startX, double startY, double offsetX, double offsetY) {

        }

        @Override
        public void endMove(double startX, double startY, double offsetX, double offsetY) {

        }

        @Override
        public void onTouch(double startX, double startY) {

        }

        @Override
        public boolean isSliding() {
            return false;
        }

        @Override
        public boolean isSlidingSupported() {
            return false;
        }
    }
    // ---------
    // Constants
    // ---------
    private final String TAG = ViewGroupGestureRecognizer.class.getSimpleName();
    private static final Integer LONGPRESS_TIME = 500;

    // -----
    // State
    // -----
    private static final class State {
        public static final Integer IDLE = 0;
        public static final Integer DOWN = 1;
        public static final Integer LONGPRESS = 2;
        public static final Integer SLIDING = 3;
    }

    // ------
    // Fields
    // ------
    private Activity activity;
    private ArrayList<View> targetViews = new ArrayList<View>();
    private int state = State.IDLE;
    private View targetView;
    private double[] downPosition = new double[2];
    private boolean enabled = false;
    private boolean postponeDisabled = false;
    private boolean intercept = false;
    private boolean cancelOnMultiTouch = false;
    private CancelableTask longPressTask;

    // -------------------
    // Constructor related
    // -------------------
    public ViewGroupGestureRecognizer(Activity a, ArrayList<View> tvs) {
        activity = a;
        addTargetViews(tvs);
    }

    private void addTargetViews(ArrayList<View> tvs) {
        for (View v : tvs) {
            addTargetView(v);
        }
    }

    public void addTargetView(View target) {
        if (targetViews.contains(target))
            return;

        targetViews.add(target);
    }

    public void setCancelOnMultiTouch(Boolean value) {
        cancelOnMultiTouch = value;
    }

    //----------------------------
    // Methods that must be called
    //----------------------------
    // The viewGroup that instantiates this gesture recognizer must call these methods from its
    // equivalent overriden methods.

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (state == State.LONGPRESS)
            intercept = true;

        if (ev.getAction() == MotionEvent.ACTION_DOWN)
            intercept = false;
        handleTouchEvent(ev);
        if (state == State.IDLE) {
            move(null, 0, 0);
        }
        if (state == State.IDLE && longPressTask != null) {
            longPressTask.cancel();
        }
        if (state == State.SLIDING) {
            intercept = true;
        }
        return enabled;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return intercept;
    }

    // We return true here so that if none of the descendants of this view group
    // claims an interest in touches on we at least do so here. That way we continue to
    // get calls to dispatchTouchEvent. Note we will no longer get calls to onInterceptTouchEvent
    // in that case however but that is ok as there isnt a child to intercept from.
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    // --------------
    // Event Handling
    // --------------
    public void enable() {
        Log.i(TAG, "enable");
        enabled = true;
    }

    public void disable(Boolean silent) {
        Log.i(TAG, "disable silent=" + silent.toString());
        if (silent && state == State.LONGPRESS) {
            postponeDisabled = true;
        } else {
            cancelGesture();
            enabled = false;
        }
    }

    public void cancelGesture() {
        Log.i(TAG, "cancelGesture");
        if (state == State.LONGPRESS)
            runAbort(targetView, R.string.toast_gesture_cancelled);
        state = State.IDLE;
    }

    private void handleTouchEvent(MotionEvent event) {
        if (!enabled && !(postponeDisabled || event.getAction() == MotionEvent.ACTION_UP))
            return;

        int action = event.getAction();
        int maskedAction = event.getActionMasked();

        if (state == State.IDLE) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    int x = (int) event.getX();
                    int y = (int) event.getY();
                    targetView = pointToTargetView((int) event.getRawX(), (int) event.getRawY());
                    setDownPosition(event);
                    if (isSliding()) {
                        state = State.SLIDING;
                        onTouch(downPosition[0], downPosition[1]);
                        break;
                    }
                    state = State.DOWN;
                    if (targetView != null) {
                        startLongpressTimer();
                    }
                    return;
                case MotionEvent.ACTION_CANCEL:
                    // Safe to ignore since we would just stay in IDLE and do nothing.
                    return;
                case MotionEvent.ACTION_MOVE:
                    // Should never happen we should always get a ACTION_DOWN first which would move us out of IDLE.
                    return;
                case MotionEvent.ACTION_UP:
                    // Should never happen we should always get a ACTION_DOWN first which would move us out of IDLE.
                    return;
            }

            if (maskedAction == MotionEvent.ACTION_POINTER_DOWN) {
                // Should never happen we should always get a ACTION_DOWN first which would move us out of IDLE.
                return;
            }
            return;
        }

        if (state == State.DOWN) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    // Happens when the backing window view gets the down event. Just ignore.
                    return;
                case MotionEvent.ACTION_MOVE:
                    if (isSlidingSupported() && isMoving(event)) {
                        startMove(targetView, downPosition[0], downPosition[1], event.getX() - downPosition[0], event.getY() - downPosition[1]);
                        move(targetView, event.getX() - downPosition[0], event.getY() - downPosition[1]);
                        if (longPressTask != null) {
                            longPressTask.cancel();
                        }
                        state = State.SLIDING;
                    }
                    return;
                case MotionEvent.ACTION_CANCEL:
                    state = State.IDLE;
                    return;
                case MotionEvent.ACTION_UP:
                    state = State.IDLE;
                    if (targetView != null) {
                        runClick(targetView);
                    }
                    return;
            }

            if (maskedAction == MotionEvent.ACTION_POINTER_DOWN) {
                if (!cancelOnMultiTouch)
                    return;

                state = State.IDLE;
                return;
            }
            return;
        }

        if (state == State.LONGPRESS) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    // This should never happen but ignore rather than abort..
                    return;
                case MotionEvent.ACTION_MOVE:
                    if (isAbortLongpressMove(event)) {
                        state = State.IDLE;
                        runAbortLongpress(targetView);
                    }
                    return;
                case MotionEvent.ACTION_CANCEL:
                    state = State.IDLE;
                    // This should never happen but endLongPress and send the video rather than abort and lose it.
                    runEndLongpress(targetView);
                    return;
                case MotionEvent.ACTION_UP:
                    state = State.IDLE;
                    runEndLongpress(targetView);
                    return;
            }

            // Second finger down aborts.
            if (maskedAction == MotionEvent.ACTION_POINTER_DOWN) {
                if (!cancelOnMultiTouch)
                    return;

                state = State.IDLE;
                runAbort(targetView, R.string.toast_two_finger_touch);
                return;
            }
            return;
        }

        if (state == State.SLIDING) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    // Happens when the backing window view gets the down event. Just ignore.
                    return;
                case MotionEvent.ACTION_MOVE:
                    move(targetView, event.getX() - downPosition[0], event.getY() - downPosition[1]);
                    return;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    state = State.IDLE;
                    endMove(downPosition[0], downPosition[1], event.getX() - downPosition[0], event.getY() - downPosition[1]);
                    return;
            }

            if (maskedAction == MotionEvent.ACTION_POINTER_DOWN) {
                if (!cancelOnMultiTouch)
                    return;

                state = State.IDLE;
                return;
            }
            return;
        }
    }

    private void longPressTimerFired() {
        if (state == State.IDLE) {
            // This should never happen because any action that starts the timer should move us out of IDLE
            return;
        }

        if (state == State.DOWN) {
            state = State.LONGPRESS;
            runStartLongpress(targetView);
            return;
        }

        if (state == State.LONGPRESS) {
            // This should never happen because we should only get put in LONGPRESS as a result of the timer firing.
            return;
        }
    }

    // ---------------------------
    // Public events we broadcast:
    // ---------------------------
    // These public interface methods should be run on the UIThread of the activity that instantiated
    // this ViewGroupGestureRecognizer. This these events may need to change views and
    // only the original thread that created a view heirarchy can touch its views.
    private void runClick(final View v) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                click(v);
            }
        });
    }

    private void runStartLongpress(final View v) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startLongpress(v);
            }
        });
    }

    private void runEndLongpress(final View v) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                endLongpress(v);
            }
        });
        postponeDisable();
    }

    private void runAbortLongpress(final View v) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bigMove(v);
            }
        });
        postponeDisable();
    }

    private void runAbort(final View v, final int reason) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                abort(v, reason);
            }
        });
        postponeDisable();
    }

    private void postponeDisable() {
        if (postponeDisabled) {
            enabled = false;
            postponeDisabled = false;
        }
    }

    // -------
    // private
    // -------
    private boolean isATargetView(View v) {
        return targetViews.contains(v);
    }

    protected boolean isAbortLongpressMove(MotionEvent event) {
        double a2 = Math.pow(downPosition[0] - (double) event.getX(), 2D);
        double b2 = Math.pow(downPosition[1] - (double) event.getY(), 2D);
        double limit = Math.pow(activity.getResources().getDimension(R.dimen.nine_view_big_move), 2D);
        if (a2 + b2 > limit) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isMoving(MotionEvent event) {
        //double a2 = Math.pow(downPosition[0] - (double) event.getX(), 2D);
        //double b2 = Math.pow(downPosition[1] - (double) event.getY(), 2D);
        //double limit = Math.pow(activity.getResources().getDimension(R.dimen.nine_view_sliding_min_bounce), 2D);
        double limit = activity.getResources().getDimension(R.dimen.nine_view_sliding_min_bounce);
        double bounce = Math.abs(downPosition[1] - (double) event.getY());
        return bounce > limit;
    }

    private void startLongpressTimer() {
        if (longPressTask != null) {
            longPressTask.cancel();
        }
        longPressTask = new CancelableTask() {
            @Override
            protected void doTask() {
                longPressTimerFired();
            }
        };
        targetView.postDelayed(longPressTask, LONGPRESS_TIME);
    }

    private void setDownPosition(MotionEvent event) {
        downPosition[0] = (double) event.getX();
        downPosition[1] = (double) event.getY();
    }

    public View pointToTargetView(int x, int y) {
        Rect rect = new Rect();
        for (View v : targetViews) {
            v.getGlobalVisibleRect(rect);
            if (rect.contains(x, y))
                return v;
        }
        return null;
    }

    public void runAction(Runnable runnable) {
        activity.runOnUiThread(runnable);
    }

    private void move(View target, double x, double y) {
        notifyMove(target, downPosition[0], downPosition[1], x, y);
    }
}