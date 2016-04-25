package com.zazoapp.client.multimedia;

import android.view.ViewGroup;

/**
 * Created by skamenkovych@codeminders.com on 4/21/2015.
 */
public interface Recorder {
    /**
     * Starts recording for specified friend
     *
     * @param friendId
     */
    void start(String friendId);

    /**
     * Ends recording and save the result
     * @return true if video file was successfully recorded
     */
    boolean stop();

    /**
     * Cancels recording. Do not save anything
     */
    void cancel();

    /**
     * Retry connection to recording device
     */
    void reconnect();

    /**
     * Should be called in onResume of host activity
     */
    void resume();

    /**
     * Should be called in onPause of host activity
     *
     * @param release true to release camera
     */
    void pause(boolean release);

    /**
     * Indicates whether it is currently recording
     *
     * @return true if it is recording
     */
    boolean isRecording();

    /**
     * Set up preview to parent container
     *
     * @param container
     */
    void addPreviewTo(ViewGroup container);

    /**
     * Switches front/back cameras
     */
    void switchCamera();

    /**
     * Sets indicators of switch camera feature
     */
    void indicateSwitchCameraFeature();
}
