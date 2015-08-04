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

    public static void getAndPollAllFriends(Context context, ZazoManagerProvider managers) {
        Log.d(TAG, "getAndPollAllFriends");
        new SyncFriendGetter(context, managers).getFriends();
    }

    public static void syncWelcomedFriends(ZazoManagerProvider managers) {
        Log.d(TAG, "syncWelcomedFriends");
        new SyncWelcomedFriends(managers);
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
                syncWelcomedFriends(managers);
            }
        }
    }

    private static void applyWelcomedFriends(boolean sync, @Nullable ZazoManagerProvider managers) {
        if (sync) {
            RemoteStorageHandler.setWelcomedFriends();
        }
        if (managers != null) {
            Features features = managers.getFeatures();
            features.checkAndUnlock();
        }
    }

    private static class SyncWelcomedFriends extends RemoteStorageHandler.GetWelcomedFriends {
        private ZazoManagerProvider managers;

        SyncWelcomedFriends(ZazoManagerProvider m) {
            managers = m;
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
            applyWelcomedFriends(notSynced, managers);
        }

        @Override
        protected void failure() {
            applyWelcomedFriends(false, managers);
        }
    }
}
