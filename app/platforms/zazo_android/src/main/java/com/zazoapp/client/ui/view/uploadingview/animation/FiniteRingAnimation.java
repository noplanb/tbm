package com.zazoapp.client.ui.view.uploadingview.animation;

import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;
import com.zazoapp.client.ui.view.uploadingview.animation.viewanimationlistener.IViewFiniteAnimationListener;

/**
 * Created by sergii on 15.11.15.
 */
public class FiniteRingAnimation extends UploadAnimation {

    private static final long ANIMATION_DURATION = 1000;
    private static final float START_SWEEP_ANGLE = 0;
    private static final float FINAL_SWEEP_ANGLE = 360;

    private IViewFiniteAnimationListener viewFiniteListener;

    protected ValueAnimator createAnimation() {
        final ValueAnimator startAngleRotate = ValueAnimator.ofFloat(START_SWEEP_ANGLE, FINAL_SWEEP_ANGLE);
        startAngleRotate.setDuration(ANIMATION_DURATION);
        startAngleRotate.setInterpolator(new DecelerateInterpolator(2));
        startAngleRotate.addListener(this);
        startAngleRotate.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (getViewFiniteListener() != null) {
                    updateView((Float) animation.getAnimatedValue());
                }
            }
        });
        return startAngleRotate;
    }

    private IViewFiniteAnimationListener getViewFiniteListener(){
        return viewFiniteListener;
    }

    @Override
    public void resetView() {
        updateView(START_SWEEP_ANGLE );
    }

    private void updateView(float aCurrentAngle) {
        getViewFiniteListener().setActualAngleFiniteAnimation(aCurrentAngle);
        getViewFiniteListener().invalidate();
    }

    public void setView(IViewFiniteAnimationListener aViewFiniteListener) {
        super.setView(aViewFiniteListener);
        viewFiniteListener = aViewFiniteListener;
    }

}