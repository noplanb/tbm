package com.zazoapp.client.ui.view.transferview.animation.listeners;

import android.support.annotation.IntRange;

/**
 * Created by sergii on 18.11.15.
 */
public interface IViewColorChangeAnimationListener extends IViewAnimationListener {
    int getStartColor();
    int getFinalColor();

    void setBackgroundColor(Integer aColor);

    void setDrawRing(boolean aIsDrawRings);

    void setRingOpacity(@IntRange(from=0,to=255) int opacity);
}
