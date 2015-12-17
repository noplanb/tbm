package com.zazoapp.client.ui.view.rotationcircleview.animation;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.zazoapp.client.R;

/**
 * Created by sergii on 11.11.15.
 */
public class AnimationController implements FlipFlopAnimation.IControllerView
        , Animator.AnimatorListener
        , IRotationCircleViewController{

    private static final String TAG = AnimationController.class.getSimpleName();

    private final Handler handler;
    private final Runnable runnableStart;

    private FlipFlopAnimation flipFlopAnimation;
    private Animator flipInCancel;
    private View primaryView;
    private View secondaryView;

    boolean isForward = true;
    boolean isCancel = true;
    private boolean isAnimationFinish = true;
    private long delayForBackwardAnimation;

    public AnimationController( Context aContext ){

        handler = new Handler();
        runnableStart = createRunnableStart();
        initAnimation(aContext);
    }

    private Runnable createRunnableStart() {
        return new Runnable() {
            @Override
            public void run() {
                start();
            }
        };
    }

    private void initAnimation(Context aContext) {
        flipFlopAnimation = new FlipFlopAnimation( aContext );
        flipFlopAnimation.setController(this);
        flipFlopAnimation.setExternalListener(this);

        flipInCancel = AnimatorInflater.loadAnimator(aContext,
                R.animator.rcv_flip_in_cencel);
    }

    private void swapState() {
        isForward = !isForward;
    }

    public void setPrimaryView(View primaryView) {
        this.primaryView = primaryView;
        flipInCancel.setTarget(primaryView);
    }

    public void setSecondaryView(View secondaryView) {
        this.secondaryView = secondaryView;
    }

    @Override
    public long getAnimDuration() {
        return flipFlopAnimation.getDuration();
    }

    @Override
    public void setAnimDuration(long animDuration) {
        Log.d(TAG, "setAnimDuration() called with: " + "animDuration = [" + animDuration + "]");
        flipFlopAnimation.setDuration( animDuration );
    }

    @Override
    public void setDelayForBackwardAnimation(long aDelayForBackwardAnimation) {
        this.delayForBackwardAnimation = aDelayForBackwardAnimation;
    }

    @Override
    public long getDelayForBackwardAnimation() {
        return delayForBackwardAnimation;
    }

    @Override
    public View getHideView() {
        return isForward ? primaryView : secondaryView;
    }

    @Override
    public View getShowView() {
        return isForward ? secondaryView : primaryView;
    }

    @Override
    public void start() {
        if (isAnimationFinish){
            isAnimationFinish = false;
            isCancel = false;
            flipFlopAnimation.start();
        } else {
            cancel();
        }
    }

    @Override
    public void cancel() {
        flipFlopAnimation.cancel();
        primaryView.setVisibility(View.VISIBLE);
        secondaryView.setVisibility(View.INVISIBLE);
        flipInCancel.start();
        handler.removeCallbacks(runnableStart);
        isForward = true;
        isCancel = true;
    }

    @Override
    public void onAnimationStart(Animator animation) {
        Log.d(TAG, "onAnimationStart() called with: " + "animation = [" + animation + "]");
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        Log.d(TAG, "onAnimationEnd() called with: " + "animation = [" + animation + "]");
        isAnimationFinish = true;
        swapState();
        if( !isForward && !isCancel ){
            handler.postDelayed( runnableStart, getDelayForBackwardAnimation() );
        }
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        Log.d(TAG, "onAnimationCancel() called with: " + "animation = [" + animation + "]");
    }

    @Override
    public void onAnimationRepeat(Animator animation) {
        Log.d(TAG, "onAnimationRepeat() called with: " + "animation = [" + animation + "]");
    }
}
