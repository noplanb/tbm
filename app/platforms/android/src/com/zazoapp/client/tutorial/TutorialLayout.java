package com.zazoapp.client.tutorial;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

/**
 * Created by skamenkovych@codeminders.com on 5/6/2015.
 */
public class TutorialLayout extends FrameLayout {
    private static final String TAG = TutorialLayout.class.getSimpleName();
    private static final int MAX_DIM = 120;
    private static final int DURATION = 1000;
    private boolean dimmed;
    private int dimValue;
    private Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator dimAnimator;
    private RectF dimExcludedRect;

    public TutorialLayout(Context context) {
        super(context);
    }

    public TutorialLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TutorialLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TutorialLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void dim() {
        dimmed = true;
        setVisibility(VISIBLE);
        dimAnimator = ValueAnimator.ofInt(0, MAX_DIM);
        dimPaint.setARGB(0, 20, 20, 20);
        dimPaint.setStyle(Paint.Style.FILL);
        dimAnimator.setDuration(DURATION);
        dimAnimator.setInterpolator(new DecelerateInterpolator());
        dimAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                dimValue = (int) animation.getAnimatedValue();
                postInvalidate();
            }
        });
        dimAnimator.start();
    }

    public void dimExceptForRect(RectF rect) {
        dimExcludedRect = rect;
        dim();
    }

    public void dimExceptForRect(int left, int top, int right, int bottom) {
        dimExceptForRect(new RectF(left, top, right, bottom));
    }

    public void dimExceptForCircle(int xCenter, int yCenter, int radius) {

    }

    public void dismiss() {
        if (dimAnimator != null && dimmed) {
            dimAnimator.cancel();
            dimAnimator.setIntValues(dimValue, 0);
            dimAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    setVisibility(GONE);
                    dimExcludedRect = null;
                    dimmed = false;
                    dimValue = 0;
                    dimAnimator.removeAllUpdateListeners();
                    dimAnimator.removeAllListeners();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            dimAnimator.start();
        }
    }

    public void clear() {
        setVisibility(GONE);
        dimmed = false;
        dimValue = 0;
        dimExcludedRect = null;
        postInvalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        dimPaint.setAlpha(dimValue);
        if (dimExcludedRect != null) {
            Path path = new Path();
            path.addRect(dimExcludedRect, Path.Direction.CCW);
            canvas.clipPath(path, Region.Op.DIFFERENCE);
            canvas.drawRect(0, 0, getRight(), getBottom(), dimPaint);
            canvas.clipRect(0, 0, getRight(), getBottom(), Region.Op.UNION); // restore clip
        }
    }

}
