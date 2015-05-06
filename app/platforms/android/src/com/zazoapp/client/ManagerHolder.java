package com.zazoapp.client;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;
import com.zazoapp.client.bench.BenchController;
import com.zazoapp.client.bench.InviteManager;
import com.zazoapp.client.multimedia.AudioManager;
import com.zazoapp.client.multimedia.CameraManager;
import com.zazoapp.client.multimedia.Player;
import com.zazoapp.client.multimedia.Recorder;
import com.zazoapp.client.multimedia.VideoPlayer;
import com.zazoapp.client.ui.MainActivity;
import com.zazoapp.client.ui.helpers.VideoRecorderManager;

/**
 * Created by skamenkovych@codeminders.com on 5/6/2015.
 */
public class ManagerHolder {
    private static final String TAG = ManagerHolder.class.getSimpleName();

    private BenchController benchController;
    private InviteManager inviteManager;
    private AudioManager audioManager;
    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private Recorder videoRecorder;
    private Player videoPlayer;

    public void init(MainActivity activity) {
        inviteManager = new InviteManager(activity, activity);
        benchController = new BenchController(activity, activity);
        audioManager = new AudioManager(activity, activity);
        videoRecorder = new VideoRecorderManager(activity, activity);
        videoPlayer = new VideoPlayer(activity, activity);
        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (proximitySensor == null) {
            Log.i(TAG, "Proximity sensor not found");
        }
        benchController.onDataLoaded();
    }

    public void registerManagers() {
        audioManager.gainFocus();
        sensorManager.registerListener(audioManager, proximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void unregisterManagers() {
        videoRecorder.pause();
        CameraManager.releaseCamera();
        videoPlayer.release();
        audioManager.abandonFocus();
        audioManager.reset();
        sensorManager.unregisterListener(audioManager);
    }

    public BenchController getBenchController() {
        return benchController;
    }

    public InviteManager getInviteManager() {
        return inviteManager;
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }

    public SensorManager getSensorManager() {
        return sensorManager;
    }

    public Sensor getProximitySensor() {
        return proximitySensor;
    }

    public Recorder getVideoRecorder() {
        return videoRecorder;
    }

    public Player getVideoPlayer() {
        return videoPlayer;
    }

}
