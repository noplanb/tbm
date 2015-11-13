package com.zazoapp.client.ui.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.zazoapp.client.R;
import com.zazoapp.client.utilities.Convenience;

/**
 * Created by skamenkovych@codeminders.com on 10/8/2015.
 */
public class TouchBlockScreen extends RelativeLayout {
    private static final int MAX_DIM = (int) (0.8f * 255);
    private static final int DURATION = 300;

    private UnlockListener unlockListener;
    private LockState state = LockState.UNLOCKED;
    private long lastTouchTime;

    private boolean dimmed;
    private int dimValue;
    private Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator dimAnimator;

    private enum LockState {
        LOCKED,
        TOUCHED,
        CLICKED,
        UNLOCKED
    }

    public TouchBlockScreen(Context context) {
        super(context);
    }

    public TouchBlockScreen(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TouchBlockScreen(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TouchBlockScreen(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public interface UnlockListener {
        void onUnlockGesture();
    }

    public void setUnlockListener(UnlockListener listener) {
        unlockListener = listener;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        handleTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    private void handleTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (state) {
            case LOCKED:
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        state = LockState.TOUCHED;
                        lastTouchTime = System.nanoTime();
                        break;
                    default:
                        clearLockState();
                }
                break;
            case TOUCHED:
                switch (action) {
                    case MotionEvent.ACTION_UP:
                        if (System.nanoTime() - lastTouchTime > 500000000) {
                            clearLockState();
                        } else {
                            state = LockState.CLICKED;
                            lastTouchTime = System.nanoTime();
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        break;
                    default:
                        clearLockState();
                }
                break;
            case CLICKED:
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        if (System.nanoTime() - lastTouchTime > 500000000) {
                            clearLockState();
                        } else {
                            state = LockState.UNLOCKED;
                            if (unlockListener != null) {
                                unlockListener.onUnlockGesture();
                            }
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        break;
                    default:
                        clearLockState();
                }
        }
    }

    private void clearLockState() {
        state = LockState.LOCKED;
        lastTouchTime = 0;
    }

    public void lock() {
        if (!isLocked()) {
            state = LockState.LOCKED;
            dimmed = true;
            TextView hint = (TextView) findViewById(R.id.block_screen_hint);
            hint.setTypeface(Convenience.getTypeface(getContext()));
            setVisibility(VISIBLE);
            if (dimAnimator != null && dimmed) {
                dimAnimator.cancel();
            }
            dimAnimator = ValueAnimator.ofInt(0, MAX_DIM);
            dimPaint.setColor(getContext().getResources().getColor(R.color.light_grey_new));
            dimPaint.setStyle(Paint.Style.FILL);
            dimAnimator.setDuration(DURATION);
            dimAnimator.setInterpolator(new DecelerateInterpolator());
            dimAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    dimValue = (int) animation.getAnimatedValue();
                    setAlpha(dimValue / (float) MAX_DIM);
                    invalidate();
                }
            });
            dimAnimator.start();
        }
    }

    public void unlock(boolean force) {
        if (state == LockState.UNLOCKED || force) {
            if (dimAnimator != null && dimmed) {
                dimAnimator.cancel();
                dimAnimator.setIntValues(dimValue, 0);
                dimAnimator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        setVisibility(INVISIBLE);
                        dimmed = false;
                        dimValue = 0;
                        state = LockState.UNLOCKED;
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
    }

    public boolean isLocked() {
        return state != LockState.UNLOCKED || dimmed;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        dimPaint.setAlpha(dimValue);
        if (dimmed) {
            canvas.drawRect(0, 0, getRight(), getBottom(), dimPaint);
        }
        super.dispatchDraw(canvas);
    }
}
