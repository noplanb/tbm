package com.zazoapp.client.ui.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
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

import java.util.ArrayList;
import java.util.List;

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
    private Scheme scheme = new VideoProgressBar.Scheme.SchemeBuilder().build();
    private int current;

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
        progressLineHeight = Convenience.dpToPx(getContext(), 3f);
    }

    public void setScheme(Scheme scheme) {
        if (scheme != null) {
            this.scheme = scheme;
        }
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
        this.current = current;
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
        progressAnimator.setDuration(Math.max(duration, 0));
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

    private RectF drawRoundRect = new RectF();
    @Override
    protected void dispatchDraw(Canvas canvas) {
        int cY = getHeight() / 2;
        canvas.save();
        canvas.translate(0, cY - progressLineHeight / 2f);
        int barsCount = Math.max(scheme.getBarCount(), 1);
        int barPadding = Convenience.dpToPx(getContext(), 2);
        int maxBarSize = getWidth() / barsCount - 2 * barPadding;
        float scaledProgress = barsCount * progress;
        float curProgress = scaledProgress - (int) scaledProgress;
        float scaledSecProgress = barsCount * secondaryProgress;
        float curSecProgress = scaledSecProgress - (int)  scaledSecProgress;
        int left = barPadding;
        int maxRight = getWidth() - barPadding;
        float radius = Convenience.dpToPx(getContext(), 1f);
        drawRoundRect.top = 0;
        drawRoundRect.bottom = progressLineHeight;
        for (int i = 0; i < barsCount; i++) {
            drawRoundRect.left = left;
            if (i < Math.floor(scaledSecProgress)) {
                drawRoundRect.right = left + maxBarSize;
                canvas.drawRoundRect(drawRoundRect, radius, radius, secondaryPaint);
            } else if (i == Math.floor(scaledSecProgress)) {
                drawRoundRect.right = left + maxBarSize * curSecProgress;
                canvas.drawRoundRect(drawRoundRect, radius, radius, secondaryPaint);
            }
            if (i < Math.floor(scaledProgress)) {
                drawRoundRect.right = left + maxBarSize;
                canvas.drawRoundRect(drawRoundRect, radius, radius, primaryPaint);
            } else if (i == Math.floor(scaledProgress)) {
                drawRoundRect.right = left + maxBarSize * curProgress;
                canvas.drawRoundRect(drawRoundRect, radius, radius, primaryPaint);
            }
            left += maxBarSize + barPadding * 2;
        }
        //canvas.drawRect(0, 0, getWidth() * secondaryProgress, progressLineHeight, secondaryPaint);
        //canvas.drawRect(0, 0, (getWidth() - sliderView.getWidth()) * progress + sliderView.getPivotX(), progressLineHeight, primaryPaint);
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

    public static class Scheme {
        private List<Element> graphicScheme;
        private int barCount;

        public enum Element {
            BAR('-'),
            POINT('.');

            private char representation;
            Element(char c) {
                representation = c;
            }

            public char getRepresentation() {
                return representation;
            }
        }

        private Scheme(SchemeBuilder b) {
            graphicScheme = b.graphicScheme;
            barCount = b.barCount;
        }

        public int getBarCount() {
            return barCount;
        }

        public int getCount() {
            return graphicScheme.size();
        }

        public Element getElementAt(int pos) {
            if (pos < 0 || pos >= graphicScheme.size()) {
                return null;
            }
            return graphicScheme.get(pos);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            if (graphicScheme.size() == 0) {
                return "<Empty>";
            }
            for (Element element : graphicScheme) {
                builder.append(element.getRepresentation());
            }
            return builder.toString();
        }

        public static class SchemeBuilder {
            private List<Element> graphicScheme = new ArrayList<>();
            private int barCount;
            public SchemeBuilder() {
            }

            public void addBar() {
                graphicScheme.add(Element.BAR);
                barCount++;
            }

            public void addPoint() {
                graphicScheme.add(Element.POINT);
            }

            public Scheme build() {
                return new Scheme(this);
            }
        }
    }

}
