/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.noplanbees.tbm.ui.view;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.noplanbees.tbm.multimedia.VideoRecorder;
import com.noplanbees.tbm.utilities.Convenience;

public class NineViewGroup extends ViewGroup {
	// ---------
	// Constants
	// ---------
	private static final String TAG = "NineViewGroup";
	private static final int BIG_MOVE_DISTANCE = 100;
	private static final int LONGPRESS_TIMEOUT = 175;

	private static final float ASPECT = 240F / 320F;

	private static final float MIN_MARGIN_DP = 10F;
	private static final float PADDING_DP = 5F;

	/**
	 * Represents an invalid position. All valid positions are in the range 0 to
	 * 1 less than the number of items in the current adapter.
	 */
	public static final int CENTER_CHILD = 4;
	public static final int INVALID_POSITION = -1;

	// ---------------------------------
	// State for longpress touch handler
	// ----------------------------------
	private static final class State {
		public static final int IDLE = 0;
		public static final int DOWN = 1;
		public static final int LONGPRESS = 2;
	}

	// TODO: a centerClick need to be added to this set of callbacks.
	// The click and longpress callbacks for surround views should just have 
	// position as a parameter.
	public interface GestureListener {
		boolean onClick(NineViewGroup parent, View view, int position, long id);
		boolean onStartLongpress(NineViewGroup parent, View view, int position, long id);
		boolean onEndLongpress();
		boolean onCancelLongpress();
		boolean onCancelLongpress(String reason);
	}

	public interface LayoutCompleteListener {
		void onLayoutComplete();
	}

	private Integer state = State.IDLE;
	private float[] downPosition = new float[2];
	private Timer longPressTimer;

	private GestureListener gestureListener;
	private LayoutCompleteListener layoutCompleteListener;

	private boolean isAttach;
	
	private VideoRecorder videoRecorder;


