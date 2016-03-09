package com.zazoapp.client.features;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import com.zazoapp.client.R;
import com.zazoapp.client.core.PreferencesHelper;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.notification.NotificationAlertManager;
import com.zazoapp.client.ui.ZazoManagerProvider;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.DialogShower;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by skamenkovych@codeminders.com on 7/22/2015.
 */
public class Features {

    public enum Feature {
        SWITCH_CAMERA(R.string.feature_switch_camera_action),
        ABORT_RECORDING(R.string.feature_abort_recording_action),
        DELETE_FRIEND(R.string.feature_delete_friend_action),
        PLAY_FULLSCREEN(R.string.feature_play_fullscreen_action),
        PAUSE_PLAYBACK(R.string.feature_pause_playback_action),
        EARPIECE(R.string.feature_earpiece_action),
        CAROUSEL(R.string.feature_carousel_action),
        ;

        private final String prefName;
        private final int actionId;

        Feature(int action) {
            prefName = "pref_feature_" + name().toLowerCase();
            actionId = action;
        }

        String getPrefName() {
            return prefName;
        }

        public String getAction(Context context) {
            return context.getString(actionId);
        }

        public boolean isUnlocked(Context context) {
            PreferencesHelper prefs = new PreferencesHelper(context);
            return prefs.getBoolean(getPrefName(), false) || DebugConfig.Bool.ENABLE_ALL_FEATURES.get();
        }
    }

    private static final String PREF_SHOW_LAST_FEATURE = "pref_show_last_feature";
    private static final long[] awardVibrationPattern = {50, 300, 90, 100, 90, 100, 90, 330};
    private FragmentActivity activity;
    private PreferencesHelper prefs;
    private Set<FeatureChangedCallback> callbacks = new HashSet<>();
    private volatile boolean notifyOnChanged = true;

    public interface FeatureChangedCallback {
        void onFeatureChanged(Feature feature, boolean unlocked);
    }

    public Features(FragmentActivity activity) {
        this.activity = activity;
        prefs = new PreferencesHelper(this.activity);
    }

    private void unlock(Feature feature) {
        prefs.putBoolean(feature.getPrefName(), true);
        notifyCallbacks(feature, true);
    }

    public void unlockByName(String name) {
        if (name != null) {
            try {
                Feature feature = Feature.valueOf(name);
                unlock(feature);
            } catch (IllegalArgumentException e) {
            }
        }
    }
    public void lock(Feature feature) {
        prefs.remove(feature.getPrefName());
        notifyCallbacks(feature, false);
    }

    public void lockAll() {
        for (Feature feature : Feature.values()) {
            prefs.remove(feature.getPrefName());
        }
    }

    public boolean isUnlocked(Feature feature) {
        return prefs.getBoolean(feature.getPrefName(), false) || DebugConfig.Bool.ENABLE_ALL_FEATURES.get();
    }

    /**
     * Checks conditions and unlock appropriate features
     * @return last unlocked Feature or null if nothing was unlocked
     */
    public Feature checkAndUnlock() {
        int count = Math.min(calculateNumberOfUnlockedFeatures(), Feature.values().length);
        int leftToUnlock = Math.max(count - getUnlockedFeaturesNumber(), 0);
        Feature lastUnlocked = null;
        for (int i = 0; i < Feature.values().length && leftToUnlock > 0; i++) {
            if (!isUnlocked(Feature.values()[i])) {
                unlock(Feature.values()[i]);
                lastUnlocked = Feature.values()[i];
                leftToUnlock--;
            }
        }
        return lastUnlocked;
    }

