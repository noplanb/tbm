package com.zazoapp.client.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.widget.FrameLayout;
import com.zazoapp.client.Config;
import com.zazoapp.client.R;
import com.zazoapp.client.utilities.Convenience;

public class PreviewTextureFrame extends FrameLayout {

    private static final String TAG = "PreviewTextureFrame";
    private TextureView textureView;
    private View recordBorder;
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
        recordBorder = new View(getContext());
        recordBorder.setBackgroundResource(R.drawable.record_frame_border);
        recordBorder.setVisibility(GONE);
        addView(recordBorder, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setClipToPadding(true);
	}

	public boolean isRecording() {
		return isRecording;
	}

	public void setRecording(boolean isRecording) {
		this.isRecording = isRecording;
        recordBorder.setVisibility(isRecording ? VISIBLE : GONE);
		invalidate();
	}

	public void setSurfaceTextureListener(SurfaceTextureListener listener) {
		textureView.setSurfaceTextureListener(listener);
	}
	
	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		if(isRecording)
			drawIndicator(canvas);
	}

	private void drawIndicator(Canvas c){
        int padding = getResources().getDimensionPixelSize(R.dimen.record_frame_thickness);
        c.clipRect(padding, padding, c.getWidth() - padding, c.getHeight() - padding); // draw only inside frame

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.FILL);

		paint.setTextSize(Convenience.dpToPx(getContext(), 13)); //some size
		paint.setAntiAlias(true);
		paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
		paint.setTextAlign(Align.CENTER);
		FontMetrics fm = paint.getFontMetrics();
		String text = getContext().getString(R.string.recording);
		int text_x = c.getWidth()/2;
		int text_y = (int) (c.getHeight() - fm.bottom - padding);

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

        if (Config.SHOW_RED_DOT) {
            //draw circle
            int radius = Convenience.dpToPx(getContext(), 4);
            c.drawCircle(0 + 2 * radius,
                    text_y - radius,
                    radius, paint);
        }

	}
}
