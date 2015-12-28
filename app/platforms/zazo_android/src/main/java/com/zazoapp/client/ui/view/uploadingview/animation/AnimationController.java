package com.zazoapp.client.ui.view.uploadingview.animation;

import android.widget.ImageView;
import com.zazoapp.client.ui.view.uploadingview.animation.animationlistener.UploadAnimationListener;
import com.zazoapp.client.ui.view.uploadingview.animation.viewanimationlistener.IViewFiniteAnimationListener;
import com.zazoapp.client.ui.view.uploadingview.animation.viewanimationlistener.IViewOpacityChangeAnimationListener;
import com.zazoapp.client.ui.view.uploadingview.animation.viewanimationlistener.IViewProgressAnimationListener;

/**
 * Created by sergii on 14.11.15.
 */
public class AnimationController implements IUploadingAnimationController, UploadAnimationListener {

    private static final String TAG = AnimationController.class.getSimpleName();

    private FiniteRingAnimation firstRingAnimation;
    private ProgressRingAnimation secondRingAnimation;

    private UploadAnimationListener externalUploadAnimationListener;
    private SpiralRotation spiralAnimation;

    private OpacityAnimation opacityAnimation;

    public AnimationController(){
        initAnimation();
    }

    private void initAnimation() {

        firstRingAnimation = new FiniteRingAnimation();

        secondRingAnimation = new ProgressRingAnimation();
        secondRingAnimation.setExternalListener(this);

        spiralAnimation = new SpiralRotation();
        spiralAnimation.setSpiralAnimationListener(this);

        opacityAnimation = new OpacityAnimation();
    }

    public void setFirstRingView(IViewFiniteAnimationListener aView) {
        firstRingAnimation.setView(aView);
    }

    public void setSecondRingView(IViewProgressAnimationListener aView) {
        secondRingAnimation.setView(aView);
    }

    public void setSpiralView(ImageView aView) {
        spiralAnimation.setView(aView);
    }

    public void setOpacityChangeColor(IViewOpacityChangeAnimationListener aView) {
        opacityAnimation.setView(aView);
    }

    @Override
    public void cancel() {
        firstRingAnimation.cancel();
        secondRingAnimation.cancel();
        spiralAnimation.cancel();
        opacityAnimation.cancel();
    }

    @Override
    public void start() {
        cancelAllRunningAnimation();

        firstRingAnimation.start();
        secondRingAnimation.start();
        spiralAnimation.start();
    }

    @Override
    public void reset() {
        firstRingAnimation.resetView();
        secondRingAnimation.resetView();
        spiralAnimation.resetView();
        opacityAnimation.resetView();
    }

    @Override
    public void updateProgress(float aValue) {
        secondRingAnimation.setProgress(aValue);
    }

    @Override
    public boolean isProgressFinish() {
        return secondRingAnimation.isAnimationFinish();
    }

    private void cancelAllRunningAnimation() {

        if ( !firstRingAnimation.isAnimationFinish() ){
            firstRingAnimation.cancel();
        }

        if ( !secondRingAnimation.isAnimationFinish() ){
            secondRingAnimation.cancel();
        }

        if ( opacityAnimation.isAnimationFinish() ){
            opacityAnimation.cancel();
        }
    }

    @Override
    public void onProgressAnimationFinish() {
        opacityAnimation.start();
        if ( getExternalUploadAnimationListener() != null ){
            getExternalUploadAnimationListener().onProgressAnimationFinish();
        }
    }

    @Override
    public void onSpiralAnimationFinish() {
        if ( getExternalUploadAnimationListener() != null ){
            getExternalUploadAnimationListener().onSpiralAnimationFinish();
        }
    }

    public UploadAnimationListener getExternalUploadAnimationListener() {
        return externalUploadAnimationListener;
    }

    public void setExternalUploadAnimationListener(UploadAnimationListener externalUploadAnimationListener) {
        this.externalUploadAnimationListener = externalUploadAnimationListener;
    }
}
