package com.zazoapp.client.ui;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import com.zazoapp.client.R;
import com.zazoapp.client.bench.BenchController;
import com.zazoapp.client.bench.InviteManager;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.multimedia.AudioManager;
import com.zazoapp.client.multimedia.CameraManager;
import com.zazoapp.client.multimedia.Player;
import com.zazoapp.client.multimedia.Recorder;
import com.zazoapp.client.multimedia.VideoPlayer;
import com.zazoapp.client.tutorial.Tutorial;
import com.zazoapp.client.ui.helpers.VideoRecorderManager;
import com.zazoapp.client.ui.view.TouchBlockScreen;

/**
 * Created by skamenkovych@codeminders.com on 5/6/2015.
 */
public class ManagerHolder implements ZazoManagerProvider {
    private static final String TAG = ManagerHolder.class.getSimpleName();

    private BenchController benchController;
    private InviteManager inviteManager;
    private AudioManager audioManager;
    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private Recorder videoRecorder;
    private Player videoPlayer;
    private Tutorial tutorial;
    private Features features;
    private boolean isInited;

    public void init(Context context, MainFragment fragment, FragmentActivity activity) {
        if (!isInited) {
            inviteManager = new InviteManager(context);
            benchController = new BenchController(context, this);
            audioManager = new AudioManager(context, this);
            sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            if (proximitySensor == null) {
                Log.i(TAG, "Proximity sensor not found");
            }
            isInited = true;
        }
        inviteManager.setListener(fragment);
        videoRecorder = new VideoRecorderManager(context, this);
        videoPlayer = new VideoPlayer(activity, this, (TouchBlockScreen) activity.findViewById(R.id.block_screen));
        tutorial = new Tutorial(activity, this);
        features = new Features(activity);
    }

    public void registerManagers() {
        audioManager.gainFocus();
        sensorManager.registerListener(audioManager, proximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
        tutorial.registerCallbacks();
        //Bug 138 fix. reload phone contacts data because of new items in contact book are possible after resume
        benchController.loadContacts();
    }

    public void unregisterManagers() {
        videoRecorder.pause(true);
        CameraManager.releaseCamera();
        videoPlayer.release();
        audioManager.abandonFocus();
        audioManager.reset();
        sensorManager.unregisterListener(audioManager);
        tutorial.unregisterCallbacks();
    }

    @Override
    public BenchController getBenchViewManager() {
        return benchController;
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
        return videoPlayer;
    }

    @Override
    public Tutorial getTutorial() {
        return tutorial;
    }

    @Override
    public Features getFeatures() {
        return features;
    }
}
