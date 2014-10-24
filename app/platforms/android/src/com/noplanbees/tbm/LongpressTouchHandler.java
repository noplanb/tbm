package com.noplanbees.tbm;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;

public abstract class LongpressTouchHandler {

	// ---------
	// Constants
	// ---------
	private final String TAG = this.getClass().getSimpleName();
	private static final Integer LONGPRESS_TIME = 200;
	private static final int BIG_MOVE_DISTANCE = 100;

	// -----
	// State
	// -----
	private static final class State{
		public static final Integer IDLE = 0;
		public static final Integer DOWN = 1;
		public static final Integer LONGPRESS = 2;
	}

	// ------
	// Fields
	// ------
	private Activity activity;
	private View backgroundView;
	private ArrayList<View> targetViews = new ArrayList<View>();
	private Integer state = State.IDLE;
	private View targetView;
	private Double[] downPosition = new Double[2];
	private Timer longPressTimer;
	private Boolean enabled = false;
	private GestureDetector flingDetector;

	// -------------------
	// Constructor related
	// -------------------
	public LongpressTouchHandler(Activity activity, View backgroundView){
		this.activity = activity;
		this.backgroundView = backgroundView;
		addListener(backgroundView);
		flingDetector = new GestureDetector(activity, new FlingListener());
	}

	public void addTargetView(View target){
		if (targetViews.contains(target))
			return;

		targetViews.add(target);
		addListener(target);
	}

	private void addListener(View target) {
		Log.i(TAG, "addListener: " + target.toString());
		target.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				handleTouchEvent(v, event);
				flingDetector.onTouchEvent(event);
				return v == backgroundView;
			}
		});
	}
	// --------------
	// Event Handling
	// --------------
	public void enable(){
		Log.i(TAG, "enable");
		enabled = true;
	}
	
	public void disable(Boolean silent){
		Log.i(TAG, "disable silent=" + silent.toString());
		cancelGesture(silent);
		enabled = false;
	}
	
	public void cancelGesture(Boolean silent) {
		Log.i(TAG, "cancelGesture silent=" + silent.toString());
		if (!silent && state == State.LONGPRESS)
			runAbort(targetView);

		state = State.IDLE;
	}

	private void handleTouchEvent(View v, MotionEvent event) {
		if (!enabled)
			return;
					
		int action = event.getAction();
		int maskedAction = event.getActionMasked();

		if (state == State.IDLE){
			switch (action){
			case MotionEvent.ACTION_DOWN:
				if (isATargetView(v)){
					state = State.DOWN;
					setDownPosition(event);
					startLongpressTimer();
					targetView = v;
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

			if (maskedAction == MotionEvent.ACTION_POINTER_DOWN){
				// Should never happen we should always get a ACTION_DOWN first which would move us out of IDLE.
				return;
			}
			return;
		}

		if (state == State.DOWN){
			switch (action){
			case MotionEvent.ACTION_DOWN:
				// Happens when the backing window view gets the down event. Just ignore.
				return;
			case MotionEvent.ACTION_MOVE:
				if (isBigMove(event)){
					// Do not output our bigMove event here since we have not started a longPress. 
					state = State.IDLE;
				}
				return;
			case MotionEvent.ACTION_CANCEL:
				state = State.IDLE;
				return;
			case MotionEvent.ACTION_UP:
				state = State.IDLE;
				runClick(targetView);
				return;
			}
			
			if (maskedAction == MotionEvent.ACTION_POINTER_DOWN){
				state = State.IDLE;
				return;
			}
			return;
		}

		if (state == State.LONGPRESS){
			switch (action){
			case MotionEvent.ACTION_DOWN:
				// Happens when the backing window view gets the down event. Just ignore.
				return;
			case MotionEvent.ACTION_MOVE:
				if (isBigMove(event)){
					state = State.IDLE;
					runBigMove(targetView);
				}
				return;
			case MotionEvent.ACTION_CANCEL:
				state = State.IDLE;
				runAbort(targetView);
				return;
			case MotionEvent.ACTION_UP:
				state = State.IDLE;
				runEndLongpress(targetView);
				return;
			}
			
			if (maskedAction == MotionEvent.ACTION_POINTER_DOWN){
				state = State.IDLE;
				runAbort(targetView);				
				return;
			}
			return;
		}
	}

	private void longPressTimerFired() {
		if (state == State.IDLE){
			// This should never happen because any action that starts the timer should move us out of IDLE
			return;
		}

		if (state == State.DOWN){
			state = State.LONGPRESS;
			runStartLongpress(targetView);
			return;
		}

		if (state == State.LONGPRESS){
			// This should never happen because we should only get put in LONGPRESS as a result of the timer firing.
			return;
		}
	}

	//---------------
	// Fling detector
	//---------------
	private final class FlingListener extends SimpleOnGestureListener{
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            runFlingRight();
                        } else {
                            runFlingLeft();
                        }
                    }
                    result = true;
                } 
                else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            runFlingDown();
                        } else {
                            runFlingUp();
                        }
                    }
                    result = true;

            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
		}
	}
	
	// ---------------------------
	// Public events we broadcast:
	// ---------------------------
	// These public interface methods should be run on the UIThread of the activity that instantiated
	// this longpressTouchHandler. This these events may need to change views and 
	// only the original thread that created a view heirarchy can touch its views.
	public abstract void click(View v);
	public abstract void startLongpress(View v);
	public abstract void endLongpress(View v);
	public abstract void bigMove(View v);
	public abstract void abort(View v);	
	public abstract void flingRight();
	public abstract void flingLeft();
	public abstract void flingUp();
	public abstract void flingDown();

	private void runClick(final View v){
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				click(v);
			}
		});
	}

	private void runStartLongpress(final View v){
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				startLongpress(v);
			}
		});
	}

	private void runEndLongpress(final View v){
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				endLongpress(v);
			}
		});
	}

	private void runBigMove(final View v){
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				bigMove(v);
			}
		});
	}

	private void runAbort(final View v){
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				abort(v);
			}
		});
	}
	
	private void runFlingRight(){
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				flingRight();
			}
		});
	}
	
	private void runFlingLeft(){
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				flingLeft();
			}
		});
	}
	
	private void runFlingUp(){
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				flingUp();
			}
		});
	}
	
	private void runFlingDown(){
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				flingDown();
			}
		});
	}

	// -------
	// private
	// -------
	private boolean isATargetView(View v) {
		return targetViews.contains(v);
	}

	private boolean isBigMove(MotionEvent event) {
		Double a2 = Math.pow(downPosition[0] - (double) event.getRawX(), 2D);
		Double b2 = Math.pow(downPosition[1] - (double) event.getRawY(), 2D);
		Double limit = (double) Convenience.dpToPx(activity, (int) Math.pow(BIG_MOVE_DISTANCE, 2D));
		if (a2+b2 > limit){
			return true;
		} else {
			return false;
		}	}

	private void startLongpressTimer() {
		if (longPressTimer != null)
			longPressTimer.cancel();

		longPressTimer = new Timer();
		longPressTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				longPressTimerFired();
			}
		}, LONGPRESS_TIME);
	}

	private void setDownPosition(MotionEvent event) {
		downPosition[0] = (double) event.getRawX();
		downPosition[1] = (double) event.getRawY();
	}
}

