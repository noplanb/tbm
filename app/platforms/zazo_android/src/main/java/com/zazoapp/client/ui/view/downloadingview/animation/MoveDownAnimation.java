package com.zazoapp.client.ui.view.downloadingview.animation;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.view.animation.AccelerateInterpolator;
import com.zazoapp.client.ui.view.downloadingview.animation.listener.IArrowHideAnimationListener;

/**
 * Created by sergii on 15.11.15.
 */
public class MoveDownAnimation extends DownloadAnimation {

    private static final long ANIMATION_DURATION = 500;
    private static final int START_SHIFT = 0;

    private ValueAnimator startAngleRotate;
    private IViewMoveDownAnimationListener viewMoveDownAnimationListener;
    private IArrowHideAnimationListener arrowHideAnimationListener;

    protected ValueAnimator createAnimation() {
        startAngleRotate = ValueAnimator.ofFloat(START_SHIFT, getViewFiniteListener().getFinalValue());
        startAngleRotate.setDuration(ANIMATION_DURATION);
        startAngleRotate.setInterpolator(new AccelerateInterpolator(2));
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

    @Override
    public void resetView() {
        updateView(START_SHIFT);
    }

    private IViewMoveDownAnimationListener getViewFiniteListener(){
        return viewMoveDownAnimationListener;
    }

    private void updateView(float aShift) {
        getViewFiniteListener().setShift((int) aShift);
        getViewFiniteListener().invalidate();
    }

    public void setView(IViewMoveDownAnimationListener aViewFiniteListener) {
        super.setView(aViewFiniteListener);
        this.viewMoveDownAnimationListener = aViewFiniteListener;
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        super.onAnimationEnd(animation);
        getViewFiniteListener().setShift(START_SHIFT);
        if( !isCancel() &&  getArrowHideAnimationListener() != null){
            getArrowHideAnimationListener().onArrowHideAnimationFinish();
        }
    }

    public void setExternalListener(IArrowHideAnimationListener arrowHideAnimationListener) {
        this.arrowHideAnimationListener = arrowHideAnimationListener;
    }

    public IArrowHideAnimationListener getArrowHideAnimationListener() {
        return arrowHideAnimationListener;
    }

    public long getAnimationDuration() {
        return ANIMATION_DURATION;
    }
}
