package com.zazoapp.client.ui.view.uploadingview.animation;

import android.animation.Animator;
import android.animation.ValueAnimator;
import com.zazoapp.client.ui.view.uploadingview.animation.animationlistener.IColorChangeAnimationListener;
import com.zazoapp.client.ui.view.uploadingview.animation.viewanimationlistener.IViewOpacityChangeAnimationListener;

/**
 * Created by sergii on 23.11.15.
 */
public class OpacityAnimation extends UploadAnimation {

    private static final long ANIMATION_DURATION = 500;

    private int initialOpacity = 255;
    private int finalOpacity = 0;
    private IViewOpacityChangeAnimationListener viewOpacityChangeAnimationListener;
    private IColorChangeAnimationListener opacityAnimationListener;

    @Override
    protected ValueAnimator createAnimation() {

        ValueAnimator anim = new ValueAnimator();
        anim.setIntValues(initialOpacity, finalOpacity);
        anim.addListener(this);
        anim.setDuration(ANIMATION_DURATION);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                updateView((Integer) valueAnimator.getAnimatedValue());
            }
        });

        return anim;
    }

    private void updateView(Integer animatedValue) {
        viewOpacityChangeAnimationListener.setOpacity(animatedValue);
        viewOpacityChangeAnimationListener.invalidate();
    }

    public void setView(IViewOpacityChangeAnimationListener aView) {

        super.setView(aView);
        viewOpacityChangeAnimationListener = aView;
    }

    public IViewOpacityChangeAnimationListener getView(){
        return viewOpacityChangeAnimationListener;
    }


    @Override
    public void onAnimationEnd(Animator animation) {
        super.onAnimationEnd(animation);
        if( !isCancel() &&  opacityAnimationListener != null){
            opacityAnimationListener.onOpacityChangeAnimationFinish();
        }
    }

    @Override
    public void resetView() {
        viewOpacityChangeAnimationListener.setOpacity(initialOpacity);
    }

    public void setExternalListener(IColorChangeAnimationListener colorChangeAnimationListener) {
        this.opacityAnimationListener = colorChangeAnimationListener;
    }

}