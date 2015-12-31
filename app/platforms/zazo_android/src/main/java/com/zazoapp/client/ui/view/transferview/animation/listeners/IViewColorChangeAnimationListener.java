package com.zazoapp.client.ui.view.transferview.animation.listeners;

import com.zazoapp.client.ui.view.transferview.animation.listeners.IViewAnimationListener;

/**
 * Created by sergii on 18.11.15.
 */
public interface IViewColorChangeAnimationListener extends IViewAnimationListener {
    int getStartColor();
    int getFinalColor();

    void setBackgroundColor(Integer aColor);

    void setDrawRing(boolean aIsDrawRings);
}
