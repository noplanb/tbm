package com.zazoapp.client.ui.view.transferview.animation;

import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import com.zazoapp.client.ui.view.transferview.animation.listeners.ISpiralAnimationListener;

/**
 * Created by sergii on 21.11.15.
 */
public class SpiralRotation implements Animation.AnimationListener {

    private static final long ANIMATION_DURATION = 800;

    private AnimationSet animSet;
    private ISpiralAnimationListener spiralAnimationListener;
    private ImageView view;
    private boolean isCancel;

    public SpiralRotation(  ){
        initAnimationSet();
    }

    private void initAnimationSet() {

        animSet = createAnimationSet();
        animSet.addAnimation(createPositionAnimation());
        animSet.addAnimation(createScaleAnimation());
        animSet.addAnimation(createRotationAnimation());
        animSet.addAnimation(createAlphaAnimation());
    }

    private AnimationSet createAnimationSet() {
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.setFillAfter(true);
        animationSet.setAnimationListener(this);
        return animationSet;
    }

    private Animation createAlphaAnimation() {
        final AlphaAnimation alphaAnimation = new AlphaAnimation(0.f, 1.0f);
        alphaAnimation.setDuration(ANIMATION_DURATION);
        return alphaAnimation;
    }

    private Animation createRotationAnimation() {

        final RotateAnimation rotateAnimation = new RotateAnimation(-270, 0f,
                Animation.RELATIVE_TO_SELF, .65f,
                Animation.RELATIVE_TO_SELF, .65f);
        rotateAnimation.setDuration(ANIMATION_DURATION);
        rotateAnimation.setInterpolator(new DecelerateInterpolator(2));
        return rotateAnimation;
    }

    private Animation createScaleAnimation() {
        final ScaleAnimation scale = new ScaleAnimation(0.2f, 1f, 0.2f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        float durationCoef = .6f;
        float delayCoef = 1 - durationCoef;
        scale.setDuration((long) (ANIMATION_DURATION * durationCoef));
        scale.setStartOffset((long) (ANIMATION_DURATION * delayCoef));
        scale.setInterpolator(new AccelerateInterpolator(8));
        return scale;
    }

    private Animation createPositionAnimation() {
        final TranslateAnimation translateAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, -0.9f, Animation.RELATIVE_TO_PARENT, .0f,
                Animation.RELATIVE_TO_PARENT, -0.5f, Animation.RELATIVE_TO_PARENT, .0f
        );
        translateAnimation.setInterpolator(new AccelerateInterpolator());
        translateAnimation.setDuration(ANIMATION_DURATION);
        return translateAnimation;
    }

    public void setView(ImageView aView) {
        this.view = aView;
    }

    public void cancel() {
        setIsCancel(true);
        if ( view != null ){
            view.setVisibility(View.INVISIBLE);
            view.clearAnimation();
        }

    }

    public void start() {
        if ( view == null ){
            return;
        }
        setIsCancel( false );
        view.setVisibility(View.VISIBLE);
        view.startAnimation(animSet);
    }

    private void setIsCancel(boolean aIsCancel) {
        isCancel = aIsCancel;
    }

    public void resetView() {
        cancel();
    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {
        if( !isCancel() && getSpiralAnimationListener() != null ){
            getSpiralAnimationListener().onSpiralAnimationFinish();
        }
    }

    private boolean isCancel() {
        return isCancel;
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }

    public ISpiralAnimationListener getSpiralAnimationListener() {
        return spiralAnimationListener;
    }

    public void setSpiralAnimationListener(ISpiralAnimationListener spiralAnimationListener) {
        this.spiralAnimationListener = spiralAnimationListener;
    }
}
