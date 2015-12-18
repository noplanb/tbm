package com.zazoapp.client.ui.animations;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.zazoapp.client.R;
import com.zazoapp.client.ui.view.GridElementView;
import com.zazoapp.client.ui.view.ThumbView;

/**
 * Provides animation for appearing friend on grid
 * Created by skamenkovych@codeminders.com on 12/18/2015.
 */
public class FriendAppearingAnimation {
    public static Animator get(Context context, GridElementView view, boolean enter) {
        ValueAnimator anim;
        if (enter) {
            anim = ValueAnimator.ofFloat(0, 1);
        } else {
            anim = ValueAnimator.ofFloat(1, 0);
        }
        Resources res = context.getResources();
        final Holder h = new Holder(view);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.setDuration(res.getInteger(android.R.integer.config_longAnimTime) + 100);
        anim.setStartDelay(100);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                h.body.setAlpha(value);
                h.thumbView.setDrawableRadius(h.thumbView.getDefaultDrawableRadius() * value);
                h.thumbView.setVisibility(View.VISIBLE);
            }
        });
        return anim;
    }

    static class Holder {
        @InjectView(R.id.body) View body;
        @InjectView(R.id.empty_view) View emptyView;
        @InjectView(R.id.img_thumb) ThumbView thumbView;

        Holder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}
