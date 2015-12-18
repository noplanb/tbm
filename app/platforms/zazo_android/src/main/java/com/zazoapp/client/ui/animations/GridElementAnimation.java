package com.zazoapp.client.ui.animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.zazoapp.client.R;
import com.zazoapp.client.ui.view.GridElementView;
import com.zazoapp.client.ui.view.ThumbView;
import com.zazoapp.client.ui.view.rotationcircleview.animation.IRotationCircleViewController;
import com.zazoapp.client.ui.view.rotationcircleview.view.RotationCircleView;

/**
 * Helper class for grid elements animation
 * Created by skamenkovych@codeminders.com on 12/18/2015.
 */
public class GridElementAnimation {
    public static Animator appearing(Context context, GridElementView view, boolean enter) {
        ValueAnimator anim;
        if (enter) {
            anim = ValueAnimator.ofFloat(0, 1);
        } else {
            anim = ValueAnimator.ofFloat(1, 0);
        }
        Resources res = context.getResources();
        final Holder h = new Holder(view);
        anim.setInterpolator(new AccelerateInterpolator());
        anim.setDuration(res.getInteger(android.R.integer.config_longAnimTime) + 100);
        anim.setStartDelay(100);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                h.body.setAlpha(value * 0.5f + 0.5f);
                h.thumbView.setDrawableRadius(h.thumbView.getDefaultDrawableRadius() * value);
                h.thumbView.setVisibility(View.VISIBLE);
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                h.holdToRec.getAnimationController().start();
            }
        });
        return anim;
    }

    public static IRotationCircleViewController holdToRec(GridElementView view) {
        final Holder h = new Holder(view);
        return h.holdToRec.getAnimationController();
    }

    static class Holder {
        @InjectView(R.id.body) View body;
        @InjectView(R.id.empty_view) View emptyView;
        @InjectView(R.id.img_thumb) ThumbView thumbView;
        @InjectView(R.id.hold_to_record) RotationCircleView holdToRec;

        Holder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}
