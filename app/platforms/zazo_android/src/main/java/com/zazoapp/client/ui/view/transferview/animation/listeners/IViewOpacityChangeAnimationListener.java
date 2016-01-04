package com.zazoapp.client.ui.view.transferview.animation.listeners;

import android.support.annotation.IntRange;

/**
 * Created by sergii on 22.11.15.
 */
public interface IViewOpacityChangeAnimationListener extends IViewAnimationListener {
    void setOpacity(@IntRange(from=0, to=255) int aOpacity);
}
