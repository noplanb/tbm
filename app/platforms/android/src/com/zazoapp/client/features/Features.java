package com.zazoapp.client.features;

import android.app.Activity;
import android.content.Context;
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
    }
    private static final String PREF_SHOW_LAST_FEATURE = "pref_show_last_feature";
    private static final long[] awardVibrationPattern = {50, 300, 90, 100, 90, 100, 90, 330};
    private Activity activity;
    private PreferencesHelper prefs;
    private Set<FeatureChangedCallback> callbacks = new HashSet<>();
    private volatile boolean notifyOnChanged = true;

    public interface FeatureChangedCallback {
        void onFeatureChanged(Feature feature, boolean unlocked);
    }

    public Features(Activity activity) {
        this.activity = activity;
        prefs = new PreferencesHelper(this.activity);
    }

    private void unlock(Feature feature) {
        prefs.putBoolean(feature.getPrefName(), true);
        notifyCallbacks(feature, true);
    }

    public void lock(Feature feature) {
        prefs.remove(feature.getPrefName());
        notifyCallbacks(feature, false);
    }

    public boolean isUnlocked(Feature feature) {
        return prefs.getBoolean(feature.getPrefName(), false) || DebugConfig.getInstance(activity).isAllFeaturesEnabled();
    }

    /**
     * Checks conditions and unlock appropriate features
     * @return last unlocked Feature or null if nothing was unlocked
     */
    public Feature checkAndUnlock() {
        int count = Math.min(calculateNumberOfUnlockedFeatures(), Feature.values().length);
        Feature lastUnlocked = null;
        for (int i = 0; i < count; i++) {
            if (!isUnlocked(Feature.values()[i])) {
                unlock(Feature.values()[i]);
                lastUnlocked = Feature.values()[i];
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

    public void showFeatureAwardDialog(ZazoManagerProvider managers, Feature feature) {
        if (TbmApplication.getInstance().isForeground() && !managers.getPlayer().isPlaying() && !managers.getRecorder().isRecording()) {
            DialogShower.showFeatureAwardDialog(activity, feature);
            prefs.putBoolean(PREF_SHOW_LAST_FEATURE, false);
        } else {
            prefs.putBoolean(PREF_SHOW_LAST_FEATURE, true);
            if (!TbmApplication.getInstance().isForeground() || Convenience.screenIsLockedOrOff(activity)) {
                NotificationAlertManager.alert(activity,
                        activity.getString(R.string.feature_unlock_message),
                        activity.getString(R.string.feature_unlock_discover_message),
                        awardVibrationPattern);
            }
        }
    }

    public boolean showNextFeatureDialog(ZazoManagerProvider managers) {
        if (TbmApplication.getInstance().isForeground() && !isAwardDialogShowed() && !managers.getBenchViewManager().isBenchShowed()) {
            DialogShower.showNextFeatureDialog(activity);
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