	//-------------
	// Constructors
	//-------------
	public NineViewGroup(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public NineViewGroup(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public NineViewGroup(Context context) {
		super(context);
		init();
	}

	private void init() {
		setClipChildren(false);
		setClipToPadding(false);
		addElementViews();
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b){
		layoutElementViews();
	}

	//----------------------
	// Callback registration
	//----------------------
	public void setVideoRecorder(VideoRecorder videoRecorder) {
		this.videoRecorder = videoRecorder;
	}

	public GestureListener getGestureListener() {
		return gestureListener;
	}

	public void setGestureListener(GestureListener gl) {
		this.gestureListener = gl;
	}

	public void setChildLayoutCompleteListener(LayoutCompleteListener childLayoutCompleteListener) {
		this.layoutCompleteListener = childLayoutCompleteListener;
	}

	
	//-------
	// Layout
	//-------
	private void addElementViews(){
		for (int i=0; i<9; i++){
			FrameLayout fl = new FrameLayout(getContext());
			fl.setBackgroundColor(Color.RED);
			addView(fl, i, new LayoutParams(elementWidth(),elementHeight()));
		}
	}
	
	private void layoutElementViews(){
		int x;
		int y;
		int i = 0;
		for (int row=0; row<3; row++){
			for (int col=0; col<3; col++){
				x = (int) (gutterLeft() + col * (elementWidth() + paddingPx()));
				y = (int) (gutterTop() + row * (elementHeight() + paddingPx()));
				FrameLayout fl = (FrameLayout) getChildAt(i);
				fl.layout(x, y, x + elementWidth(), y + elementHeight());
				i++;
			}
		}
	}
	
	// Layout helpers
	private Pair<Float, Float> elementSize(){
	    float width;
	    float height;
	    if (isHeightConstrained()){
	        height = (getHeight() - 2 * (marginPx() + paddingPx()) );
	        width = ASPECT * height;
	    } else {
	        width = ( getWidth() - 2 * (marginPx() + paddingPx()) ) / 3;
	        height = width / ASPECT;
	    }
	    return new Pair<Float, Float>(width, height);
	}
	
	private int elementWidth(){
		return Math.round(elementSize().first);
	}
	
	private int elementHeight(){
		return Math.round(elementSize().second);
	}
	
	private boolean isHeightConstrained() {
		return !isWidthConstrained();
	}
	
	private boolean isWidthConstrained(){
		 return (float) getWidth() / (float) getHeight() < ASPECT;
	}
	
	private Integer gutterTop(){		
		if (isHeightConstrained())
			return (int) marginPx();
		else
			return (int) (( getHeight() - 3*elementHeight() - 2*paddingPx() ) / 2);
		
	}
	
	private Integer gutterLeft(){
		if (isWidthConstrained())
			return (int) marginPx();
		else
			return (int) ( ( getWidth() - 3*elementWidth() -2*paddingPx()) / 2); 
	}
	
	private float paddingPx(){
		return Convenience.dpToPx(getContext(), PADDING_DP);
	}
	
	private float marginPx(){
		return Convenience.dpToPx(getContext(), MIN_MARGIN_DP);
	}
	


	//---------------
	// Public getters
	//---------------
    public View getCenterView() {
        return getChildAt(4);
    }

    /**
     * Returns view by desired populating order:
     * <pre>
     * 7 6 4
     * 5 8 0
     * 3 1 2
     * </pre>
     * 
     * By converting from this internal index order
     * <pre>
     * 0 1 2 
     * 3 4 5 
     * 6 7 8
     * </pre>
     * @return corresponding view
     */
    public View getSurroundingView(int position) {
    	return getChildAt(indexWithPosition(position));
    }
    
    private int indexWithPosition(int position){
    	switch (position) {
		case 0: return 5; 
		case 1: return 7; 
		case 2: return 8; 
		case 3: return 6; 
		case 4: return 2; 
		case 5: return 3; 
		case 6: return 1; 
		case 7: return 0; 
		case 8: return 4;
		default: throw new RuntimeException("Illegal position passed to getSurroundedView");
    	}
    }
    
    private int positionWithIndex(int index){
    	switch (index) {
		case 0: return 7; 
		case 1: return 6; 
		case 2: return 4; 
		case 3: return 5; 
		case 4: return 8; 
		case 5: return 0; 
		case 6: return 3; 
		case 7: return 1; 
		case 8: return 2;
		default: throw new RuntimeException("Illegal position passed to getSurroundedView");
    	}
    }
    
    //--------------------------
    // Longpress gesture handler
    //--------------------------
    // TODO: This long press touch handler needs to be factored into a separate LongpressGestureRecognizer class in the same way I 
    // originally had it in branch TBM master. Let us please not instantiate massive objects inline. It makes the code very unpleasant to read
    // and the files very long.
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		isAttach = true;
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		isAttach = false;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		return true;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled()) {
			// A disabled view that is clickable still consumes the touch
			// events, it just doesn't respond to them.
			return isClickable() || isLongClickable();
		}

		if (!isAttach)
			return false;

		int action = event.getAction();
		int maskedAction = event.getActionMasked();
		int x = (int) event.getX();
		int y = (int) event.getY();
		int motionPosition = pointToPosition(x, y);
		View child = getChildAt(motionPosition);

		if (state == State.IDLE) {
			switch (action) {
			case MotionEvent.ACTION_DOWN:
				state = State.DOWN;
				if(child != null){
					MotionEvent cp = MotionEvent.obtain(event);
					cp.offsetLocation(-child.getLeft(), -child.getTop());
					boolean consumed = child.dispatchTouchEvent(cp);
				}
				setDownPosition(event);
				startLongpressTimer(x, y);
				// targetView = v;
				return true;
			case MotionEvent.ACTION_CANCEL:
				// Safe to ignore since we would just stay in IDLE and do
				// nothing.
				return true;
			case MotionEvent.ACTION_MOVE:
				// Should never happen we should always get a ACTION_DOWN first
				// which would move us out of IDLE.
				return true;
			case MotionEvent.ACTION_UP:
				// Should never happen we should always get a ACTION_DOWN first
				// which would move us out of IDLE.
				return true;
			}

			if (maskedAction == MotionEvent.ACTION_POINTER_DOWN) {
				// Should never happen we should always get a ACTION_DOWN first
				// which would move us out of IDLE.
				return true;
			}
			return true;
		}

		if (state == State.DOWN) {
			switch (action) {
			case MotionEvent.ACTION_DOWN:
				// Happens when the backing window view gets the down event.
				// Just ignore.
				return true;
			case MotionEvent.ACTION_MOVE:
				if (isBigMove(event)) {
					// Do not output our bigMove event here since we have not
					// started a longPress.
					state = State.IDLE;
				}
				return true;
			case MotionEvent.ACTION_CANCEL:
				state = State.IDLE;
				return true;
			case MotionEvent.ACTION_UP:
				state = State.IDLE;
				if(child != null){
					MotionEvent cp = MotionEvent.obtain(event);
					cp.offsetLocation(-child.getLeft(), -child.getTop());
					boolean consumed = child.dispatchTouchEvent(cp);
				}
				runClick(event);
				return true;
			}

			if (maskedAction == MotionEvent.ACTION_POINTER_DOWN) {
				state = State.IDLE;
				return true;
			}
			return true;
		}

		if (state == State.LONGPRESS) {
			switch (action) {
			case MotionEvent.ACTION_DOWN:
				// This should never happen but ignore rather than abort..
				return true;
			case MotionEvent.ACTION_MOVE:
				if (isBigMove(event)) {
					state = State.IDLE;
					runBigMove(event);
				}
				return true;
			case MotionEvent.ACTION_CANCEL:
				state = State.IDLE;
				// This should never happen but endLongPress and send the video
				// rather than abort and lose it.
				runEndLongpress(event);
				return true;
			case MotionEvent.ACTION_UP:
				state = State.IDLE;
				runEndLongpress(event);
				return true;
			}

			// Second finger down aborts.
			if (maskedAction == MotionEvent.ACTION_POINTER_DOWN) {
				state = State.IDLE;
				runAbort(event, "Two Finger Touch");
				return true;
			}
			return true;
		}

		return false;
	}