    int calculateNumberOfUnlockedFeatures() {
        List<Friend> friends = FriendFactory.getFactoryInstance().all();
        int activatedInviteeCount = 0;
        int activatedNonInviteeCount = 0;
        for (Friend friend : friends) {
            if (friend.everSent()) {
                if (friend.isConnectionCreator()) {
                    activatedNonInviteeCount++;
                } else {
                    activatedInviteeCount++;
                }
            }
        }
        // If we are invitee than unlock activatedInviteeCount + (activatedNonInviteeCount > 0) ? 1 : 0 features;
        // If not just unlock activatedInviteeCount features
        return Math.max(0, activatedInviteeCount + ((activatedNonInviteeCount > 0 && UserFactory.current_user().isInvitee()) ? 1 : 0) - 1);
    }

    int getUnlockedFeaturesNumber() {
        int count = 0;
        for (Feature feature : Feature.values()) {
            if (isUnlocked(feature)) {
                count++;
            }
        }
        return count;
    }

    public void showFeatureAwardDialog(ZazoManagerProvider managers, Feature feature) {
        if (TbmApplication.getInstance().isForeground() && !managers.getPlayer().isPlaying() && !managers.getRecorder().isRecording()) {
            DialogShower.showFeatureAwardDialog(activity, feature);
            managers.getRecorder().stop(); // called to update recording indicators state
            prefs.putBoolean(PREF_SHOW_LAST_FEATURE, false);
        } else {
            prefs.putBoolean(PREF_SHOW_LAST_FEATURE, true);
            if (!TbmApplication.getInstance().isForeground() || Convenience.screenIsLockedOrOff(activity)) {
                NotificationAlertManager.alert(activity,
                        activity.getString(R.string.feature_unlock_message),
                        activity.getString(R.string.feature_unlock_discover_message),
                        awardVibrationPattern, NotificationAlertManager.NotificationType.FEATURE_AWARD.id());
            }
        }
    }

    public boolean showNextFeatureDialog(ZazoManagerProvider managers, boolean justUnlockedFeature) {
        if (TbmApplication.getInstance().isForeground() && !isAwardDialogShowed() && !managers.getBenchViewManager().isBenchShown()) {
            DialogShower.showNextFeatureDialog(activity, justUnlockedFeature);
            return true;
        }
        return false;
    }

    public boolean shouldShowAwardDialog() {
        return prefs.getBoolean(PREF_SHOW_LAST_FEATURE, false);
    }

    public boolean isAwardDialogShowed() {
        return DialogShower.isFeatureAwardDialogShown(activity);
    }

    public Feature nextFeature() {
        for (Feature feature : Feature.values()) {
            if (!isUnlocked(feature)) {
                return feature;
            }
        }
        return null;
    }

    public Feature lastUnlockedFeature() {
        Feature lastUnlocked = null;
        for (Feature feature : Feature.values()) {
            if (isUnlocked(feature)) {
                lastUnlocked = feature;
            } else {
                break;
            }
        }
        return lastUnlocked;
    }

    public static boolean allFeaturesOpened(PreferencesHelper prefs) {
        for (Feature feature : Feature.values()) {
            if (!prefs.getBoolean(feature.getPrefName(), false)) {
                return false;
            }
        }
        return true;
    }

    public static String retrieveFeaturesStatus(Context context) {
        PreferencesHelper prefs = new PreferencesHelper(context);
        StringBuilder status = new StringBuilder();
        for (Feature feature : Feature.values()) {
            status.append(feature.name());
            if (prefs.getBoolean(feature.getPrefName(), false)) {
                status.append(" \u2713");
            }
            if (feature.ordinal() != Feature.values().length - 1) {
                status.append("\n");
            }
        }
        return status.toString();
    }

    public final void addCallback(FeatureChangedCallback callback) {
        callbacks.add(callback);
    }

    public final void removeCallback(FeatureChangedCallback callback) {
        callbacks.remove(callback);
    }

    protected void notifyCallbacks(Feature feature, boolean unlocked) {
        if (notifyOnChanged) {
            for (FeatureChangedCallback callback : callbacks) {
                callback.onFeatureChanged(feature, unlocked);
            }
        }
    }
}
