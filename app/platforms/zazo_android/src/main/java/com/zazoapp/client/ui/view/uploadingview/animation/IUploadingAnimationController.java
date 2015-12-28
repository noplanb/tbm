package com.zazoapp.client.ui.view.uploadingview.animation;

/**
 * Created by sergii on 14.11.15.
 */
public interface IUploadingAnimationController {

    void cancel();
    void start();
    void reset();

    /** update in percent from 0 to 100*/
    void updateProgress(float aValue);

    boolean isProgressFinish();
}
