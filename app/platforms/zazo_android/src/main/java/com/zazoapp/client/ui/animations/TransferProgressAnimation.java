package com.zazoapp.client.ui.animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.support.annotation.DimenRes;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.zazoapp.client.R;
import com.zazoapp.client.ui.view.ITransferView;
import com.zazoapp.client.ui.view.transferview.DownloadingView;
import com.zazoapp.client.ui.view.transferview.UploadingView;
import com.zazoapp.client.ui.view.transferview.animation.listeners.ITransferAnimationListener;

/**
 * Created by Serhii on 31.12.2015.
 */
public class TransferProgressAnimation {

    private static final int START_END_DURATION = 350;
    private static final int PROGRESS_DURATION = 750;

    @InjectView(R.id.downloading_animation_view) DownloadingView downloadingView;
    @InjectView(R.id.uploading_animation_view) UploadingView uploadingView;
    @InjectView(R.id.animation_background) View animationBackground;
    @InjectView(R.id.unread_count_layout) View anchorLayout;
    private View parent;

    private TransferProgressAnimation(View v) {
        ButterKnife.inject(this, v);
        parent = v;
    }

    public static void animateDownloading(View parent, Runnable task) {
        TransferProgressAnimation anim = new TransferProgressAnimation(parent);
        anim.animateTransferView(anim.downloadingView, task);
    }

    public static void animateUploading(View parent, Runnable task) {
        TransferProgressAnimation anim = new TransferProgressAnimation(parent);
        anim.animateTransferView(anim.uploadingView, task);
    }

    private void animateTransferView(final ITransferView transferView, final Runnable task) {
        animationBackground.setAlpha(0f);
        ValueAnimator startAnimation = ValueAnimator.ofFloat(0, 1f);
        startAnimation.setDuration(START_END_DURATION);
        startAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            float size = Math.min(getWidth(), getHeight()) * 0.5f;
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                setProgressAnimation(transferView, (int) (size * value));
                animationBackground.setAlpha(value * 0.5f);
            }
        });
        final ValueAnimator endAnimation = ValueAnimator.ofFloat(1f, 0f);
        endAnimation.setDuration(START_END_DURATION);
        endAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            float size = Math.min(getWidth(), getHeight()) * 0.5f;
            int padding = getSize(R.dimen.grid_item_status_icon_padding);
            int contentRadius = anchorLayout.getWidth() / 2 - padding;
            float endSize = contentRadius * 2;
            float targetCenterX = anchorLayout.getX() + anchorLayout.getPivotX() - padding;
            float targetCenterY = anchorLayout.getY() + anchorLayout.getPivotY() - padding;
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                setProgressFinishAnimation(transferView, (int) Math.abs((size - endSize) * value + endSize), 1 - value, targetCenterX, targetCenterY);
                animationBackground.setAlpha(value * 0.5f);
            }
        });
        endAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                parent.post(new Runnable() {
                    @Override
                    public void run() {
                        task.run();
                        transferView.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });
        transferView.setVisibility(View.VISIBLE);
        transferView.getAnimationController().cancel();
        transferView.getAnimationController().setEndAnimationListener(new ITransferAnimationListener() {
            @Override
            public void onAnimationEnd() {
                endAnimation.start();
            }
        });

        final ValueAnimator progress = ValueAnimator.ofFloat(0, 100f);
        progress.setInterpolator(new LinearInterpolator());
        progress.setDuration(PROGRESS_DURATION);
        progress.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                transferView.getAnimationController().updateProgress(value);
            }
        });
        startAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                transferView.getAnimationController().start();
                progress.start();
            }
        });
        startAnimation.start();
    }

    private int getSize(@DimenRes int dimen) {
        return parent.getResources().getDimensionPixelSize(dimen);
    }

    /**
     * @param v animated view, should be placed inside FrameLayout
     */
    private void setProgressAnimation(View v, int size) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) v.getLayoutParams();
        params.width = size;
        params.height = size;
        v.setLayoutParams(params);
        v.setX((getWidth() - size) / 2);
        v.setY(2 * getHeight() / 5 - size / 2);
        v.setVisibility(View.VISIBLE);
    }

    private int getWidth() {
        return parent.getWidth();
    }

    private int getHeight() {
        return parent.getHeight();
    }
    /**
     * @param v animated view, should be placed inside FrameLayout
     */
    private void setProgressFinishAnimation(View v, int size, float fraction, float targetX, float targetY) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) v.getLayoutParams();
        params.width = size;
        params.height = size;
        v.setLayoutParams(params);
        int radius = size / 2;
        int cX = getWidth() / 2;
        int cY = 2 * getHeight() / 5;
        // move along the line between two center points
        v.setX(cX + (targetX - cX) * fraction - radius);
        v.setY(cY + (targetY - cY) * fraction - radius);
        v.setVisibility(View.VISIBLE);
    }
}
