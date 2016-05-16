package com.zazoapp.client.multimedia;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Build;
import android.util.Log;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.ui.BaseManagerProvider;

/**
 * Created by skamenkovych@codeminders.com on 4/20/2015.
 */
public class AudioManager implements SensorEventListener, AudioController {

    private static final String TAG = AudioManager.class.getSimpleName();

    private android.media.AudioManager.OnAudioFocusChangeListener focusChangeListener;
    private android.media.AudioManager audioManager;
    private BaseManagerProvider managerProvider;
    private boolean hasFocus;
    private boolean isSpeakerPhoneOn = true;
    private boolean isProximityClose = false;

    private static final int GAIN_TYPE = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            ? android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            : android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT;

    public AudioManager(Context context, BaseManagerProvider managerProvider) {
        audioManager = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.managerProvider = managerProvider;
        initAudioFocusListener();
    }

    private void initAudioFocusListener() {
        focusChangeListener = new android.media.AudioManager.OnAudioFocusChangeListener() {
            public void onAudioFocusChange(int focusChange) {
                Log.i(TAG, "focus changed: " + focusChange);
                Player player = managerProvider.getPlayer();
                switch (focusChange) {
                    case android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    case android.media.AudioManager.AUDIOFOCUS_LOSS:
                        player.stop();
                        if (managerProvider.getRecorder().isRecording()) {
                            managerProvider.getRecorder().cancel();
                        }
                        break;
                    case android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        player.setVolume(0.1f);
                        break;
                    case android.media.AudioManager.AUDIOFOCUS_GAIN:
                        player.setVolume(1.0f);
                        break;
                }
                hasFocus = focusChange == android.media.AudioManager.AUDIOFOCUS_GAIN;
            }
        };
    }

    @Override
    public boolean gainFocus() {
        if (hasFocus) {
            return true;
        }
        int result = audioManager.requestAudioFocus(focusChangeListener, android.media.AudioManager.STREAM_MUSIC, GAIN_TYPE);
        if (result != android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Dispatch.dispatch("AudioManager: couldn't get focus");
        }
        return hasFocus = result == android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    @Override
    public void abandonFocus() {
        if (hasFocus) {
            hasFocus = audioManager.abandonAudioFocus(focusChangeListener) != android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
    }

    @Override
    public boolean hasFocus() {
        return hasFocus;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        isProximityClose = event.values[0] == 0;
        Log.i(TAG, "Proximity is close " + isProximityClose);
        if (managerProvider.getFeatures().isUnlocked(Features.Feature.EARPIECE)) {
            setSpeakerPhoneOn(managerProvider.getPlayer().isPlaying());
        }
    }

    @Override
    public void setExclusive(boolean exclusive) {
        int[] streams = new int[] {
                android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.STREAM_SYSTEM
        };

        for (int stream : streams) {
            audioManager.setStreamMute(stream, exclusive);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public boolean isSpeakerPhoneOn() {
        return isSpeakerPhoneOn;
    }

    @Override
    public void setSpeakerPhoneOn(boolean enable) {
        Log.i(TAG, "setSpeakerPhoneOn " + isSpeakerPhoneOn + " " + enable + " " + isProximityClose);
        if (isSpeakerPhoneOn == (!enable || !isProximityClose)) {
            return;
        }
        if (enable) {
            // if feature is locked - always true, otherwise opposite to isProximityClose
            isSpeakerPhoneOn = !managerProvider.getFeatures().isUnlocked(Features.Feature.EARPIECE) || !isProximityClose;
            if (isSpeakerPhoneOn) {
                audioManager.setMode(android.media.AudioManager.MODE_NORMAL);
            } else {
                audioManager.setMode(android.media.AudioManager.MODE_IN_COMMUNICATION);
            }
            audioManager.setSpeakerphoneOn(isSpeakerPhoneOn);
            managerProvider.getPlayer().changeAudioStream();
        } else {
            resetSpeakerPhoneMode();
        }
    }

    @Override
    public void reset() {
        resetSpeakerPhoneMode();
    }

    private void resetSpeakerPhoneMode() {
        if (hasFocus) {
            audioManager.setMode(android.media.AudioManager.MODE_NORMAL);
            audioManager.setSpeakerphoneOn(true);
            if (!isSpeakerPhoneOn) {
                isSpeakerPhoneOn = true;
                managerProvider.getPlayer().changeAudioStream();
            }
        }
    }
}
