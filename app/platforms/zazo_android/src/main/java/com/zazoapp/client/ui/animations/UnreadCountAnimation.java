package com.zazoapp.client.ui.animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.zazoapp.client.R;

/**
 * Created by Serhii on 31.12.2015.
 */
public class UnreadCountAnimation {
    public static void animate(final View parent, final Runnable changeAction, final Runnable endAction) {
        final Holder h = new Holder(parent);
        ValueAnimator.AnimatorUpdateListener updateListener = new ValueAnimator.AnimatorUpdateListener() {
            float scaleFactor = 0.33f;
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                int currentColor = h.unreadCount.getCurrentTextColor();
                int r = Color.red(currentColor);
                int b = Color.blue(currentColor);
                int g = Color.green(currentColor);
                h.unreadCount.setTextColor(Color.argb((int) (0xFF * value), r, g, b));
                h.layout.setScaleX(1f + scaleFactor * (1f - value));
                h.layout.setScaleY(1f + scaleFactor * (1f - value));
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
                    parent.post(endAction);
                }
            }
        });
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

    static class Holder {
        @InjectView(R.id.unread_count_layout) View layout;
        @InjectView(R.id.tw_unread_count) TextView unreadCount;

        Holder(View v) {
            ButterKnife.inject(this, v);
        }
    }
}
