package com.zazoapp.client.ui.animations;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.AnimRes;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import com.zazoapp.client.R;

/**
 * Created by skamenkovych@codeminders.com on 11/6/2015.
 */
public class SlideHorizontalFadeAnimation {
    public static Animation get(Context context, @AnimRes int type) {
        float start = 0f, end = 0f;
        float fromAlpha = 0f, toAlpha = 0f;
        Resources res = context.getResources();
        switch (type) {
            case R.anim.slide_left_fade_in:
                start = res.getDimension(R.dimen.anim_sliding_fragment_distance);
                toAlpha = 1.0f;
                break;
            default:
                end = res.getDimension(R.dimen.anim_sliding_fragment_distance);
                fromAlpha = 1.0f;
                break;
        }
        AnimationSet set = new AnimationSet(true);
        set.addAnimation(new TranslateAnimation(start, end, 0f, 0f));
        set.addAnimation(new AlphaAnimation(fromAlpha, toAlpha));
        set.setDuration(res.getInteger(R.integer.anim_duration));
        set.setInterpolator(context, android.R.interpolator.accelerate_quad);
        return set;
    }
}
