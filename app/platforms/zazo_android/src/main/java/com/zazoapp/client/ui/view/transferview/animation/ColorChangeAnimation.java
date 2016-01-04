package com.zazoapp.client.ui.view.transferview.animation;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import com.zazoapp.client.ui.view.transferview.animation.listeners.IColorChangeAnimationListener;
import com.zazoapp.client.ui.view.transferview.animation.listeners.IViewColorChangeAnimationListener;

/**
 * Created by sergii on 18.11.15.
 */
public class ColorChangeAnimation extends BaseAnimation {

    private static final long ANIMATION_DURATION = 350;
    private static final String ALPHA = "alpha";
    private static final String COLOR = "color";

    private int initialColor = 0xFFFFFFFF;
    private IViewColorChangeAnimationListener viewColorChangeAnimationListener;
    private IColorChangeAnimationListener colorChangeAnimationListener;

    @Override
    protected ValueAnimator createAnimation() {
        initialColor = viewColorChangeAnimationListener.getStartColor();
        ValueAnimator anim = new ValueAnimator();
        anim.setValues(PropertyValuesHolder.ofInt(COLOR, initialColor, viewColorChangeAnimationListener.getFinalColor()),
                PropertyValuesHolder.ofInt(ALPHA, 255, 0, 0, 0));
        anim.setEvaluator(new ArgbEvaluator());
        anim.addListener(this);
        anim.setDuration(ANIMATION_DURATION);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                updateView((Integer) valueAnimator.getAnimatedValue(COLOR), (Integer) valueAnimator.getAnimatedValue(ALPHA));
            }
        });

        return anim;
    }

    private void updateView(int color, int alpha) {
        viewColorChangeAnimationListener.setBackgroundColor(color);
        viewColorChangeAnimationListener.setRingOpacity(alpha);
        viewColorChangeAnimationListener.invalidate();
    }

    public void setView(IViewColorChangeAnimationListener aView) {

        super.setView(aView);
        viewColorChangeAnimationListener = aView;
        initialColor = -1;
    }

    public IViewColorChangeAnimationListener getView(){
        return viewColorChangeAnimationListener;
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        super.onAnimationEnd(animation);
        if( !isCancel() &&  colorChangeAnimationListener != null){
            colorChangeAnimationListener.onColorChangeAnimationFinish();
        }
    }

    @Override
    public void resetView() {
        if ( isInitColorValid() ){
            viewColorChangeAnimationListener.setBackgroundColor(initialColor);
            viewColorChangeAnimationListener.setRingOpacity(255);
        }
    }

    private boolean isInitColorValid() {
        return initialColor != Integer.MIN_VALUE;
    }

    public void setExternalListener(IColorChangeAnimationListener colorChangeAnimationListener) {
        this.colorChangeAnimationListener = colorChangeAnimationListener;
    }

}
