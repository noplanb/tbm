package com.noplanbees.tbm.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.EditText;

import com.noplanbees.tbm.utilities.Convenience;

public class SignEditText extends EditText {

	private boolean isPlusDrawn;

	public SignEditText(Context context) {
		super(context);
	}

	public SignEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public SignEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		if(!isPlusDrawn){
			drawSign();
			isPlusDrawn = true;
		}
		super.onDraw(canvas);
	}

	void drawSign() {
			Rect bounds = new Rect();
			Paint textPaint = getPaint();
			textPaint.setStyle(Style.FILL);
			textPaint.setColor(Color.WHITE);
			textPaint.setTextSize(getTextSize());
			String text = "+";
			textPaint.getTextBounds(text, 0, text.length(), bounds);
			int height = getMeasuredHeight() - getPaddingBottom() - getPaddingTop();
			int width = bounds.width();

			Bitmap bm = Bitmap.createBitmap(width, height, Config.ARGB_4444);

			Canvas canvas = new Canvas(bm);
			int xPos = (0);
			int yPos = (int) ((canvas.getHeight() / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)) ; 

			canvas.drawText(text, xPos, yPos, textPaint);

			Drawable drawable = new BitmapDrawable(getResources(), bm);

			this.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
			setCompoundDrawablePadding((int) Convenience.pxToDp(getContext(), width));
	}
}