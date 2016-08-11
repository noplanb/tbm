package com.zazoapp.client.core;

import android.support.annotation.Nullable;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.network.HttpRequest;
import com.zazoapp.client.utilities.StringUtils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.List;

public class RemoteStorageHandler {
    private static final String TAG = RemoteStorageHandler.class.getSimpleName();
    private static final String VIDEO_ID_KV_KEY = "-VideoIdKVKey";
    private static final String VIDEO_STATUS_KV_KEY = "-VideoStatusKVKey";
    private static final String WELCOMED_FRIENDS_KV_KEY = "-WelcomedFriends";
    private static final String USER_SETTINGS_KV_KEY = "-UserSettings";

    //--------------------------------
    // Data structures keys and values
    //--------------------------------
    private static class DataKeys {
        public static final String VIDEO_ID_KEY = "videoId";
        public static final String STATUS_KEY = "status";
        public static final String VALUE_KEY = "value";
        public static final String KEY1_KEY = "key1";
        public static final String KEY2_KEY = "key2";
        public static final String MKEY = "mkey";
        public static final String VIDEO_IDS = "video_ids";
        public static final String STATUS_VIDEO_ID = "video_id";
    }

    public static class StatusEnum {
        public static final String DOWNLOADED = "downloaded";
        public static final String VIEWED = "viewed";
    }

    //=======================================
    // Convenience Public setters and getters
    //=======================================

    //--------
    // Setters
    //--------
    public static void addRemoteOutgoingVideoId(Friend friend, String videoId){
        LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
        data.put(DataKeys.VIDEO_ID_KEY, videoId);
        setRemoteKV(outgoingVideoIdsRemoteKVKey(friend), videoId, data);
    }

    public static void setRemoteIncomingVideoStatus(Friend friend, String videoId, String status){
        LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
        data.put(DataKeys.VIDEO_ID_KEY, videoId);
        data.put(DataKeys.STATUS_KEY, status);
        setRemoteKV(incomingVideoStatusRemoteKVKey(friend), data);
    }

    //--------
    // Getters
    //--------

    public abstract static class GetWelcomedFriends extends GetRemoteKV {

        protected abstract void gotWelcomedFriends(List<String> mkeys);

        public GetWelcomedFriends() {
            super(buildWelcomedFriendsKvKey(), null);
        }

        @Override
        protected void gotRemoteKV(String json) {
            if (json == null) {
                gotWelcomedFriends(null);
                return;
            }
            List<String> list = null;
            Gson gson = new Gson();
            try {
                list = gson.fromJson(json, List.class);
            } catch (JsonSyntaxException e) {
            }
            gotWelcomedFriends(list);
        }

    }

    public static void setWelcomedFriends() {
        List<String> list = new ArrayList<>();
        ArrayList<Friend> friends = FriendFactory.getFactoryInstance().all();
        for (Friend friend : friends) {
            if (friend.everSent()) {
                list.add(friend.getMkey());
            }
        }
        Gson g = new Gson();
        String value = g.toJson(list);
        setRemoteKV(buildWelcomedFriendsKvKey(), null, value);
    }

    public static void deleteWelcomedFriends() {
        deleteRemoteKV(buildWelcomedFriendsKvKey(), null);
        for (Friend friend : FriendFactory.getFactoryInstance().all()) {
            deleteRemoteKV(outgoingVideoStatusRemoteKVKey(friend), null);
        }
    }


    public abstract static class GetUserSettings extends GetRemoteKV {
        public GetUserSettings() {
            super(buildUserSettingsKvKey(), null);
        }

        @Override
        protected void gotRemoteKV(String json) {
            if (json == null) {
                gotUserSettings(null);
                return;
            }
            UserSettings settings = null;
            Gson gson = new Gson();
            try {
                settings = gson.fromJson(json, UserSettings.class);
            } catch (JsonSyntaxException e) {
            }
            gotUserSettings(settings);
        }

        protected abstract void gotUserSettings(@Nullable UserSettings settings);

        protected static class UserSettings {
            public List<String> openedFeatures; // TODO!!!!!!!!!!! userSettings
        }
    }

    public static void setUserSettings() {
        GetUserSettings.UserSettings settings = new GetUserSettings.UserSettings();
        settings.openedFeatures = new ArrayList<>();
        for (Features.Feature feature : Features.Feature.values()) {
            if (feature.isUnlockedPref(TbmApplication.getContext())) {
                settings.openedFeatures.add(feature.name());
            }
        }
        Gson g = new Gson();
        String value = g.toJson(settings);
        setRemoteKV(buildUserSettingsKvKey(), null, value);
    }

    public static void deleteUserSettings() {
        deleteRemoteKV(buildUserSettingsKvKey(), null);
    }
    //-------
    // Delete
    //-------
    public static void deleteRemoteIncomingVideoId(Friend friend, String videoId, HttpRequest.Callbacks callbacks){
        deleteRemoteKV(incomingVideoIdsRemoteKVKey(friend), videoId, callbacks);
    }
	
	//------------------------
	// Keys for remote storage
	//------------------------
	public static String outgoingVideoRemoteFilename(Friend friend, String videoId) {
		return buildOutgoingVideoFilenameKey(friend, videoId);
	}

	public static String incomingVideoRemoteFilename(Friend friend, String videoId) {
		return buildIncomingVideoFilenameKey(friend, videoId);
	}

	private static String outgoingVideoIdsRemoteKVKey(Friend friend) {
		return buildOutgoingKvKey(friend, VIDEO_ID_KV_KEY);
	}

	private static String incomingVideoIdsRemoteKVKey(Friend friend) {
		return buildIncomingKvKey(friend, VIDEO_ID_KV_KEY);
	}

