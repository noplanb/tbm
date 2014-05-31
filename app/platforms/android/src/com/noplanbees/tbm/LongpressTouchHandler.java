package com.noplanbees.tbm;

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class LongpressTouchHandler {
	final String TAG = this.getClass().getSimpleName();
	View target;
	Hashtable<Integer, String> actionToString = new Hashtable<Integer, String>();
	Double downPos[] = new Double[2];
	Double lastPos[] = new Double[2];
	Boolean bigMoveCalled;
	Boolean isLongPress;
	Timer longPressTimer;
	Activity activity;
	
	public LongpressTouchHandler (Activity activity, View t){
		target = t;
		this.activity = activity;
		setupActionToString();
		setupEventListeners();
	}
    
	public void click(View v) {
		Log.i(TAG, "click");
	}
	
	public void startLongpress(View v){
		Log.i(TAG, "startLongpress");
		isLongPress = true;
	}
	
	public void endLongpress(View v){
		Log.i(TAG, "endLongpress");
	}
	
	public void bigMove(View v){
		Log.i(TAG, "bigMove");
		bigMoveCalled = true;
	}
	
	private void setupActionToString(){
		actionToString.put(MotionEvent.ACTION_CANCEL, "ACTION_CANCEL");
		actionToString.put(MotionEvent.ACTION_DOWN, "ACTION_DOWN");
		actionToString.put(MotionEvent.ACTION_MASK, "ACTION_MASK");
		actionToString.put(MotionEvent.ACTION_MOVE, "ACTION_MOVE");
		actionToString.put(MotionEvent.ACTION_OUTSIDE, "ACTION_OUTSIDE");
		actionToString.put(MotionEvent.ACTION_POINTER_DOWN, "ACTION_POINTER_DOWN");
		actionToString.put(MotionEvent.ACTION_POINTER_UP, "ACTION_POINTER_UP");
		actionToString.put(MotionEvent.ACTION_UP, "ACTION_UP");
	}
	
	private boolean handleTouch(View v, MotionEvent event) {
		double x = event.getX();
		double y = event.getY();
		int a = event.getAction();
		
//		Log.i(TAG, "Touch Event:" + actionToString.get(event.getAction()) + " " + Double.toString(x) + ", " + Double.toString(y));
		
		switch (a) {
		case MotionEvent.ACTION_DOWN:
			downPos[0] = x;
			downPos[1] = y;
			bigMoveCalled = false;
			isLongPress = false;
			if (longPressTimer != null)
				longPressTimer.cancel();
			longPressTimer = new Timer();
			longPressTimer.schedule(new TimerTask() {
				@Override
				public void run() {
				    // These public interface methods should be run on the UIThread of the activity that instantiated
				    // this longpress touchHandler. This is because Longpress events may need to change views and 
					// only the original thread that created a view heirarchy can touch its views.
					activity.runOnUiThread(new Runnable(){
						@Override
						public void run() { startLongpress(target);}
					});
				}
			}, 200);
            break;
            
		case MotionEvent.ACTION_MOVE:
			if (bigMoveCalled)
				return false;
			lastPos[0] = x;
			lastPos[1] = y;
			if (isBigMove()) {
				longPressTimer.cancel();
				bigMove(target);
				return false;
			}
			break;
			
		case MotionEvent.ACTION_UP:
			longPressTimer.cancel();
			if (!bigMoveCalled){
				if (isLongPress){
					endLongpress(target);
				} else {
					click(target);
				}
			}
			
		default:
			break;
		}
		return true;
	}
	
	private boolean isBigMove() {
		Double a2 = Math.pow(downPos[0] - lastPos[0], 2D);
		Double b2 = Math.pow(downPos[1] - lastPos[1], 2D);
		Double c2 = a2 + b2;
//		Log.i(TAG, "c2 = " + Double.toString(c2));
		if (a2+b2 > 20000D){
			return true;
		} else {
			return false;
		}
	}

	private void setupEventListeners() {
		target.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return handleTouch(v,event);
			}
		});		
	}

}
