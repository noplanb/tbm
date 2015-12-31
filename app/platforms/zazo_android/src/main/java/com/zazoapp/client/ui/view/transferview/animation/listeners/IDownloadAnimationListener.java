package com.zazoapp.client.ui.view.transferview.animation.listeners;

/**
 * Created by sergii on 18.11.15.
 */
public interface IDownloadAnimationListener extends IArrowShowAnimationListener
        , IArrowHideAnimationListener
        , IProgressAnimationListener
        , IColorChangeAnimationListener {


    class Stub implements IDownloadAnimationListener {

        @Override
        public void onArrowHideAnimationFinish() {

        }

        @Override
        public void onArrowShowAnimationFinish() {

        }

        @Override
        public void onColorChangeAnimationFinish() {

        }

        @Override
        public void onProgressAnimationFinish() {

        }
    }
}
