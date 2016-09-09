package com.zazoapp.client.ui.helpers;

import android.content.Context;
import android.support.annotation.Nullable;
import com.zazoapp.client.core.FriendGetter;
import com.zazoapp.client.core.RemoteStorageHandler;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.model.ActiveModelsHandler;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.network.aws.S3CredentialsGetter;

import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 7/14/2016.
 */
public class RegistrationHelper {
    Context context;

    public RegistrationHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    private SyncCallbacks syncCallbacks;

    public void sync(SyncCallbacks callbacks, boolean destroyFriends) {
        syncCallbacks = callbacks;
        new RegFriendGetter(context, destroyFriends).getFriends();
    }

    public interface SyncCallbacks {
        void onStartSyncing();
        void onSyncError();
        void onSyncComplete();
    }

    private class RegFriendGetter extends FriendGetter {
        public RegFriendGetter(Context c, boolean destroyAll) {
            super(c, destroyAll);
            if (syncCallbacks != null) {
                syncCallbacks.onStartSyncing();
            }
        }

        @Override
        protected void success() {
            new RegSyncUserSettings();
        }

        @Override
        protected void failure() {
            reportFailure();
        }
    }

    private class RegSyncUserSettings extends RemoteStorageHandler.GetUserSettings {
        private Features features;

        RegSyncUserSettings() {
            features = new Features(context);
        }

        @Override
        protected void failure() {
            reportFailure();
        }

        @Override
        protected void gotUserSettings(@Nullable UserSettings settings) {
            if (settings != null && settings.openedFeatures != null && features != null) {
                for (String openedFeature : settings.openedFeatures) {
                    features.unlockByName(openedFeature);
                }
            }
            new RegSyncWelcomedFriends();
        }
    }

    private class RegSyncWelcomedFriends extends RemoteStorageHandler.GetWelcomedFriends {

        @Override
        protected void gotWelcomedFriends(List<String> mkeys) {
            if (mkeys != null && !mkeys.isEmpty()) {
                List<Friend> friends = FriendFactory.getFactoryInstance().all();
                for (Friend friend : friends) {
                    String mkey = friend.getMkey();
                    if (mkeys.contains(mkey)) {
                        friend.setEverSent(true);
                        mkeys.remove(mkey);
                    } else {
                        friend.setEverSent(false);
                    }
                }
            }
            RemoteStorageHandler.setWelcomedFriends();
            Features features = new Features(context);
            features.checkAndUnlock();
            RemoteStorageHandler.setUserSettings();
            new RegS3CredentialsGetter();
        }

        @Override
        protected void failure() {
            reportFailure();
        }
    }

    private class RegS3CredentialsGetter extends S3CredentialsGetter {
        public RegS3CredentialsGetter() {
            super(context, true);
        }

        @Override
        public void success() {
            User user = UserFactory.current_user();
            if (user != null) {
                user.set(User.Attributes.REGISTERED, "true");
            }
            ActiveModelsHandler.getInstance(context).saveAll();
            if (syncCallbacks != null) {
                syncCallbacks.onSyncComplete();
            }
        }

        @Override
        public void failure() {
            reportFailure();
        }
    }

    private void reportFailure() {
        if (syncCallbacks != null) {
            syncCallbacks.onSyncError();
        }
    }
}
