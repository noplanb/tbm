package com.zazoapp.client.ui.view.transferview.animation;

import com.zazoapp.client.ui.view.transferview.animation.listeners.ITransferAnimationListener;

/**
 * Created by Serhii on 31.12.2015.
 */
public abstract class TransferAnimationController {
    public abstract void cancel();
    public abstract void start();
    public abstract void reset();

    /* update in percent from 0 to 100*/
    public abstract void updateProgress(float aValue);

    public abstract boolean isProgressFinish();

    private ITransferAnimationListener endListener;

    public void setEndAnimationListener(ITransferAnimationListener listener) {
        endListener = listener;
    }

    public ITransferAnimationListener getEndAnimationListener() {
        return endListener;
    }
}
