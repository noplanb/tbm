package com.zazoapp.client.ui.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.support.annotation.FloatRange;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.zazoapp.client.R;
import com.zazoapp.client.ui.animations.VideoProgressBarAnimation;
import com.zazoapp.client.utilities.Convenience;

/**
 * Created by skamenkovych@codeminders.com on 1/19/2016.
 */
public class VideoProgressBar extends FrameLayout {
    private static final String TAG = VideoProgressBar.class.getSimpleName();

    @InjectView(R.id.slider_view) AutoResizeTextView sliderView;
    private float progress = 0.1f;
    private float secondaryProgress = 1f;
    private float progressLineHeight;

    private Paint primaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Paint secondaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private ValueAnimator progressAnimator;
    private Animator appearingAnimation;

    public VideoProgressBar(Context context) {
        this(context, null);
    }

    public VideoProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.playback_slider, this);
        ButterKnife.inject(this);
        progressLineHeight = Convenience.dpToPx(getContext(), 2f);
    }

    public void setCurrent(int current, boolean animate) {
        final String currentText = (current > 0) ? String.valueOf(current) : "";
        if (animate) {
            VideoProgressBarAnimation.animateValueChange(sliderView, new Runnable() {
                @Override
                public void run() {
                    sliderView.setText(currentText);
                }
            }, null);
        } else {
            sliderView.setText(currentText);
        }
    }

    public void setProgress(@FloatRange(from = 0f, to = 1f) float progress) {
        this.progress = progress;
        setSliderPosition();
        invalidate();
    }

    public @FloatRange(from = 0f, to = 1f) float getProgress() {
        return progress;
    }

    public void animateProgress(@FloatRange(from = 0f, to = 1f) float startProgress, @FloatRange(from = 0f, to = 1f) float endProgress, int duration) {
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
        progressAnimator = ValueAnimator.ofFloat(startProgress, endProgress);
        progressAnimator.setInterpolator(new LinearInterpolator());
        progressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setProgress((float) animation.getAnimatedValue());
            }
        });
        progressAnimator.setDuration(duration);
        progressAnimator.start();
    }

    private void setSliderPosition() {
        sliderView.setX((getWidth() - sliderView.getWidth()) * progress);
    }

    public void setSecondaryProgress(@FloatRange(from = 0f, to = 1f) float progress) {
        this.secondaryProgress = progress;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        recalculatePaints(w, h);
        setSliderPosition();
    }

    private void recalculatePaints(int w, int h) {
        primaryPaint.setShader(new LinearGradient(0, 0, 0, progressLineHeight,
                new int[]{
                        Color.parseColor("#eeeeee"),
                        Color.WHITE,
                        Color.WHITE,
                        Color.parseColor("#eeeeee")
                }, new float[]{0f, 0.1f, 0.8f, 0.9f}, Shader.TileMode.CLAMP));
        secondaryPaint.setShader(new LinearGradient(0, 0, 0, progressLineHeight,
                new int[]{
                        Color.parseColor("#999999"),
                        Color.parseColor("#a5a5a5"),
                        Color.parseColor("#a5a5a5"),
                        Color.parseColor("#888888")
                }, new float[]{0f, 0.1f, 0.8f, 0.9f}, Shader.TileMode.CLAMP));
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int cY = getHeight() / 2;
        canvas.save();
        canvas.translate(0, cY - progressLineHeight / 2f);
        canvas.drawRect(0, 0, getWidth() * secondaryProgress, progressLineHeight, secondaryPaint);
        canvas.drawRect(0, 0, (getWidth() - sliderView.getWidth()) * progress + sliderView.getPivotX(), progressLineHeight, primaryPaint);
        canvas.restore();
        super.dispatchDraw(canvas);
    }

    public void doAppearing() {
        if (appearingAnimation != null) {
            appearingAnimation.cancel();
        }
        setProgress(0);
        appearingAnimation = VideoProgressBarAnimation.getTerminalAnimation(this, true);
        appearingAnimation.start();
    }

    public void doDisappearing() {
        if (appearingAnimation != null) {
            appearingAnimation.cancel();
        }
        appearingAnimation = VideoProgressBarAnimation.getTerminalAnimation(this, false);
        appearingAnimation.start();
        setCurrent(0, false);
    }

    public void pause() {
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
    }
}
