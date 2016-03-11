package com.zazoapp.client.core;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.ui.ZazoManagerProvider;

import java.util.List;

public class SyncManager {
    private static final String TAG = SyncManager.class.getSimpleName();
    private SyncManager(){
    }

    /**
     * Loads friends from server and sync status data
     * @param context context
     * @param managers if not null will try to unlock features silently on sync finish
     */
    public static void getAndPollAllFriends(Context context, @Nullable ZazoManagerProvider managers) {
        Log.d(TAG, "getAndPollAllFriends");
        new SyncFriendGetter(context, managers).getFriends();
    }

    /**
     * Sync welcomed friends with server
     * @param managers if not null will try to unlock features silently on sync finish
     */
    public static void syncWelcomedFriends(@Nullable ZazoManagerProvider managers) {
        new SyncUserSettings(managers);
        new SyncWelcomedFriends(managers, true);
    }

    private static class SyncFriendGetter extends FriendGetter {
        private boolean isJustUpgraded;
        private ZazoManagerProvider managers;
        public SyncFriendGetter(Context c, ZazoManagerProvider m) {
            super(c, false);
            managers = m;
            isJustUpgraded = UserFactory.current_user().inviteeIsNotSet();
        }
        
        @Override
        protected void success() {
            doNext();
        }

        @Override
        protected void failure() {
            doNext();
        }

        private void doNext() {
            new Poller().pollAll(); // all requests will be done sequentially due to serial executor
            if (isJustUpgraded) {
                applyWelcomedFriends(true, managers);
            } else {
                new SyncUserSettings(managers);
                new SyncWelcomedFriends(managers, false);
            }
        }
    }

    /**
     * Applies welcomed friends depending on parameters
     *
     * @param sync flag means whether welcomed friends will be posted on server
     * @param managers if not null will try to unlock features silently
     */
    private static void applyWelcomedFriends(boolean sync, @Nullable ZazoManagerProvider managers) {
        if (sync) {
            RemoteStorageHandler.setWelcomedFriends();
        }
        if (managers != null) {
            Features features = managers.getFeatures();
            features.checkAndUnlock();
        }
        RemoteStorageHandler.setUserSettings();
    }

    private static class SyncWelcomedFriends extends RemoteStorageHandler.GetWelcomedFriends {
        private ZazoManagerProvider managers;
        private boolean forceSync;

        SyncWelcomedFriends(ZazoManagerProvider m, boolean force) {
            Log.d(TAG, "syncWelcomedFriends");
            managers = m;
            forceSync = force;
        }

        @Override
        protected void gotWelcomedFriends(List<String> mkeys) {
            boolean notSynced = false;
            if (mkeys != null && !mkeys.isEmpty()) {
                List<Friend> friends = FriendFactory.getFactoryInstance().all();
                for (Friend friend : friends) {
                    String mkey = friend.getMkey();
                    if (mkeys.contains(mkey)) {
                        friend.setEverSent(true);
                        mkeys.remove(mkey);
                    } else if (!friend.everSent()) {
                        friend.setEverSent(false);
                    } else {
                        notSynced = true;
                    }
                }
            }
            applyWelcomedFriends(notSynced || forceSync, managers);
        }

        @Override
        protected void failure() {
            applyWelcomedFriends(false, managers);
        }
    }

    private static class SyncUserSettings extends RemoteStorageHandler.GetUserSettings {
        private ZazoManagerProvider managers;

        SyncUserSettings(ZazoManagerProvider m) {
            Log.d(TAG, "syncUserSettings");
            managers = m;
        }

        @Override
        protected void failure() {
        }

        @Override
        protected void gotUserSettings(@Nullable UserSettings settings) {
            if (settings == null || settings.openedFeatures == null) {
                return;
            }
            if (managers != null) {
                for (String openedFeature : settings.openedFeatures) {
                    managers.getFeatures().unlockByName(openedFeature);
                }
            }
        }
    }
}
