package com.zazoapp.client.ui.animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

    public static void animateTerminal(VideoProgressBar view, boolean start) {
        final Holder h = new Holder(view);
        float valueOffset = (start) ? 0f : 1f;
        ValueAnimator animator = ValueAnimator.ofFloat(0 + valueOffset, 1f - valueOffset);
        animator.setDuration(400);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                h.progressBar.setAlpha(value);
                h.textView.setScaleX(value);
                h.textView.setScaleY(value);
                h.progressBar.setSecondaryProgress(value);
            }
        });
        animator.start();
    }

    static class Holder {
        @InjectView(R.id.slider_view) TextView textView;
        VideoProgressBar progressBar;

        Holder(VideoProgressBar progressBar) {
            this.progressBar = progressBar;
            ButterKnife.inject(this, progressBar);
        }
    }
}
