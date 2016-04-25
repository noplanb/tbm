package com.zazoapp.client.ui;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import com.zazoapp.client.bench.InviteManager;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.multimedia.AudioManager;
import com.zazoapp.client.multimedia.CameraManager;
import com.zazoapp.client.multimedia.Player;
import com.zazoapp.client.multimedia.Recorder;
import com.zazoapp.client.ui.helpers.VideoRecorderManager;

/**
 * Created by skamenkovych@codeminders.com on 4/25/2016.
 */
public class BaseManagerHolder implements BaseManagerProvider {
    private static final String TAG = BaseManagerHolder.class.getSimpleName();
    private InviteManager inviteManager;
    private AudioManager audioManager;
    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private Recorder videoRecorder;
    private Player videoPlayer;
    private Features features;
    private boolean isBaseInited;

    public void init(Context context, FragmentActivity activity, InviteManager.InviteDialogListener inviteDialogListener) {
        if (!isBaseInited) {
            inviteManager = new InviteManager(context);
            audioManager = new AudioManager(context, this);
            sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            if (proximitySensor == null) {
                Log.i(TAG, "Proximity sensor not found");
            }
            isBaseInited = true;
        }
        inviteManager.setListener(inviteDialogListener);
        videoRecorder = new VideoRecorderManager(context, this);
        features = new Features(activity);
    }

    public void registerManagers() {
        audioManager.gainFocus();
        sensorManager.registerListener(audioManager, proximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void unregisterManagers() {
        videoRecorder.pause(true);
        CameraManager.releaseCamera();
        getPlayer().release();
        audioManager.abandonFocus();
        audioManager.reset();
        sensorManager.unregisterListener(audioManager);
    }

    public void setVideoPlayer(Player player) {
        videoPlayer = player;
    }

    @Override
    public InviteManager getInviteHelper() {
        return inviteManager;
    }

    @Override
    public AudioManager getAudioController() {
        return audioManager;
    }

    public SensorManager getSensorManager() {
        return sensorManager;
    }

    public Sensor getProximitySensor() {
        return proximitySensor;
    }

    @Override
    public Recorder getRecorder() {
        return videoRecorder;
    }

    @Override
    public Player getPlayer() {
        return videoPlayer != null ? videoPlayer : Player.STUB;
    }

    @Override
    public Features getFeatures() {
        return features;
    }
}
