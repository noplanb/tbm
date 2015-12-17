package com.zazoapp.client.ui.view.rotationcircleview.animation;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.View;

import com.zazoapp.client.R;

import java.util.ArrayList;

/**
 * Created by sergii on 13.11.15.
 */
public class FlipFlopAnimation {

    interface IControllerView {
        View getHideView();
        View getShowView();
    }

    private Animator.AnimatorListener externalAnimationListener;

    private AnimatorSet flipIn;
    private AnimatorSet flipOut;
    private ObjectAnimator flipOutPosition;
    private ObjectAnimator flipOutAlpha;
    private ObjectAnimator flipInAlpha;
    private ObjectAnimator flipInPosition;

    private IControllerView controller;

    public FlipFlopAnimation(Context aContext) {
        initAnimation(aContext);
    }

    private void initAnimation(Context aContext) {
        initFlipOutAnimation( aContext );
        initFlipInAnimation(aContext);
    }

    private void initFlipInAnimation(Context aContext) {
        flipIn = (AnimatorSet) AnimatorInflater.loadAnimator(aContext,
                R.animator.rcv_flip_left_in);
        ArrayList<Animator> array = flipIn.getChildAnimations();
        for( Animator animator : array ){
            ObjectAnimator object = (ObjectAnimator) animator;
            if( object.getPropertyName().equals("alpha") ){
                flipInAlpha = object;
            } else {
                flipInPosition = object;
            }
        }
        array.clear();
    }

    private void initFlipOutAnimation(Context aContext) {
        flipOut = (AnimatorSet) AnimatorInflater.loadAnimator(aContext,
                R.animator.rcv_flip_left_out);
        ArrayList<Animator> array = flipOut.getChildAnimations();
        for( Animator animator : array ){
            ObjectAnimator object = (ObjectAnimator) animator;
            if( object.getPropertyName().equals("alpha") ){
                flipOutAlpha = object;
            } else {
                flipOutPosition = object;
            }
        }
        array.clear();
    }

    public void setExternalListener(Animator.AnimatorListener externalAnimationListener) {
        this.externalAnimationListener = externalAnimationListener;
    }

    public Animator.AnimatorListener getExternalListener() {
        return externalAnimationListener;
    }

    public long getDuration() {
        return flipOut.getDuration();
    }

    public void setDuration(long animDuration) {
        final long animDurationHalf = animDuration/2;

        flipInPosition.setDuration(animDuration);
        flipInAlpha.setStartDelay(animDurationHalf);

        flipOutPosition.setDuration(animDuration);
        flipOutAlpha.setStartDelay(animDurationHalf);

    }

    public void start(){
        if ( getController() == null
                || getController().getHideView() == null
                || getController().getShowView() == null ){
            return;
        }

        flipOut.removeAllListeners();
        flipOut.addListener(getExternalListener());
        prepareView();

        flipOut.start();
        flipIn.start();
    }

    private void prepareView() {
        getController().getShowView().setVisibility(View.VISIBLE);
        getController().getShowView().setAlpha(0.f);
        flipIn.setTarget(getController().getShowView());
        flipOut.setTarget(getController().getHideView());
    }

    public void cancel() {
        flipIn.cancel();
        flipOut.cancel();
    }

    public IControllerView getController() {
        return controller;
    }

    public void setController(IControllerView controller) {
        this.controller = controller;
    }

}
