package com.zazoapp.client.ui.animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

/**
 * Created by skamenkovych@codeminders.com on 1/4/2016.
 */
public class ViewedAnimation {
    public static void animate(final View parent, final ImageView imgViewed, final Runnable changeAction) {
        final ValueAnimator anim = ValueAnimator.ofInt(255, 0, 255);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.setDuration(2000);
        anim.setRepeatCount(2);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                AlphaColorFilter filter = AlphaColorFilter.forAlpha((int) anim.getAnimatedValue());
                imgViewed.setColorFilter(filter);
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (changeAction != null) {
                    parent.post(changeAction);
                }
            }
        });
        anim.start();
    }
}
