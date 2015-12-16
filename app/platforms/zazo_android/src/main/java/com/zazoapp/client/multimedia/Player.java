package com.zazoapp.client.multimedia;

import android.view.View;

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
    void togglePlayOverView(View view, String friendId);
    void stop();
    void release();
    void setVolume(float volume);
    void registerStatusCallbacks(StatusCallbacks statusCallback);
    void unregisterStatusCallbacks(StatusCallbacks statusCallback);
    boolean isPlaying();
    void rewind(int msec);
    void restartAfter(int delay);
    void changeAudioStream();

    interface StatusCallbacks {
        void onVideoPlaying(String friendId, String videoId);
        void onVideoStopPlaying(String friendId);
        void onCompletion(String friendId);
        void onVideoPlaybackError(String friendId, String videoId);
    }
}