	private boolean isBigMove(MotionEvent event) {
		double a2 = Math.pow(downPosition[0] - (double) event.getRawX(), 2D);
		double b2 = Math.pow(downPosition[1] - (double) event.getRawY(), 2D);
		double limit = (double) Convenience.dpToPx(getContext(), (int) Math.pow(BIG_MOVE_DISTANCE, 2D));
		if (a2 + b2 > limit) {
			return true;
		} else {
			return false;
		}
	}

	private void startLongpressTimer(final int x, final int y) {
		cancelLongpressTimer();
	
		longPressTimer = new Timer();
		longPressTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				Log.d(TAG, "startLongpressTimer.run");
				longPressTimerFired(x, y);
			}
		}, LONGPRESS_TIMEOUT);
	}

	private void cancelLongpressTimer() {
		if (longPressTimer != null)
			longPressTimer.cancel();
	}

	private void longPressTimerFired(int x, int y) {
		if (state == State.IDLE) {
			// This should never happen because any action that starts the timer
			// should move us out of IDLE
			return;
		}

		if (state == State.DOWN) {
			state = State.LONGPRESS;
			runStartLongpress(x, y);
			return;
		}

		if (state == State.LONGPRESS) {
			// This should never happen because we should only get put in
			// LONGPRESS as a result of the timer firing.
			return;
		}
	}

	private void setDownPosition(MotionEvent event) {
		downPosition[0] = event.getRawX();
		downPosition[1] = event.getRawY();
	}

	/**
	 * Maps a point to a position in the list.
	 * 
	 * @param x
	 *            X in local coordinate
	 * @param y
	 *            Y in local coordinate
	 * @return The position of the item which contains the specified point, or
	 *         {@link #INVALID_POSITION} if the point does not intersect an
	 *         item.
	 */
	// TODO: When this handler is factored out we should just pass it a list of target views 
	// It should return  one of those views when an event occurs in its rect. -Sani
	public int pointToPosition(int x, int y) {
		Rect rect = new Rect();

		final int count = getChildCount();
		for (int i = count - 1; i >= 0; i--) {
			final View child = getChildAt(i);
			if (child.getVisibility() == View.VISIBLE) {
				child.getHitRect(rect);
				if (rect.contains(x, y)) {
					return i;
				}
			}
		}
		return INVALID_POSITION;
	}
	
	// TODO: This code was and still is Ugly. The and the ugliness is repeated twice. Go back to the way LongpressTouchhanler worked in my master branch
	// events should bubble down to a target view and just return that view. Then pull the index from the view.
	// I cleaned up a little for now but will need to factor all of this out. -Sani
	private void runClick(MotionEvent ev) {
		int x = (int) ev.getX();
		int y = (int) ev.getY();
		int index = pointToPosition(x, y);
		Log.d(TAG, "runClick index: " +  index);
		final int position;
		if(index == CENTER_CHILD) {
			position = INVALID_POSITION;
			// TODO: added a return here for now. But eventually when this is factored the gestureListener in nineViewGroup should 
			// fire a centerClick event.
			return;
		} else {
			position = positionWithIndex(index);
		}

		final View child = getChildAt(index);
		post(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "click: " + position);
				if (gestureListener != null)
					gestureListener.onClick(NineViewGroup.this, child, position, position);
			}
		});
	}

	private void runStartLongpress(int x, int y) {

		int index = pointToPosition(x, y);
		final int position;
		if(index == CENTER_CHILD){
			position = INVALID_POSITION;
			return;
		} else {
			position = positionWithIndex(index);
		}

		final View child = getChildAt(index);
		post(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "startLongpress: " + position);
				if (gestureListener != null)
					gestureListener.onStartLongpress(NineViewGroup.this, child, position, position);
			}
		});
	}

	private void runEndLongpress(MotionEvent ev) {
		cancelLongpressTimer();
		post(new Runnable() {
			@Override
			public void run() {
				if (gestureListener != null)
					gestureListener.onEndLongpress();
			}
		});
	}

	private void runBigMove(MotionEvent ev) {
		post(new Runnable() {
			@Override
			public void run() {
				if (gestureListener != null)
					gestureListener.onCancelLongpress();
			}
		});
	}

	private void runAbort(MotionEvent ev, final String reason) {
		post(new Runnable() {
			@Override
			public void run() {
				if (gestureListener != null)
					gestureListener.onCancelLongpress(reason);
			}
		});
	}

}
