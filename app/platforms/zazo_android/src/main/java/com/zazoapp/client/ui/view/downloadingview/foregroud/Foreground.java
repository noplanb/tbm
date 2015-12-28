package com.zazoapp.client.ui.view.downloadingview.foregroud;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import com.zazoapp.client.ui.view.downloadingview.animation.IViewMoveDownAnimationListener;

/**
 * Created by sergii on 16.11.15.
 */
public class Foreground extends View implements IViewMoveDownAnimationListener {

    private static final float ICON_SIZE_COEF = 0.8f;
    private static final float LINE_Y_POS = 0.9f;
    private Paint drawPaintLine;

    private Paint drawPaintArrow;
    private Rect rectLine;
    private Rect rectIconDestination;
    private Rect rectIconSource;
    private Bitmap bitmap;

    private int iconShift = 0;
    private int iconHeight;

    public Foreground(Context context) {
        super(context);

        initRect();
        initPaint();
    }

    private void initRect() {
        rectLine = new Rect(0,0,0,0);
        rectIconDestination = new Rect(0,0,0,0);
        rectIconSource = new Rect(0,0,0,0);
    }

    private void initPaint() {
        drawPaintLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        drawPaintLine.setColor(Color.BLUE);
        drawPaintLine.setAntiAlias(true);
        drawPaintLine.setDither(true);

        drawPaintArrow = new Paint(Paint.ANTI_ALIAS_FLAG);
        drawPaintArrow.setAntiAlias(true);
        drawPaintArrow.setFilterBitmap(true);
        drawPaintArrow.setDither(true);
    }

    private void updateRect(int w, int h) {
        setIconHeight((int) (h * ICON_SIZE_COEF));
        updateIconDestinationRect(w);
        rectLine.set(0, (int) (h * LINE_Y_POS), w, h);
    }

    private void updateIconDestinationRect(int w) {
        float xRatio = 1f;
        float yRatio = 1f;
        if (rectIconSource.height() != 0 && rectIconSource.width() != 0) {
            if (getWidth() * rectIconSource.height() > rectIconSource.width() * getHeight()) {
                xRatio = rectIconSource.height() / (float) rectIconSource.width();
            } else {
                yRatio = rectIconSource.width() / (float) rectIconSource.height();
            }
        }
        rectIconDestination.set(0, iconShift, (int) (getWidth() * xRatio), (int) (getHeight() * yRatio + iconShift));
    }

    private int getIconHeight() {
        return iconHeight;
    }

    private void setIconHeight(int iconHeight) {
        this.iconHeight = iconHeight;
    }

    private void updateIconSourceRect() {
        rectIconSource.right = bitmap.getWidth();
        rectIconSource.bottom = bitmap.getHeight();
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        updateIconSourceRect();
        updateIconDestinationRect(rectIconDestination.right);
    }

    public void setLineColor(int aColor) {
        drawPaintLine.setColor(aColor);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(rectLine, drawPaintLine);
        if ( bitmap != null ){
            canvas.drawBitmap(bitmap, rectIconSource, rectIconDestination, drawPaintArrow);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateRect(w, h);
    }

    @Override
    public int getFinalValue() {
        return rectLine.bottom;
    }

    @Override
    public void setShift( int shift ){
        iconShift = shift;
        updateIconDestinationRect(rectIconDestination.right);
    }
}
