package com.zazoapp.client.multimedia;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Build;
import android.util.Log;
import com.zazoapp.client.ui.helpers.VideoRecorderManager;

/**
 * Created by skamenkovych@codeminders.com on 4/20/2015.
 */
public class AudioManager implements SensorEventListener {

    private static final String TAG = AudioManager.class.getSimpleName();

    private android.media.AudioManager.OnAudioFocusChangeListener focusChangeListener;
    private android.media.AudioManager audioManager;
    private VideoRecorderManager recorder;
    private VideoPlayer player;
    private boolean hasFocus;

    private static final int GAIN_TYPE = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            ? android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            : android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT;

    public AudioManager(Context context, VideoRecorderManager recorder, VideoPlayer player) {
        audioManager = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.recorder = recorder;
        this.player = player;
        initAudioFocusListener();
    }

    private void initAudioFocusListener() {
        focusChangeListener = new android.media.AudioManager.OnAudioFocusChangeListener() {
            public void onAudioFocusChange(int focusChange) {
                Log.i(TAG, "focus changed: " + focusChange);
                switch (focusChange) {
                    case android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        player.stop();
                        break;
                    case android.media.AudioManager.AUDIOFOCUS_LOSS:
                        hasFocus = false;
                        player.stop();
                        break;
                    case android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        player.setVolume(0.1f);
                        break;
                    case android.media.AudioManager.AUDIOFOCUS_GAIN:
                        player.setVolume(1.0f);
                        break;
                }
            }
        };
    }

    /**
     * Request audio focus
     * @return true if audio focus has been granted
     */
    public boolean gainFocus() {
        if (hasFocus) {
            return true;
        }
        int result = audioManager.requestAudioFocus(focusChangeListener, android.media.AudioManager.STREAM_MUSIC, GAIN_TYPE);
        return hasFocus = result == android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    public void abandonFocus() {
        if (hasFocus) {
            hasFocus = audioManager.abandonAudioFocus(focusChangeListener) != android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
    }

    public boolean hasFocus() {
        return hasFocus;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.values[0] == 0) {
            audioManager.setMode(android.media.AudioManager.MODE_IN_CALL);
            audioManager.setSpeakerphoneOn(false);
        } else {
            audioManager.setMode(android.media.AudioManager.MODE_NORMAL);
            audioManager.setSpeakerphoneOn(true);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
