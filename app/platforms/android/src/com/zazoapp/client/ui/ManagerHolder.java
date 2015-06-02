package com.zazoapp.client.ui;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;
import com.zazoapp.client.R;
import com.zazoapp.client.bench.BenchController;
import com.zazoapp.client.bench.InviteManager;
import com.zazoapp.client.multimedia.AudioManager;
import com.zazoapp.client.multimedia.CameraManager;
import com.zazoapp.client.multimedia.Player;
import com.zazoapp.client.multimedia.Recorder;
import com.zazoapp.client.multimedia.VideoPlayer;
import com.zazoapp.client.tutorial.Tutorial;
import com.zazoapp.client.tutorial.TutorialLayout;
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
    private Tutorial tutorial;

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
        tutorial = new Tutorial((TutorialLayout) activity.findViewById(R.id.tutorial_layout), activity);
    }

    public void registerManagers() {
        audioManager.gainFocus();
        sensorManager.registerListener(audioManager, proximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
        tutorial.registerCallbacks();
        //Bug 138 fix. reload phone contacts data because of new items in contact book are possible after resume
        benchController.loadContacts();
    }

    public void unregisterManagers() {
        videoRecorder.pause();
        CameraManager.releaseCamera();
        videoPlayer.release();
        audioManager.abandonFocus();
        audioManager.reset();
        sensorManager.unregisterListener(audioManager);
        tutorial.unregisterCallbacks();
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

    public Tutorial getTutorial() {
        return tutorial;
    }
}
