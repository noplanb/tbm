package com.zazoapp.client.ui.view.downloadingview.animation;

/**
 * Created by sergii on 15.11.15.
 */
public interface IViewMoveDownAnimationListener extends IViewAnimationListener {

    void setShift(int shift);
    int getFinalValue();
}
