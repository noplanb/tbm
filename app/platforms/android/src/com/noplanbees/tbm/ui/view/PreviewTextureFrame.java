package com.noplanbees.tbm.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.FrameLayout;

import com.noplanbees.tbm.utilities.Convenience;
import com.noplanbees.tbm.R;

public class PreviewTextureFrame extends FrameLayout {

    private static final String TAG = "PreviewTextureFrame";
    private TextureView textureView;
	private boolean isRecording;

	public PreviewTextureFrame(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public PreviewTextureFrame(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public PreviewTextureFrame(Context context) {
		super(context);
		init();
	}

	private void init() {
		textureView = new TextureView(getContext());
		addView(textureView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
	}

	public boolean isRecording() {
		return isRecording;
	}

	public void setRecording(boolean isRecording) {
		this.isRecording = isRecording;
		invalidate();
	}

	public void setSurfaceTextureListener(SurfaceTextureListener listener) {
		textureView.setSurfaceTextureListener(listener);
	}
	
	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		Log.d(TAG, "dispatchDraw " + isRecording);
		if(isRecording)
			drawIndicator(canvas);
	}
	
	private void drawIndicator(Canvas c){
		Rect border = new Rect(0, 0, c.getWidth(), c.getHeight());
		
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.FILL);

		paint.setTextSize(Convenience.dpToPx(getContext(), 13)); //some size
		paint.setAntiAlias(true);
		paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
		paint.setTextAlign(Align.CENTER);
		FontMetrics fm = paint.getFontMetrics();
		String text = "Recording";
		int text_x = c.getWidth()/2;
		int text_y = (int) (c.getHeight() - fm.bottom);

		RectF r = new RectF(0, 
				text_y + fm.top,
				c.getWidth(), 
				c.getHeight());
		
		//draw text background
		paint.setColor(Color.parseColor("#30000000"));
		c.drawRect(r , paint);

		//draw text
		paint.setColor(Color.WHITE);
		c.drawText(text, text_x, text_y, paint);

		paint.setColor(getResources().getColor(R.color.recording_border_color));
		
		//draw circle
		int radius = Convenience.dpToPx(getContext(), 4);
		c.drawCircle(0 + 2*radius, 
				text_y - radius,
				radius, paint);
		
		//draw borders
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(Convenience.dpToPx(getContext(), 2.5f));
		c.drawRect(border, paint);

	}
}
