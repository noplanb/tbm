package com.zazoapp.client.ui.view.transferview.animation.listeners;

/**
 * Created by sergii on 15.11.15.
 */
public interface IViewProgressAnimationListener extends IViewAnimationListener {

    void setActualAngleProgressAnimation(float actualAngle);
    void setSweepAngleProgressValue(float sweepAngle);

    float getProgressCurrentAngle();

    void setDrawRing(boolean isDrawRing);
}