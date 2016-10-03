package com.zazoapp.client.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import com.zazoapp.client.BuildConfig;
import com.zazoapp.client.utilities.Convenience;

/**
 * Created by skamenkovych@codeminders.com on 9/15/2016.
 */
public class CropImageView extends AppCompatImageView {

    private static final String TAG = CropImageView.class.getSimpleName();
    private final float minDistance;

    // These matrices will be used to move and zoom image
    Matrix matrix = new Matrix();
    private final RectF imageRect = new RectF();
    private final RectF tempRect = new RectF();

    private RectF cropRectRel;
    private RectF cropRect = new RectF();
    private RectF cropRectCircle = new RectF();
    private RectF cropImageRect;

    // Remember some things for zooming
    private PointF start = new PointF();
    private Paint cropPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float cropRectRadius;
    private int imageRotation;

    public CropImageView(Context context) {
        this(context, null, 0);
    }

    public CropImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.f;
    private float mMinScale = 1.f;

    public CropImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // Create our ScaleGestureDetector
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        minDistance = Convenience.dpToPx(getContext(), 40f);
        cropPaint.setStyle(Paint.Style.STROKE);
        cropPaint.setColor(Color.WHITE);
        cropPaint.setStrokeWidth(Convenience.dpToPx(getContext(), 2f));
        cropRectRadius = Convenience.dpToPx(getContext(), 5f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getDrawable() == null) {
            return false;
        }
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        mScaleDetector.onTouchEvent(event);
        if (mScaleDetector.isInProgress()) {
            start.set(event.getX(), event.getY());
            return true;
        }
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
                start.set(event.getX(), event.getY());
                tempRect.set(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
                matrix.mapRect(tempRect);
                float xShift = 0, yShift = 0;
                if (tempRect.left > cropRect.left) {
                    xShift = cropRect.left - tempRect.left;
                } else if (tempRect.right < cropRect.right) {
                    xShift = cropRect.right - tempRect.right;
                }
                if (tempRect.top > cropRect.top) {
                    yShift = cropRect.top - tempRect.top;
                } else if (tempRect.bottom < cropRect.bottom) {
                    yShift = cropRect.bottom - tempRect.bottom;
                }
                matrix.postTranslate(xShift, yShift);
                setImageMatrix(matrix);
                break;
            default:
                start.set(event.getX(), event.getY());
                break;
        }
        return true;
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        setImageDrawable(drawable, null, null);
    }

    /**
     * @param cropRect relative rect RectF(0f, 0f, 1f, 1f) to view
     * @param cropImageRect interesting rect on drawable
     */
    public void setImageDrawable(Drawable drawable, RectF cropRect, RectF cropImageRect) {
        this.cropRectRel = cropRect;
        this.cropImageRect = cropImageRect;
        imageRotation = 0;
        setUpMatrix(drawable, getWidth(), getHeight());
        super.setImageDrawable(drawable);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setUpMatrix(getDrawable(), w, h);
    }

    private void setUpMatrix(Drawable drawable, int w, int h) {
        if (drawable == null || w == 0 || h == 0) {
            return;
        }
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        if (imageRotation % 180 != 0) {
            width += height;
            height = width - height;
            width = width - height;
        }
        float centerViewX, centerViewY;
        float centerImageX, centerImageY;
        if (cropRectRel == null) {
            mScaleFactor = Math.max(w / (float) width, h / (float) height);
            centerViewX = w / 2;
            centerViewY = h / 2;
            centerImageX = width / 2;
            centerImageY = height / 2;
            cropRect.set(0, 0, w, h);
        } else {
            if (cropImageRect == null) {
                cropImageRect = new RectF(0, 0, width, height);
            }
            float baseViewSize = Math.min(w, h);
            cropRect.set((w - cropRectRel.width() * baseViewSize) / 2,
                    (h - cropRectRel.height() * baseViewSize) / 2,
                    (w + cropRectRel.width() * baseViewSize) / 2,
                    (h + cropRectRel.height() * baseViewSize) / 2);
            mScaleFactor = Math.max(cropRect.width() / cropImageRect.width(), cropRect.height() / cropImageRect.height());
            centerViewX = cropRect.centerX();
            centerViewY = cropRect.centerY();
            centerImageX = cropImageRect.centerX();
            centerImageY = cropImageRect.centerY();
        }
        float radius = cropRect.width() / 2.5f;
        cropRectCircle.set(cropRect.centerX() - radius, cropRect.centerY() - radius, cropRect.centerX() + radius, cropRect.centerY() + radius);
        mMinScale = Math.max(cropRect.width() / (float) width, cropRect.height() / (float) height);
        if (mScaleFactor < mMinScale) {
            mScaleFactor = mMinScale;
        }
        matrix.reset();
        matrix.postTranslate(centerViewX - centerImageX, centerViewY - centerImageY);
        matrix.postScale(mScaleFactor, mScaleFactor, centerViewX, centerViewY);
        matrix.postRotate(imageRotation, centerViewX, centerViewY);
        setImageMatrix(matrix);
    }

    private static final Paint testPaint = new Paint();
    static {
        testPaint.setColor(Color.GREEN);
        testPaint.setStrokeWidth(3);
        testPaint.setStyle(Paint.Style.STROKE);
    }

    private RectF drawTempRect = new RectF();
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (BuildConfig.DEBUG) {
            if (cropImageRect != null) {
                matrix.mapRect(drawTempRect, cropImageRect);
                testPaint.setColor(Color.BLUE);
                canvas.drawRect(drawTempRect, testPaint);
            }
            testPaint.setColor(Color.GREEN);
            canvas.drawRect(cropRect, testPaint);
            testPaint.setColor(Color.RED);
            canvas.drawRect(0, 0, getWidth(), getHeight(), testPaint);
        }
        canvas.drawRoundRect(cropRect, cropRectRadius, cropRectRadius, cropPaint);
        canvas.drawCircle(cropRectCircle.centerX(), cropRectCircle.centerY(), cropRectCircle.width() / 2, cropPaint);
    }

    private void refreshImageRect() {
        int width = (getDrawable() != null) ? getDrawable().getIntrinsicWidth() : 0;
        int height = (getDrawable() != null) ? getDrawable().getIntrinsicHeight() : 0;
        imageRect.set(0, 0, width, height);
        matrix.mapRect(imageRect);
    }

    public RectF getImageRect() {
        refreshImageRect();
        return new RectF(imageRect);
    }

    public RectF getCropRect() {
        return new RectF(cropRect);
    }

    public RectF getCroppedImageRect() {
        refreshImageRect();
        float[] origin = new float[] {0, 0};
        matrix.mapPoints(origin);
        Matrix m = new Matrix();
        m.postRotate(-imageRotation, origin[0], origin[1]);
        m.postTranslate(-origin[0], -origin[1]);
        m.postScale(1 / mScaleFactor, 1 / mScaleFactor);

        RectF rotatedRect = new RectF();
        m.mapRect(rotatedRect, cropRect);
        float xOffset = 0, yOffset = 0;
        if (rotatedRect.left < 0) {
            xOffset = Math.abs(rotatedRect.left);
        }
        if (rotatedRect.top < 0) {
            yOffset = Math.abs(rotatedRect.top);
        }
        rotatedRect.offset(xOffset, yOffset);
        return rotatedRect;
    }

    public void rotateImage() {
        imageRotation = (imageRotation + 90) % 360;
        setUpMatrix(getDrawable(), getWidth(), getHeight());
    }

    public int getImageRotation() {
        return imageRotation;
    }

    private class ScaleListener extends
            ScaleGestureDetector.SimpleOnScaleGestureListener {

        private float width, height;
        private float[] focusPoint = new float[2];
        private float[] pivotPoint = new float[2];
        private float[] anchorPoint = new float[2];
        private float newScale;
        private float xShift, yShift;
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float currentScale = detector.getScaleFactor();
            newScale = mScaleFactor * currentScale;

            focusPoint[0] = detector.getFocusX();
            focusPoint[1] = detector.getFocusY();

            if (newScale >= mMinScale) {
                mScaleFactor = newScale;
            } else {
                currentScale = mMinScale / mScaleFactor;
                mScaleFactor = mMinScale;
            }
            matrix.postTranslate(anchorPoint[0] - pivotPoint[0], anchorPoint[1] - pivotPoint[1]);
            pivotPoint[0] = focusPoint[0];
            pivotPoint[1] = focusPoint[1];
            matrix.postScale(currentScale, currentScale, anchorPoint[0], anchorPoint[1]);
            matrix.postTranslate(focusPoint[0] - anchorPoint[0], focusPoint[1] - anchorPoint[1]);
            tempRect.set(0, 0, width, height);
            matrix.mapRect(tempRect);
            if (tempRect.left > cropRect.left) {
                xShift = cropRect.left - tempRect.left;
            } else if (tempRect.right < cropRect.right) {
                xShift = cropRect.right - tempRect.right;
            }
            if (tempRect.top > cropRect.top) {
                yShift = cropRect.top - tempRect.top;
            } else if (tempRect.bottom < cropRect.bottom) {
                yShift = cropRect.bottom - tempRect.bottom;
            }
            matrix.postTranslate(xShift, yShift);
            pivotPoint[0] += xShift;
            pivotPoint[1] += yShift;
            setImageMatrix(matrix);
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            anchorPoint[0] = detector.getFocusX();
            anchorPoint[1] = detector.getFocusY();
            pivotPoint[0] = anchorPoint[0];
            pivotPoint[1] = anchorPoint[1];
            width = getDrawable().getIntrinsicWidth();
            height = getDrawable().getIntrinsicHeight();
            return super.onScaleBegin(detector);
        }
    }

}
