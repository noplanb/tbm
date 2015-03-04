package com.zazoapp.client;

import android.app.Activity;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.zazoapp.client.utilities.Convenience;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public abstract class ViewGroupGestureRecognizer {

	//----------
	// Interface
	//----------
	public abstract boolean click(View v);
	public abstract boolean startLongpress(View v);
	public abstract boolean endLongpress(View v);
	public abstract boolean bigMove(View v);
	public abstract boolean abort(View v, String reason);	
	
	// ---------
	// Constants
	// ---------
	private final String TAG = ViewGroupGestureRecognizer.class.getSimpleName();
	private static final Integer LONGPRESS_TIME = 175;
	private static final int BIG_MOVE_DISTANCE = 200;

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
	private ViewGroup viewGroup;
	private ArrayList<View> targetViews = new ArrayList<View>();
	private Integer state = State.IDLE;
	private View targetView;
	private Double[] downPosition = new Double[2];
	private Timer longPressTimer;
	private Boolean enabled = false;
	private Boolean intercept = false;
	private Boolean cancelOnMultiTouch = false;

	// -------------------
	// Constructor related
	// -------------------
	public ViewGroupGestureRecognizer(Activity a, ViewGroup vg, ArrayList<View>tvs){
		activity = a;
		viewGroup = vg;
		addTargetViews(tvs);
	}

	private void addTargetViews(ArrayList<View> tvs) {
		for (View v : tvs){
			addTargetView(v);
		}
	}
	
	public void addTargetView(View target){
		if (targetViews.contains(target))
			return;

		targetViews .add(target);
	}
	
	public void setCancelOnMultiTouch(Boolean value){
	    cancelOnMultiTouch = value;
	}

	
	//----------------------------
	// Methods that must be called
	//----------------------------
	// The viewGroup that instantiates this gesture recognizer must call these methods from its
	// equivalent overriden methods.

    public void dispatchTouchEvent(MotionEvent ev) {
        if ( state == State.LONGPRESS)
            intercept = true;
        
        if (ev.getAction() == MotionEvent.ACTION_DOWN)
            intercept = false;
        handleTouchEvent(ev);
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
			runAbort(targetView, "Gesture cancelled by controller.");

		state = State.IDLE;
	}

	private void handleTouchEvent(MotionEvent event) {
		if (!enabled)
			return;
					
		int action = event.getAction();
		int maskedAction = event.getActionMasked();

		if (state == State.IDLE){
			switch (action){
			case MotionEvent.ACTION_DOWN:
				int x = (int) event.getX();
				int y = (int) event.getY();
				targetView = pointToTargetView(x,y);
				if (targetView != null){
					state = State.DOWN;
					setDownPosition(event);
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
			    if (!cancelOnMultiTouch)
			        return;
			    
				state = State.IDLE;
				return;
			}
			return;
		}

		if (state == State.LONGPRESS){
			switch (action){
			case MotionEvent.ACTION_DOWN:
				// This should never happen but ignore rather than abort..
				return;
			case MotionEvent.ACTION_MOVE:
				if (isBigMove(event)){
					state = State.IDLE;
					runBigMove(targetView);
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
			if (maskedAction == MotionEvent.ACTION_POINTER_DOWN){
			    if (!cancelOnMultiTouch)
			        return;
			    
				state = State.IDLE;
				runAbort(targetView, "Two Finger Touch");				
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


	// ---------------------------
	// Public events we broadcast:
	// ---------------------------
	// These public interface methods should be run on the UIThread of the activity that instantiated
	// this ViewGroupGestureRecognizer. This these events may need to change views and 
	// only the original thread that created a view heirarchy can touch its views.
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

	private void runAbort(final View v, final String reason){
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				abort(v, reason);
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
		}
    }

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
	
	public View pointToTargetView(int x, int y) {
		Rect rect = new Rect();
		for (View v : targetViews){
			v.getHitRect(rect);
			if (rect.contains(x,y))
				return v;
		}
		return null;
	}

}