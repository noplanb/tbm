package com.zazoapp.client.ui;

import com.zazoapp.client.bench.InviteHelper;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.multimedia.AudioController;
import com.zazoapp.client.multimedia.Player;
import com.zazoapp.client.multimedia.Recorder;

/**
 * Created by skamenkovych@codeminders.com on 4/25/2016.
 */
public interface BaseManagerProvider {
    AudioController getAudioController();
    Recorder getRecorder();
    Player getPlayer();
    InviteHelper getInviteHelper();
    Features getFeatures();
    void registerManagers();
    void unregisterManagers();
}
