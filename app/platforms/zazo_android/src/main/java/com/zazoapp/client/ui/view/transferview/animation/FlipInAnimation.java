package com.zazoapp.client.ui.view.transferview.animation;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.view.View;
import com.zazoapp.client.R;
import com.zazoapp.client.ui.view.transferview.animation.listeners.IArrowShowAnimationListener;

/**
 * Created by sergii on 14.11.15.
 */
// TODO: 18.11.15 remove
// TODO: 18.11.15 extends from DownloadAnimator
public class FlipInAnimation implements Animator.AnimatorListener {

    private static final String TAG = FlipInAnimation.class.getSimpleName();

    private AnimatorSet flipIn;
    private View view;
    //TODO Depend on behavior remove
    private Animator flipInCancel;

    private boolean isAnimationFinish = true;
    private IArrowShowAnimationListener arrowShowAnimationListener;

    public FlipInAnimation(Context aContext){
        initAnimation(aContext);
    }

    private void initAnimation(Context aContext) {
        flipIn = (AnimatorSet) AnimatorInflater.loadAnimator(aContext,
                R.animator.rcv_half_flip_left_in);

        flipIn.addListener(this);

        flipInCancel = AnimatorInflater.loadAnimator(aContext,
                R.animator.rcv_flip_in_cencel);
    }

    public void setView(View aView) {
        view = aView;
        flipInCancel.setTarget(view);
        flipIn.setTarget(view);
    }

    public void start() {
        if ( view == null ){
            return;
        }
        isAnimationFinish = false;
        view.setVisibility(View.VISIBLE);

        flipIn.start();
    }

    public void cancel() {
        flipIn.cancel();
        flipInCancel.start();
        if ( view != null ){
            view.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
        isAnimationFinish = true;
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    public boolean isAnimationFinish() {
        return isAnimationFinish;
    }

}

