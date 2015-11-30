package com.zazoapp.client.ui.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import com.zazoapp.client.R;
import com.zazoapp.client.utilities.Convenience;

public class ClearableAutoCompleteTextView extends EditText {
	// was the text just cleared?
	boolean justCleared = false;

	// if not set otherwise, the default clear listener clears the text in the
	// text view
	private OnClearListener defaultClearListener = new OnClearListener() {

		@Override
		public void onClear() {
			ClearableAutoCompleteTextView et = ClearableAutoCompleteTextView.this;
			et.setText("");
		}
	};

	private OnClearListener onClearListener = defaultClearListener;

	// The image we defined for the clear button
	public Drawable imgClearButton = getResources().getDrawable(R.drawable.ic_clear_search_api_holo_light);
	public Drawable imgIconButton = getResources().getDrawable(R.drawable.icon_search);

	public interface OnClearListener {
		void onClear();
	}

	/* Required methods, not used in this implementation */
	public ClearableAutoCompleteTextView(Context context) {
		super(context);
		init();
	}

	/* Required methods, not used in this implementation */
	public ClearableAutoCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	/* Required methods, not used in this implementation */
	public ClearableAutoCompleteTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	void init() {
		// Set the bounds of the button
		this.setCompoundDrawablesWithIntrinsicBounds(imgIconButton, null, imgClearButton, null);
		setCompoundDrawablePadding(Convenience.dpToPx(getContext(), 10));

		// if the clear button is pressed, fire up the handler. Otherwise do
		// nothing
		this.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {

				ClearableAutoCompleteTextView et = ClearableAutoCompleteTextView.this;

				if (et.getCompoundDrawables()[2] == null)
					return false;

				if (event.getAction() != MotionEvent.ACTION_UP)
					return false;

				if (event.getX() > et.getWidth() - et.getPaddingRight() - imgClearButton.getIntrinsicWidth()) {
					onClearListener.onClear();
					justCleared = true;
				}
				return false;
			}
		});
	}

	public void setImgClearButton(Drawable imgClearButton) {
		this.imgClearButton = imgClearButton;
	}

	public void setOnClearListener(final OnClearListener clearListener) {
		this.onClearListener = clearListener;
	}

	public void hideClearButton() {
		this.setCompoundDrawables(null, null, null, null);
	}

	public void showClearButton() {
		this.setCompoundDrawablesWithIntrinsicBounds(null, null, imgClearButton, null);
	}

}