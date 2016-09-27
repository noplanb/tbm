package com.zazoapp.client.ui.animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.util.Log;
import android.widget.TextView;

/**
 * Created by skamenkovych@codeminders.com on 2/16/2016.
 */
public class TextAnimations {
    public static Animator animateAlpha(final TextView view, final CharSequence newValue) {
        if (newValue.equals(view.getText())) {
            return null;
        }
        final ValueAnimator anim2 = ValueAnimator.ofFloat(0, 1);
        int duration = 300;
        anim2.setDuration(duration);
        final ValueAnimator anim = ValueAnimator.ofFloat(view.getAlpha(), 0);
        anim.setDuration(duration);
        anim.addListener(new AnimatorListenerAdapter() {
            boolean isCanceled = false;
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!isCanceled) {
                    view.setText(newValue);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                isCanceled = true;
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
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(anim, anim2);
        animatorSet.start();
        return animatorSet;
    }
}
