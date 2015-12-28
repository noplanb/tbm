package com.zazoapp.client.ui.view.downloadingview.animation;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;
import com.zazoapp.client.ui.view.downloadingview.animation.listener.IProgressAnimationListener;

/**
 * Created by sergii on 15.11.15.
 */
public class ProgressRingAnimation extends DownloadAnimation {

    private static final long ANIMATION_DURATION = 1300;
    private static final float FINAL_ACTUAL_ANGLE = 360;
    private static final float FINAL_PROGRESS = 100;
    private static final float PERCENTAGE_TO_ANGLE_COEF = FINAL_ACTUAL_ANGLE / FINAL_PROGRESS;

    private IViewProgressAnimationListener viewProgressListener;
    private float currentAngle;
    private float progress = 0;
    private IProgressAnimationListener progressAnimationListener;

    protected ValueAnimator createAnimation() {
        currentAngle = getViewProgressListener().getProgressCurrentAngle();
        ValueAnimator startAngleRotate = ValueAnimator.ofFloat(currentAngle, currentAngle + FINAL_ACTUAL_ANGLE);
        startAngleRotate.setDuration(ANIMATION_DURATION);
        startAngleRotate.setRepeatCount(ValueAnimator.INFINITE);
        startAngleRotate.setInterpolator(new LinearInterpolator());
        startAngleRotate.addListener(this);
        startAngleRotate.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (getViewProgressListener() != null) {
                    updateView((Float) animation.getAnimatedValue());
                }
            }
        });
        return startAngleRotate;
    }

    @Override
    public void resetView() {
        getViewProgressListener().setSweepAngleProgressValue(0);
        updateView(currentAngle);
    }

    private IViewProgressAnimationListener getViewProgressListener(){
        return viewProgressListener;
    }

    private void updateView(float aCurrentAngle) {
        getViewProgressListener().setActualAngleProgressAnimation(aCurrentAngle);
        getViewProgressListener().invalidate();
    }

    public void setView(IViewProgressAnimationListener aViewFiniteListener) {
        super.setView(aViewFiniteListener);
        this.viewProgressListener = aViewFiniteListener;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float aProgress) {

        if ( getViewProgressListener() != null) {
            this.progress = Math.min( aProgress, FINAL_PROGRESS );
            float sweepAngle = aProgress * PERCENTAGE_TO_ANGLE_COEF;

            if ( isDownloadFinish(sweepAngle ) ){
                getAnimation().cancel();
            }
            getViewProgressListener().setSweepAngleProgressValue( sweepAngle );
            getViewProgressListener().invalidate();
        }
    }

    private boolean isDownloadFinish(float sweepAngle) {
        return sweepAngle - FINAL_ACTUAL_ANGLE >= 0.f;
    }


    @Override
    public void onAnimationEnd(Animator animation) {
        super.onAnimationEnd(animation);
        if( !isCancel() &&  getProgressAnimationListener() != null){
            getProgressAnimationListener().onProgressAnimationFinish();
        }
    }

    public void setExternalListener(IProgressAnimationListener progressAnimationListener) {
        this.progressAnimationListener = progressAnimationListener;
    }

    public IProgressAnimationListener getProgressAnimationListener() {
        return progressAnimationListener;
    }
}
