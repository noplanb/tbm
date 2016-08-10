package com.zazoapp.client.ui.animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.zazoapp.client.R;
import com.zazoapp.client.ui.view.VideoProgressBar;

/**
 * Created by Serhii on 31.12.2015.
 */
public class VideoProgressBarAnimation {
    public static void animateValueChange(final TextView textView, @NonNull final Runnable changeAction, @Nullable final Runnable endAction) {
        ValueAnimator.AnimatorUpdateListener updateListener = new ValueAnimator.AnimatorUpdateListener() {
            float scaleFactor = 0.33f;
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                int currentColor = textView.getCurrentTextColor();
                int r = Color.red(currentColor);
                int b = Color.blue(currentColor);
                int g = Color.green(currentColor);
                textView.setTextColor(Color.argb((int) (0xFF * value), r, g, b));
            }
        };
        ValueAnimator anim1 = ValueAnimator.ofFloat(1f, 0);
        final ValueAnimator anim2 = ValueAnimator.ofFloat(0, 1f);
        anim1.setInterpolator(new AccelerateInterpolator());
        anim1.setDuration(300);
        anim1.addUpdateListener(updateListener);
        anim2.setInterpolator(new AccelerateInterpolator());
        anim2.setDuration(anim1.getDuration());
        anim2.addUpdateListener(updateListener);
        anim2.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (endAction != null) {
                    textView.post(endAction);
                }
            }
        });
        anim1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                textView.post(new Runnable() {
                    @Override
                    public void run() {
                        changeAction.run();
                        anim2.start();
                    }
                });
            }
        });
        anim1.start();
    }

    public static void animateIconRect(@NonNull final VideoProgressBar bar, final RectF rect, final int iconSize, @Nullable final Runnable endAction) {
        ValueAnimator.AnimatorUpdateListener updateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                float offset = iconSize * value / 2;
                float centerX = rect.centerX();
                float centerY = rect.centerY();
                rect.left = centerX - offset;
                rect.right = centerX + offset;
                rect.top = centerY - offset;
                rect.bottom = centerY + offset;
                bar.invalidate();
            }
        };
        ValueAnimator anim1 = ValueAnimator.ofFloat((iconSize > 0) ? rect.width() / iconSize : 1f, 1.33f, 1f);
        anim1.setInterpolator(new AccelerateInterpolator());
        anim1.setDuration(300);
        anim1.addUpdateListener(updateListener);
        anim1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                bar.post(new Runnable() {
                    @Override
                    public void run() {
                        if (endAction != null) {
                            endAction.run();
                        }
                    }
                });
            }
        });
        anim1.start();
    }

    public static Animator getTerminalAnimation(View view, boolean start) {
        final Holder h = new Holder(view);
        float valueOffset = (start) ? 0f : 1f;
        ValueAnimator animator = ValueAnimator.ofFloat(view.getAlpha(), 1f - valueOffset);
        animator.setDuration((start) ? 600 : 400);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                h.contextBar.setAlpha(Math.min(value * 1.5f, 1f));
                h.progressBar.setSecondaryProgress(value);
            }
        });
        return animator;
    }

    static class Holder {
        @InjectView(R.id.slider_view) TextView textView;
        @InjectView(R.id.progress_bar) VideoProgressBar progressBar;
        View contextBar;

        Holder(View videoContextBar) {
            contextBar = videoContextBar;
            ButterKnife.inject(this, videoContextBar);
        }
    }
}
