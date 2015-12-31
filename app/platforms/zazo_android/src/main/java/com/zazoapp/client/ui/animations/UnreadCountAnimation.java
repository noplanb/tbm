package com.zazoapp.client.ui.animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.TextView;

/**
 * Created by Serhii on 31.12.2015.
 */
public class UnreadCountAnimation {
    public static void animate(final View parent, final TextView unreadCount, final Runnable changeAction) {
        ValueAnimator.AnimatorUpdateListener updateListener = new ValueAnimator.AnimatorUpdateListener() {
            float scaleFactor = 0.33f;
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                int currentColor = unreadCount.getCurrentTextColor();
                int r = Color.red(currentColor);
                int b = Color.blue(currentColor);
                int g = Color.green(currentColor);
                unreadCount.setTextColor(Color.argb((int) (0xFF * value), r, g, b));
                unreadCount.setScaleX(1f + scaleFactor * (1f - value));
                unreadCount.setScaleY(1f + scaleFactor * (1f - value));
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
        anim1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                parent.post(new Runnable() {
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
}
