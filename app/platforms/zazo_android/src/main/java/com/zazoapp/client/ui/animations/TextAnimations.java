package com.zazoapp.client.ui.animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.widget.TextView;

/**
 * Created by skamenkovych@codeminders.com on 2/16/2016.
 */
public class TextAnimations {
    public static void animateAlpha(final TextView view, final CharSequence newValue) {
        final ValueAnimator anim2 = ValueAnimator.ofFloat(0, 1);
        int duration = 300;
        anim2.setDuration(duration);
        final ValueAnimator anim = ValueAnimator.ofFloat(1, 0);
        anim.setDuration(duration);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                view.setText(newValue);
                anim2.start();
            }
        });
        ValueAnimator.AnimatorUpdateListener updateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                view.setAlpha((Float) animation.getAnimatedValue());
            }
        };
        anim.addUpdateListener(updateListener);
        anim2.addUpdateListener(updateListener);
        anim.start();
    }
}
