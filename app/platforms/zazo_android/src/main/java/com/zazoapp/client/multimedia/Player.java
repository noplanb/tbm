package com.zazoapp.client.multimedia;

import android.support.annotation.IntDef;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by skamenkovych@codeminders.com on 4/21/2015.
 */
public interface Player {
    /**
     * Init VideoPlayer instance
     * @param rootView parent layout of VideoView
     *
     */
    void init(View rootView);
    boolean togglePlayOverView(View view, String friendId, @PlayFlags int options);
    void stop();
    void release();
    void setVolume(float volume);
    void registerStatusCallbacks(StatusCallbacks statusCallback);
    void unregisterStatusCallbacks(StatusCallbacks statusCallback);
    boolean isPlaying();
    void rewind(int msec);
    void restartAfter(int delay);
    void changeAudioStream();
    void updatePlayerPosition();

    interface StatusCallbacks {
        void onVideoPlaying(String friendId, String videoId);
        void onVideoStopPlaying(String friendId);
        void onCompletion(String friendId);
        void onVideoPlaybackError(String friendId, String videoId);
    }

    Player STUB = new Player() {
        @Override
        public void init(View rootView) {
        }

        @Override
        public boolean togglePlayOverView(View view, String friendId, int options) {
            return false;
        }

        @Override
        public void stop() {
        }

        @Override
        public void release() {
        }

        @Override
        public void setVolume(float volume) {
        }

        @Override
        public void registerStatusCallbacks(StatusCallbacks statusCallback) {
        }

        @Override
        public void unregisterStatusCallbacks(StatusCallbacks statusCallback) {
        }

        @Override
        public boolean isPlaying() {
            return false;
        }

        @Override
        public void rewind(int msec) {
        }

        @Override
        public void restartAfter(int delay) {
        }

        @Override
        public void changeAudioStream() {
        }

        @Override
        public void updatePlayerPosition() {
        }
    };

    @IntDef(flag = true, value = {PlayOptions.FULLSCREEN, PlayOptions.TRANSCRIPT})
    @Retention(RetentionPolicy.SOURCE)
    @interface PlayFlags {}
}
