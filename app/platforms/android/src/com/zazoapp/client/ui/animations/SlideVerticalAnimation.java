package com.zazoapp.client.ui.animations;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.AnimRes;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import com.zazoapp.client.R;

/**
 * Created by skamenkovych@codeminders.com on 11/6/2015.
 */
public class SlideVerticalAnimation {

    public static Animation get(Context context, @AnimRes int type) {
        float start, end;
        Resources res = context.getResources();
        switch (type) {
            case R.anim.slide_up:
                start = res.getDimension(R.dimen.feature_unlock_another_height);
                end = 0;
                break;
            default:
                end = res.getDimension(R.dimen.feature_unlock_another_height);
                start = 0;
                break;
        }
        Animation slide = new TranslateAnimation(0f, 0f, start, end);
        slide.setDuration(res.getInteger(android.R.integer.config_mediumAnimTime));
        slide.setInterpolator(context, android.R.interpolator.accelerate_quad);
        return slide;
    }
}
