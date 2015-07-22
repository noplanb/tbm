package com.zazoapp.client.features;

import com.zazoapp.client.core.PreferencesHelper;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.ui.MainActivity;

/**
 * Created by skamenkovych@codeminders.com on 7/22/2015.
 */
public class Features {
    public enum Feature {
        SWITCH_CAMERA(),
        ABORT_RECORDING(),
        DELETE_FRIEND(),
        EARPIECE(),
        CAROUSEL(),
        ;

        private String prefName;

        Feature() {
            prefName = "pref_feature_" + name().toLowerCase();
        }

        String getPrefName() {
            return prefName;
        }
    }

    private MainActivity activity;
    private PreferencesHelper prefs;

    public Features(MainActivity activity) {
        this.activity = activity;
        prefs = new PreferencesHelper(activity);
    }

    private void unlock(Feature feature) {
        prefs.putBoolean(feature.getPrefName(), true);
    }

    public boolean isUnlocked(Feature feature) {
        return prefs.getBoolean(feature.getPrefName(), DebugConfig.getInstance(activity).isAllFeaturesEnabled());
    }
}
