package com.zazoapp.client;

import com.zazoapp.client.bench.BenchViewManager;
import com.zazoapp.client.bench.InviteHelper;
import com.zazoapp.client.multimedia.AudioController;
import com.zazoapp.client.multimedia.Player;
import com.zazoapp.client.multimedia.Recorder;

/**
 * Created by skamenkovych@codeminders.com on 4/21/2015.
 */
public interface ZazoManagerProvider {
    BenchViewManager getBenchViewManager();
    AudioController getAudioController();
    Recorder getRecorder();
    Player getPlayer();
    InviteHelper getInviteHelper();
}
