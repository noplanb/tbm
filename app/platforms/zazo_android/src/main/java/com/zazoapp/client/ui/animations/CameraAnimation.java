package com.zazoapp.client.ui.animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewPropertyAnimator;

/**
 * Created by skamenkovych@codeminders.com on 1/6/2016.
 */
public class CameraAnimation {
    private static final String TAG = CameraAnimation.class.getSimpleName();

    public static void animateOut(final View target, final Runnable endAction) {
        animate(target, endAction, target.animate().alpha(0));
    }

    public static void animateIn(final View target, final Runnable endAction) {
        animate(target, endAction, target.animate().alpha(1));
    }

    private static void animate(final View target, @Nullable final Runnable endAction, ViewPropertyAnimator anim) {
        anim.setDuration(400);
        anim.setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                target.post(new Runnable() {
                    @Override
                    public void run() {
                        if (endAction != null) {
                            endAction.run();
                        }
                    }
                });
            }
        });
        anim.start();
    }

}
