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

package com.noplanbees.tbm.ui;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.noplanbees.tbm.Convenience;
import com.noplanbees.tbm.VideoRecorder;

public class NineViewGroup extends ViewGroup {
	// ---------
	// Constants
	// ---------
	private static final String TAG = "NineViewGroup";
	private static final int BIG_MOVE_DISTANCE = 100;

	private static final int MATRIX_DIMENSIONS = 3;

	private static final float ASPECT = 240F / 320F;

	private static final int MIN_MARGIN_DP = 10;
	private static final int PADDING_DP = 5;

	private static final int TOTAL_CHILD_COUNT = 9;
	private static final int CENTRAL_VIEW_POSITION = TOTAL_CHILD_COUNT / 2;

	/**
	 * Represents an invalid position. All valid positions are in the range 0 to
	 * 1 less than the number of items in the current adapter.
	 */
	public static final int INVALID_POSITION = -1;

	// -----
	// State
	// -----
	private static final class State {
		public static final int IDLE = 0;
		public static final int DOWN = 1;
		public static final int LONGPRESS = 2;
	}

	public interface OnItemTouchListener {
		boolean onItemClick(NineViewGroup parent, View view, int position, long id);

		boolean onItemLongClick(NineViewGroup parent, View view, int position, long id);

		boolean onItemStopTouch();

		boolean onCancelTouch();

		boolean onCancelTouch(String reason);
	}

	public interface OnChildLayoutCompleteListener {
		void onChildLayoutComplete();
	}

	private Integer state = State.IDLE;
	private float[] downPosition = new float[2];
	private Timer longPressTimer;

	private BaseAdapter adapter;
	private OnItemTouchListener itemClickListener;
	private OnChildLayoutCompleteListener childLayoutCompleteListener;

	private boolean isAttach;
	private DataSetObserver dataSetObserver;
	private boolean isDirty;

	private VideoRecorder videoRecorder;

