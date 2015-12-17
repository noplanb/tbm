package com.zazoapp.client.ui.view.rotationcircleview.animation;

/**
 * Created by sergii on 14.11.15.
 */
public interface IRotationCircleViewController {
    long getAnimDuration();
    void setAnimDuration(long animDuration);

    long getDelayForBackwardAnimation();
    void setDelayForBackwardAnimation(long animDuration);

    void cancel();
    void start();
}