	private static String outgoingVideoStatusRemoteKVKey(Friend friend) {
		return buildOutgoingKvKey(friend, VIDEO_STATUS_KV_KEY);
	}

	private static String incomingVideoStatusRemoteKVKey(Friend friend) {
		return buildIncomingKvKey(friend, VIDEO_STATUS_KV_KEY);
	}

    public static String buildIncomingVideoFilenameKey(Friend friend, String videoId) {
        StringBuilder builder = new StringBuilder();
        builder.append(friend.getMkey()).append("-");
        builder.append(UserFactory.getCurrentUserMkey()).append("-");
        builder.append(md5(friend.get(Friend.Attributes.CKEY) + videoId));
        return builder.toString();
    }

    public static String buildOutgoingVideoFilenameKey(Friend friend, String videoId) {
        StringBuilder builder = new StringBuilder();
        builder.append(UserFactory.getCurrentUserMkey()).append("-");
        builder.append(friend.getMkey()).append("-");
        builder.append(md5(friend.get(Friend.Attributes.CKEY) + videoId));
        return builder.toString();
    }

    public static String buildIncomingKvKey(Friend friend, String suffix) {
        StringBuilder builder = new StringBuilder();
        String sender = friend.getMkey();
        String receiver = UserFactory.getCurrentUserMkey();
        String ckey = friend.get(Friend.Attributes.CKEY);
        builder.append(sender).append("-").append(receiver).append("-");
        builder.append(md5(sender + receiver + ckey));
        builder.append(suffix);
        return builder.toString();
    }

    public static String buildOutgoingKvKey(Friend friend, String suffix) {
        StringBuilder builder = new StringBuilder();
        String sender = UserFactory.getCurrentUserMkey();
        String receiver = friend.getMkey();
        String ckey = friend.get(Friend.Attributes.CKEY);
        builder.append(sender).append("-").append(receiver).append("-");
        builder.append(md5(sender + receiver + ckey));
        builder.append(suffix);
        return builder.toString();
    }

    private static String buildWelcomedFriendsKvKey() {
        return UserFactory.getCurrentUserMkey() + WELCOMED_FRIENDS_KV_KEY;
    }

    private static String buildUserSettingsKvKey() {
        return UserFactory.getCurrentUserMkey() + USER_SETTINGS_KV_KEY;
    }

    //--------------------------
    // Set Get and Delete Remote
    //--------------------------

    // Set key
    private static void setRemoteKV(String key, LinkedTreeMap<String, String> data) {
        String key2 = null;
        setRemoteKV(key, key2, data);
    }

    // Set key1, key2
    private static void setRemoteKV(String key1, String key2, LinkedTreeMap<String, String> data) {
        Gson g = new Gson();
        String value = g.toJson(data, data.getClass());
        setRemoteKV(key1, key2, value);
    }

    private static void setRemoteKV(String key1, String key2, String value) {
        if (key1 == null || value == null) {
            Log.e(TAG, "KVStore key1 or value can't be null");
            return;
        }
        LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
        params.put(DataKeys.KEY1_KEY, key1);
        if (key2 != null)
            params.put(DataKeys.KEY2_KEY, key2);
        params.put(DataKeys.VALUE_KEY, value);
        new SetRemote("kvstore/set", params, HttpRequest.POST);
    }

	private static class SetRemote extends HttpRequest {
		public SetRemote (String uri, LinkedTreeMap<String, String> params, String method){		
			super(uri, params, method, new Callbacks() {
                @Override
                public void success(String response) {
                    LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
                    data.put("success", response);
                }

                @Override
                public void error(String errorString) {
                    Log.e(TAG, "SetRemote: ERROR: " + errorString);
                }
            });
		}
	}

    // Get key1, key2
    private static abstract class GetRemoteKV {

        public GetRemoteKV(String key1, String key2) {
            LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
            params.put(DataKeys.KEY1_KEY, key1);
            if (key2 != null)
                params.put(DataKeys.KEY2_KEY, key2);
            new GetRemoteKVRequest("kvstore/get", params, "GET");
        }

        protected abstract void gotRemoteKV(String json);
        protected abstract void failure();

        private class GetRemoteKVRequest extends HttpRequest {
            public GetRemoteKVRequest(String uri, LinkedTreeMap<String, String> params, String method) {
                super(uri, params, method, new Callbacks() {

                    @SuppressWarnings("unchecked")
                    @Override
                    public void success(String response) {

                        if (response.isEmpty()) {
                            gotRemoteKV(null);
                            return;
                        }

                        LinkedTreeMap<String, String> data = StringUtils.linkedTreeMapWithJson(response);
                        if (data != null) {
                            gotRemoteKV(data.get(DataKeys.VALUE_KEY));
                        } else {
                            gotRemoteKV(null);
                        }
                    }

                    @Override
                    public void error(String errorString) {
                        Log.e(TAG, "GetRemoteKV: " + errorString);
                        failure();
                    }
                });
            }
        }
    }

    //-----------------
    // DeleteRemoteKV
    //-----------------
    private static void deleteRemoteKV(String key1, String key2){
        deleteRemoteKV(key1, key2, null);
    }

    private static void deleteRemoteKV(String key1, String key2, HttpRequest.Callbacks callbacks){
        LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
        params.put(DataKeys.KEY1_KEY, key1);
        if (key2 != null)
            params.put(DataKeys.KEY2_KEY, key2);
        new HttpRequest("kvstore/delete", params, "GET", callbacks);
    }

    private static String md5(String data) {
        return new String(Hex.encodeHex(DigestUtils.md5(data)));
    }
}
