package com.zazoapp.client.multimedia;

import android.view.View;
import android.view.ViewGroup;
import com.zazoapp.client.ui.view.VideoView;

/**
 * Created by skamenkovych@codeminders.com on 4/21/2015.
 */
public interface Player {
    /**
     * Init VideoPlayer instance
     * @param videoBody parent layout of VideoView
     * @param videoView VideoView
     */
    void init(ViewGroup videoBody, VideoView videoView);
    void togglePlayOverView(View view, String friendId);
    void stop();
    void release();
    void setVolume(float volume);
    void registerStatusCallbacks(StatusCallbacks statusCallback);
    void unregisterStatusCallbacks(StatusCallbacks statusCallback);
    boolean isPlaying();
    void rewind(int msec);
    void restartAfter(int delay);

    interface StatusCallbacks {
        void onVideoPlaying(String friendId, String videoId);
        void onVideoStopPlaying(String friendId);
        void onCompletion(String friendId);
        void onVideoPlaybackError(String friendId, String videoId);
    }
}
