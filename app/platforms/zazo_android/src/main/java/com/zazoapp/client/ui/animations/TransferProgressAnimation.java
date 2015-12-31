package com.zazoapp.client.ui.animations;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.support.annotation.DimenRes;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.zazoapp.client.R;
import com.zazoapp.client.ui.view.downloadingview.DownloadingView;
import com.zazoapp.client.ui.view.downloadingview.animation.listener.IDownloadAnimationListener;
import com.zazoapp.client.ui.view.uploadingview.UploadingView;

/**
 * Created by Serhii on 31.12.2015.
 */
public class TransferProgressAnimation {

    private static final int START_END_DURATION = 350;
    private static final int PROGRESS_DURATION = 750;

    @InjectView(R.id.downloading_animation_view) DownloadingView downloadingView;
    @InjectView(R.id.uploading_animation_view) UploadingView uploadingView;
    @InjectView(R.id.animation_background) View animationBackground;
    @InjectView(R.id.tw_unread_count) TextView twUnreadCount;
    private View parent;

    private TransferProgressAnimation(View v) {
        ButterKnife.inject(this, v);
        parent = v;
    }

    public static void animateDownloading(View parent, Runnable task) {
        new TransferProgressAnimation(parent).animateDownloadingInner(task);
    }

    private void animateDownloadingInner(final Runnable task) {
        animationBackground.setAlpha(0f);
        ValueAnimator startAnimation = ValueAnimator.ofFloat(0, 1f);
        startAnimation.setDuration(START_END_DURATION);
        startAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            float size = Math.min(getWidth(), getHeight()) * 0.5f;
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                setProgressAnimation(downloadingView, (int) (size * value));
                animationBackground.setAlpha(value * 0.5f);
            }
        });
        final ValueAnimator endAnimation = ValueAnimator.ofFloat(1f, 0f);
        endAnimation.setDuration(START_END_DURATION);
        endAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            float size = Math.min(getWidth(), getHeight()) * 0.5f;
            int contentRadius = getSize(R.dimen.grid_item_status_icon_content_radius);
            float endSize = contentRadius * 2;
            float targetX = twUnreadCount.getX() + contentRadius;
            float targetY = twUnreadCount.getY() + contentRadius;
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                setProgressFinishAnimation(downloadingView, (int) Math.abs((size - endSize) * value + endSize), 1 - value, targetX, targetY);
                animationBackground.setAlpha(value * 0.5f);
            }
        });
        downloadingView.setVisibility(View.VISIBLE);
        downloadingView.getAnimationController().cancel();
        downloadingView.getAnimationController().setExternalDownloadAnimationListener(new IDownloadAnimationListener.Stub() {
            @Override
            public void onArrowHideAnimationFinish() {
                endAnimation.start();
            }

            @Override
            public void onColorChangeAnimationFinish() {
                parent.post(task);
            }
        });

        final ValueAnimator progress = ValueAnimator.ofFloat(0, 100f);
        progress.setInterpolator(new LinearInterpolator());
        progress.setDuration(PROGRESS_DURATION);
        progress.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                downloadingView.getAnimationController().updateProgress(value);
            }
        });
        startAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                downloadingView.getAnimationController().start();
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
        v.setX(cX - radius + (targetX - cX) * fraction);
        v.setY(cY - radius + (targetY - cY) * fraction);
        v.setVisibility(View.VISIBLE);
    }
}
