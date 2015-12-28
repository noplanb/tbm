package com.zazoapp.client.ui.view.downloadingview.animation;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import com.zazoapp.client.ui.view.downloadingview.animation.listener.IArrowShowAnimationListener;

/**
 * Created by sergii on 18.11.15.
 */
public class FlipInAnimator extends DownloadAnimation {

    private static final long ANIMATION_DURATION = 500;
    private static final String ROTATION_Y = "rotationY";
    private static final float START_VALUE = -90;
    private static final float END_VALUE = 0;

    private IArrowShowAnimationListener arrowShowAnimationListener;

    @Override
    protected ValueAnimator createAnimation() {
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(getViewAnimationListener() ,
                ROTATION_Y, START_VALUE, END_VALUE );
        objectAnimator.setObjectValues(objectAnimator);
        objectAnimator.setDuration(ANIMATION_DURATION);

        return objectAnimator;
    }

    @Override
    public void resetView() {
        getViewAnimationListener().setVisibility(View.INVISIBLE);
    }

    public void setExternalListener(IArrowShowAnimationListener arrowShowAnimationListener) {
        this.arrowShowAnimationListener = arrowShowAnimationListener;
    }


    @Override
    public void onAnimationEnd(Animator animation) {
        super.onAnimationEnd(animation);
        if( !isCancel() &&  getArrowShowAnimationListener() != null){
            getArrowShowAnimationListener().onArrowShowAnimationFinish();
        }
    }

    @Override
    public void start() {
        getViewAnimationListener().setVisibility(View.VISIBLE);
        super.start();
    }

    public IArrowShowAnimationListener getArrowShowAnimationListener() {
        return arrowShowAnimationListener;
    }

}
