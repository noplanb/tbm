package com.zazoapp.client.ui;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import com.zazoapp.client.bench.BenchController;
import com.zazoapp.client.multimedia.VideoPlayer;
import com.zazoapp.client.tutorial.Tutorial;

/**
 * Created by skamenkovych@codeminders.com on 5/6/2015.
 */
public class ManagerHolder extends BaseManagerHolder implements ZazoManagerProvider {
    private static final String TAG = ManagerHolder.class.getSimpleName();

    private BenchController benchController;
    private Tutorial tutorial;
    private boolean isInited;

    public void init(Context context, MainFragment fragment, FragmentActivity activity) {
        super.init(context, activity, fragment);
        if (!isInited) {
            benchController = new BenchController(context, this);
            isInited = true;
        }
        benchController.setBenchListener(fragment);
        setVideoPlayer(new VideoPlayer(activity, this));
        tutorial = new Tutorial(activity, this);
    }

    public void registerManagers() {
        super.registerManagers();
        tutorial.registerCallbacks();
        //Bug 138 fix. reload phone contacts data because of new items in contact book are possible after resume
        benchController.loadContacts();
    }

    public void unregisterManagers() {
        super.unregisterManagers();
        tutorial.unregisterCallbacks();
    }

    @Override
    public BenchController getBenchViewManager() {
        return benchController;
    }

    @Override
    public Tutorial getTutorial() {
        return tutorial;
    }

}
