package com.zazoapp.client.ui.view.downloadingview.animation;

/**
 * Created by sergii on 18.11.15.
 */
public interface IViewColorChangeAnimationListener extends IViewAnimationListener {
    int getStartColor();
    int getFinalColor();

    void setBackgroundColor(Integer aColor);

    void setDrawRing(boolean aIsDrawRings);
}
