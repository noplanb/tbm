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

    private static final float LINE_Y_COEF = 0.11f;
    private static final float CONTENT_COEF = 0.5f;
    private Paint drawPaintLine;

    private Paint drawPaintArrow;
    private Rect rectLine;
    private Rect rectIconDestination;
    private Rect rectContent;
    private Rect rectIconSource;
    private Bitmap bitmap;

    private int iconShift = 0;

    public Foreground(Context context) {
        super(context);

        initRect();
        initPaint();
    }

    private void initRect() {
        rectLine = new Rect();
        rectIconDestination = new Rect();
        rectIconSource = new Rect();
        rectContent = new Rect();
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
        int contentShiftX = (int) (w * 0.5f * CONTENT_COEF);
        int contentShiftY = (int) (h * 0.5f * CONTENT_COEF);
        rectContent.set(contentShiftX, contentShiftY, w - contentShiftX, h - contentShiftY);
        updateIconDestinationRect(w);
    }

    private void updateIconDestinationRect(int w) {
        float ratio = 1f;
        int rectLineHeight = (int) (rectContent.height() * LINE_Y_COEF);
        int iconMaxHeight = rectContent.height() - rectLineHeight * 2;
        int iconMaxWidth = rectContent.width();
        if (rectIconSource.height() != 0 && rectIconSource.width() != 0) {
            if (iconMaxWidth * rectIconSource.height() > rectIconSource.width() * iconMaxHeight) {
                ratio = iconMaxHeight / (float) rectIconSource.height();
            } else {
                ratio = iconMaxWidth / (float) rectIconSource.width();
            }
        }
        float newWidth = rectIconSource.width() * ratio;
        float newHeight = rectIconSource.height() * ratio;
        int xOffset = (int) ((iconMaxWidth - newWidth) * 0.5) + rectContent.left;
        int yOffset = (int) ((iconMaxHeight - newHeight) * 0.5) + rectContent.top + iconShift;

        rectIconDestination.set(xOffset, yOffset , (int) newWidth + xOffset, (int) newHeight + yOffset);
        rectLine.set(rectIconDestination.left, rectContent.bottom - rectLineHeight, rectIconDestination.right, rectContent.bottom);
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
