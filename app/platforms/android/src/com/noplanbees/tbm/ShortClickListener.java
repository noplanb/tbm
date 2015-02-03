package com.noplanbees.tbm;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.noplanbees.tbm.utilities.Convenience;

public class ShortClickListener {
	private static String TAG = ShortClickListener.class.getSimpleName();
	
	//-----------------
	// Public Interface
	//-----------------
	public interface Callbacks{
		void onShortClick(View v);
	}
	
	// -----
	// State
	// -----
	private static final class State{
		public static final Integer IDLE = 0;
		public static final Integer DOWN = 1;
	}
	
	private static final Integer SHORT_TIME = 80;
	private static final int MOVE_DISTANCE = 50;

	
	//-----------------------------------
	// Instantiation and member variables
	//-----------------------------------
	private Activity activity;
	private View view;
	private ShortClickListener.Callbacks delegate;
	private Integer state = State.IDLE;
	private Double[] downPosition = new Double[2];
	private Timer cancelTimer;
	
	public ShortClickListener(Activity a, View v, ShortClickListener.Callbacks delegate){
		activity = a;
		view = v;
		this.delegate = delegate;
		addListener();
	}


	private void addListener() {
		view.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Log.d(TAG, "ontouch!!");
				handleTouch(v, event);
				return true;
			}
		});
	}


	protected void handleTouch(View v, MotionEvent event) {
		int action = event.getAction();
		int maskedAction = event.getActionMasked();

		if (state == State.IDLE){
			Log.d(TAG, "STATE IS IDLE");
			switch (action){
			case MotionEvent.ACTION_DOWN:
				Log.d(TAG, "ACTION_DOWN");
				state = State.DOWN;
				startCancelTimer();
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
			Log.d(TAG, "STATE IS DOWN");
			switch (action){
			case MotionEvent.ACTION_DOWN:
				return;
			case MotionEvent.ACTION_MOVE:
				if (isBigMove(event)){
					state = State.IDLE;
				}
				return;
			case MotionEvent.ACTION_CANCEL:
				state = State.IDLE;
				return;
			case MotionEvent.ACTION_UP:
				Log.d(TAG, "ACTION_UP");
				state = State.IDLE;
				runClick();
				return;
			}
			
			if (maskedAction == MotionEvent.ACTION_POINTER_DOWN){
				state = State.IDLE;
				return;
			}
			return;
		}
	}
	
	
	//---------------
	// Helper methods
	//---------------
	private boolean isBigMove(MotionEvent event) {
		Double a2 = Math.pow(downPosition[0] - (double) event.getRawX(), 2D);
		Double b2 = Math.pow(downPosition[1] - (double) event.getRawY(), 2D);
		Double limit = (double) Convenience.dpToPx(activity, (int) Math.pow(MOVE_DISTANCE, 2D));
		if (a2+b2 > limit){
			return true;
		} else {
			return false;
		}	}

	private void startCancelTimer() {
		if (cancelTimer != null)
			cancelTimer.cancel();

		cancelTimer = new Timer();
		cancelTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				cancelTimerFired();
			}
		}, SHORT_TIME);
	}

	private void setDownPosition(MotionEvent event) {
		downPosition[0] = (double) event.getRawX();
		downPosition[1] = (double) event.getRawY();
	}

	
	private void cancelTimerFired() {
		if (state == State.DOWN){
			state = State.IDLE;
			return;
		}
	}
	
	private void runClick(){
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				delegate.onShortClick(view);
			}
		});
	}
}
