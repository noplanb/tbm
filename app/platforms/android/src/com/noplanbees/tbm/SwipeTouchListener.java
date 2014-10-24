package com.noplanbees.tbm;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class SwipeTouchListener implements OnTouchListener{
	private final String TAG = getClass().getSimpleName();
	
	private GestureDetector gestureDetector;
	
	public SwipeTouchListener (Context c){
		gestureDetector = new GestureDetector(c, new GestureListener());
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		gestureDetector.onTouchEvent(event);
	    return false;
	}
	
	private final class GestureListener extends SimpleOnGestureListener{
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
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
                        }
                    }
                    result = true;
                } 
                else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            onSwipeBottom();
                        } else {
                            onSwipeTop();
                        }
                    }
                    result = true;

            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
		}
	}
	public void onSwipeTop() {
		//Log.i(TAG, "onSwipeTop");
	}

	public void onSwipeBottom() {
		//Log.i(TAG, "onSwipeBottom");				
	}

	public void onSwipeLeft() {
		//Log.i(TAG, "onSwipeLeft");
	}

	public void onSwipeRight() {
		//Log.i(TAG, "onSwipeRight");
	}
}