	private SparseArray<View> recycleBin;

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
		// allow childrens to extends parent border
		setClipChildren(false);
		setClipToPadding(false);
	}

	public void setAdapter(BaseAdapter adapter) {
		if (this.adapter != null) {
			this.adapter.unregisterDataSetObserver(dataSetObserver);
		}
		if (recycleBin == null) {
			recycleBin = new SparseArray<View>(TOTAL_CHILD_COUNT);
		}
		recycleBin.clear();

		dataSetObserver = new AdapterDataSetObserver();
		adapter.registerDataSetObserver(dataSetObserver);

		this.adapter = adapter;
	}

	public BaseAdapter getAdapter() {
		return adapter;
	}

	public void setVideoRecorder(VideoRecorder videoRecorder) {
		this.videoRecorder = videoRecorder;
	}

	public OnItemTouchListener getItemClickListener() {
		return itemClickListener;
	}

	public void setItemClickListener(OnItemTouchListener itemClickListener) {
		this.itemClickListener = itemClickListener;
	}

	public void setChildLayoutCompleteListener(OnChildLayoutCompleteListener childLayoutCompleteListener) {
		this.childLayoutCompleteListener = childLayoutCompleteListener;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (adapter == null || videoRecorder == null)
			return;

		View preview = videoRecorder.getView();
		int childCount = getChildCount();
		if (isDirty == true && childCount > 0) {
			// = getChildAt(CENTRAL_VIEW_POSITION);
			int j = 0;
			while (getChildCount() > 1) {
				View child = getChildAt(j);
				if (preview != null && child.equals(preview)) {
					j++;
					continue;
				}
				removeView(child);
			}
			Log.d(TAG, "removed all childs");
		}

		if (childCount < adapter.getCount() || isDirty) {
			int position = 0;
			int bottomEdge = 0;
			while (bottomEdge < getHeight() && position < TOTAL_CHILD_COUNT) {
				int rightEdge = 0;
				int measuredHeight = 0;
				while (rightEdge < getWidth() && position < TOTAL_CHILD_COUNT) {
					View newChild;
					if (position == CENTRAL_VIEW_POSITION) {
						newChild = videoRecorder.getView();
						if (!isDirty)
							addAndMeasureChild(newChild, position);
					} else {
						int childPos;
						if (position > CENTRAL_VIEW_POSITION)
							childPos = position - 1;
						else
							childPos = position;
						View convertView = recycleBin.get(childPos);
						newChild = adapter.getView(childPos, convertView, this);
						recycleBin.put(childPos, newChild);
						addAndMeasureChild(newChild, position);
					}
					rightEdge += newChild.getMeasuredWidth();
					measuredHeight = getHeight() / MATRIX_DIMENSIONS;
					position++;
				}
				bottomEdge += measuredHeight;
			}
		}
		layoutChildren();
		isDirty = false;
	}

	/**
	 * Adds a view as a child view and takes care of measuring it
	 * 
	 * @param child
	 *            The view to add
	 */
	private void addAndMeasureChild(View child, int position) {
		LayoutParams params = child.getLayoutParams();
		if (params == null) {
			params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		}
		addViewInLayout(child, position, params, true);

		int itemWidth = (getWidth() - 2 * Convenience.dpToPx(getContext(), MIN_MARGIN_DP) - 2 * Convenience.dpToPx(
				getContext(), PADDING_DP)) / MATRIX_DIMENSIONS;
		int itemHeight = (int) ((float) itemWidth / ASPECT);
		child.measure(MeasureSpec.EXACTLY | itemWidth, MeasureSpec.EXACTLY | itemHeight);
	}

	/**
	 * Positions the children at the "correct" positions
	 */
	private void layoutChildren() {
		View child = getChildAt(0);
		int numCol = getWidth() / child.getMeasuredWidth();

		int padding_px = Convenience.dpToPx(getContext(), PADDING_DP);
		int startX = (getWidth() - 3 * child.getMeasuredWidth() - 2 * padding_px) / 2;
		startX = (startX < MIN_MARGIN_DP) ? MIN_MARGIN_DP : startX;
		int startY = (getHeight() - (child.getMeasuredHeight() + padding_px) * 3) / 2;
		startY = (startY < MIN_MARGIN_DP) ? MIN_MARGIN_DP : startY;

		for (int index = 0; index < getChildCount(); index++) {
			child = getChildAt(index);
			int width = child.getMeasuredWidth();
			int height = child.getMeasuredHeight();
			int mod = index / numCol;
			int left = startX + (index - mod * numCol) * (width + padding_px);
			int top = startY + mod * (height + padding_px);

			child.layout(left, top, left + width, top + height);
		}

		if (childLayoutCompleteListener != null) {
			childLayoutCompleteListener.onChildLayoutComplete();
		}

		Log.d(TAG, "layoutChildren");
	}

	// /**
	// * Positions the children at the "correct" positions
	// */
	// private void layoutChildren() {
	// View child = getChildAt(0);
	// int numCol = getWidth()/child.getMeasuredWidth();
	//
	// int width=0, height=0;
	// int margin_px = Convenience.dpToPx(getContext(), MARGIN_DP);
	// int widthFrame = (getWidth() - 2*margin_px)/MATRIX_DIMENSIONS;
	// int heightFrame = (getHeight() - 2*margin_px)/MATRIX_DIMENSIONS;
	// int paddingHor = (widthFrame - child.getMeasuredWidth())/2;
	// int paddingVert = (heightFrame - child.getMeasuredHeight())/2;
	//
	// for (int index = 0; index < getChildCount(); index++) {
	// child = getChildAt(index);
	// width = child.getMeasuredWidth();
	// height = child.getMeasuredHeight();
	// int mod = index / numCol;
	// int left = (index - mod * numCol) * widthFrame + paddingHor + margin_px;
	// int top = mod * heightFrame + paddingVert + margin_px;
	//
	// child.layout(left, top, left + width, top + height);
	// }
	//
	// if(childLayoutCompleteListener!=null){
	// childLayoutCompleteListener.onChildLayoutComplete();
	// }
	//
	// Log.d(TAG, "layoutChildren");
	// }

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

		if (state == State.IDLE) {
			switch (action) {
			case MotionEvent.ACTION_DOWN:
				state = State.DOWN;
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

	private void startLongpressTimer(final int x, final int y) {
		cancelLongpressTimer();

		longPressTimer = new Timer();
		longPressTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				Log.d(TAG, "startLongpressTimer.run");
				longPressTimerFired(x, y);
			}
		}, ViewConfiguration.getLongPressTimeout());
	}

	private void cancelLongpressTimer() {
		if (longPressTimer != null)
			longPressTimer.cancel();
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
	public int pointToPosition(int x, int y) {
		Rect frame = new Rect();

		final int count = getChildCount();
		for (int i = count - 1; i >= 0; i--) {
			final View child = getChildAt(i);
			if (child.getVisibility() == View.VISIBLE) {
				child.getHitRect(frame);
				if (frame.contains(x, y)) {
					return i;
				}
			}
		}
		return INVALID_POSITION;
	}

	private void runClick(MotionEvent ev) {
		int x = (int) ev.getX();
		int y = (int) ev.getY();
		int _motionPosition = pointToPosition(x, y);
		final int motionPosition;
		if(_motionPosition == CENTRAL_VIEW_POSITION)
			motionPosition = INVALID_POSITION;
		else if(_motionPosition>CENTRAL_VIEW_POSITION)
			motionPosition = _motionPosition - 1;
		else
			motionPosition = _motionPosition;

		final View child = getChildAt(_motionPosition);
		final long id = adapter.getItemId(motionPosition);
		post(new Runnable() {
			@Override
			public void run() {
				itemClickListener.onItemClick(NineViewGroup.this, child, motionPosition, id);
			}
		});
	}

	private void runStartLongpress(int x, int y) {

		int _motionPosition = pointToPosition(x, y);
		final int motionPosition;
		if(_motionPosition == CENTRAL_VIEW_POSITION)
			motionPosition = INVALID_POSITION;
		else if(_motionPosition>CENTRAL_VIEW_POSITION)
			motionPosition = _motionPosition - 1;
		else
			motionPosition = _motionPosition;
		final View child = getChildAt(_motionPosition);
		final long id = adapter.getItemId(motionPosition);
		post(new Runnable() {
			@Override
			public void run() {
				if (itemClickListener != null)
					itemClickListener.onItemLongClick(NineViewGroup.this, child, motionPosition, id);
			}
		});
	}

	private void runEndLongpress(MotionEvent ev) {
		cancelLongpressTimer();
		post(new Runnable() {
			@Override
			public void run() {
				if (itemClickListener != null)
					itemClickListener.onItemStopTouch();
			}
		});
	}

	private void runBigMove(MotionEvent ev) {
		post(new Runnable() {
			@Override
			public void run() {
				if (itemClickListener != null)
					itemClickListener.onCancelTouch();
			}
		});
	}

	private void runAbort(MotionEvent ev, final String reason) {
		post(new Runnable() {
			@Override
			public void run() {
				if (itemClickListener != null)
					itemClickListener.onCancelTouch(reason);
			}
		});
	}

	private class AdapterDataSetObserver extends DataSetObserver {
		@Override
		public void onChanged() {
			isDirty = true && getChildCount() > 0;
			requestLayout();
		}

		@Override
		public void onInvalidated() {
			requestLayout();
		}
	}
}
