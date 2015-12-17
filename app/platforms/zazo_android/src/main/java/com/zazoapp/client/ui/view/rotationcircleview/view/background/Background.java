package com.zazoapp.client.ui.view.rotationcircleview.view.background;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/**
 * Created by sergii on 12.11.15.
 */
public class Background extends View implements IBackground{

    private float centerX;
    private float centerY;
    private float radiusRing;
    private float radiusCircle;
    private Paint drawPaintRing;
    private Paint drawPaintCircle;

    public Background(Context context) {
        super(context);
        initRing( );
        initCircle();
    }

    private void initCircle() {
        drawPaintCircle = new Paint();
        drawPaintCircle.setColor(Color.RED);
        drawPaintCircle.setAntiAlias(true);
    }

    private void initRing() {
        drawPaintRing = new Paint(Paint.ANTI_ALIAS_FLAG);
        drawPaintRing.setStyle(Paint.Style.STROKE);
        drawPaintRing.setStrokeWidth(4);
        drawPaintRing.setColor(Color.GREEN);
        drawPaintRing.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(centerX, centerY, radiusRing, drawPaintRing);
        canvas.drawCircle(centerX, centerY, radiusCircle, drawPaintCircle);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateCenter();
        updatePaints();
    }

    private void updateCenter() {
        centerX = getWidth()/2;
        centerY = getHeight()/2;
    }

    private void updatePaints() {
        float delta = drawPaintRing.getStrokeWidth()/2;
        radiusRing = Math.min(centerX, centerX) - delta;
        radiusCircle = radiusRing - delta + 1;
    }

    @Override
    public int getCircleColor() {
        return drawPaintCircle.getColor();
    }

    @Override
    public void setCircleColor(int aColor) {
        drawPaintCircle.setColor(aColor);
    }

    @Override
    public int getRingColor() {
        return drawPaintRing.getColor();
    }

    @Override
    public void setRingColor(int aColor) {
        drawPaintRing.setColor(aColor);
    }

    @Override
    public float getRingThickness() {
        return drawPaintRing.getStrokeWidth();
    }

    @Override
    public void setRingThickness(float aThickness) {
        drawPaintRing.setStrokeWidth(aThickness);
        updatePaints();
    }
}


