package com.zazoapp.client.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.Config;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.utilities.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by skamenkovych@codeminders.com on 12/7/2015.
 */
public class FriendFinderRequests {
    private static final String PROD_HOST = "ff.zazoapp.com";
    private static final String STAGING_HOST = "ff-staging.zazoapp.com";

    private static final String NOTIFICATION_API = "api/v1/notifications";
    private static final String CONTACTS_API = "api/v1/contacts";
    private static final String SUGGESTIONS_API = "api/v1/suggestions";
    private static final String SUBSCRIPTIONS_API = "api/v1/subscriptions";

    public static void sendContacts(JSONArray contacts, @Nullable HttpRequest.Callbacks callbacks) {
        JSONObject object = new JSONObject();
        try {
            object.put("contacts", contacts);
            new HttpRequest.Builder()
                    .setUrl(getUrl(CONTACTS_API))
                    .setHost(getServerHost())
                    .setMethod(HttpRequest.POST)
                    .setJsonParams(object)
                    .setCallbacks(callbacks)
                    .build();
        } catch (JSONException e) {
            if (callbacks != null) {
                callbacks.error("Json exception");
            }
        }
    }

    public static void addFriend(String nkey, @Nullable HttpRequest.Callbacks callbacks, String phoneNumber) {
        JSONObject params = null;
        if (phoneNumber != null) {
            params = new JSONObject();
            try {
                params.put("phone_number", phoneNumber);
            } catch (JSONException e) {
                if (callbacks != null) {
                    callbacks.error("Json exception");
                }
            }
        }
        requestNotificationApi("add", nkey, callbacks, params);
    }

    public static void ignoreFriend(String nkey, @Nullable HttpRequest.Callbacks callbacks) {
        requestNotificationApi("ignore", nkey, callbacks);
    }

    public static void addFriend(@Nullable HttpRequest.Callbacks callbacks, int id, String phoneNumber) {
        if (DebugConfig.Bool.IMITATE_REQUESTS.get()) {
            testRequest(callbacks);
            return;
        }
        if (id >= 0) {
            JSONObject object = new JSONObject();
            try {
                HttpRequest.Builder requestBuilder = new HttpRequest.Builder()
                        .setUrl(getUrl(CONTACTS_API, String.valueOf(id), "add"))
                        .setHost(getServerHost())
                        .setMethod(HttpRequest.POST)
                        .setCallbacks(callbacks);
                if (phoneNumber != null) {
                    object.put("phone_number", phoneNumber);
                    requestBuilder.setJsonParams(object);
                }
                requestBuilder.build();
            } catch (JSONException e) {
                if (callbacks != null) {
                    callbacks.error("Json exception");
                }
            }
        } else if (callbacks != null) {
            callbacks.error("No contacts added");
        }
    }

    public static void ignoreFriend(@Nullable HttpRequest.Callbacks callbacks, int id) {
        if (DebugConfig.Bool.IMITATE_REQUESTS.get()) {
            testRequest(callbacks);
            return;
        }
        if (id >= 0) {
            new HttpRequest.Builder()
                    .setUrl(getUrl(CONTACTS_API, String.valueOf(id), "ignore"))
                    .setHost(getServerHost())
                    .setMethod(HttpRequest.POST)
                    .setCallbacks(callbacks)
                    .build();
        } else if (callbacks != null) {
            callbacks.error("No contacts added");
        }
    }

    public static void unsubscribe(@Nullable HttpRequest.Callbacks callbacks) {
        requestSubscriptionsApi("unsubscribe", callbacks);
    }

    public static void subscribe(@Nullable HttpRequest.Callbacks callbacks) {
        requestSubscriptionsApi("subscribe", callbacks);
    }

    public static void getSubscriptionsState(@NonNull SubscriptionsStateCallback callback) {
        new GetSubscriptionsState(callback);
    }

    public static void getSuggestions(SuggestionsCallback callback) {
        new GetSuggestions(callback);
    }

    private static final Random testRandom = new Random();
    public static void testRequest(@NonNull final HttpRequest.Callbacks callbacks) {
        final int value = testRandom.nextInt(4);
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (value == 0) {
                    callbacks.error("test error");
                } else {
                    callbacks.success("test success");
                }
            }
        }, 1000);
    }

    private static void requestNotificationApi(String action, String nkey, @Nullable HttpRequest.Callbacks callbacks) {
        requestNotificationApi(action, nkey, callbacks, null);
    }

    private static void requestNotificationApi(String action, String nkey, @Nullable HttpRequest.Callbacks callbacks, @Nullable JSONObject params) {
        if (DebugConfig.Bool.IMITATE_REQUESTS.get()) {
            testRequest(callbacks);
            return;
        }
        HttpRequest.Builder requestBuilder = new HttpRequest.Builder()
                .setUrl(getUrl(NOTIFICATION_API, nkey, action))
                .setHost(getServerHost())
                .setMethod(HttpRequest.POST)
                .setCallbacks(callbacks);
        if (params != null) {
            requestBuilder.setJsonParams(params);
        }
        requestBuilder.build();
    }

    private static void requestSubscriptionsApi(String action, @Nullable HttpRequest.Callbacks callbacks) {
        if (DebugConfig.Bool.IMITATE_REQUESTS.get()) {
            testRequest(callbacks);
            return;
        }
        new HttpRequest.Builder()
                .setUrl(getUrl(SUBSCRIPTIONS_API, action))
                .setHost(getServerHost())
                .setMethod(HttpRequest.POST)
                .setCallbacks(callbacks)
                .build();
    }

    private static String getUrl(String... uri) {
        StringBuilder uriCombined = new StringBuilder();
        for (String s : uri) {
            uriCombined.append("/").append(s);
        }
        return Config.fullUrl(getServerUri(), uriCombined.toString());
    }

    private static String getServerUri() {
        return "https://" + getServerHost();
    }

    public static String getServerHost() {
        return DebugConfig.Bool.USE_CUSTOM_SERVER.get() ? STAGING_HOST : PROD_HOST;
    }

    /**
     *
     * @param context
     * @param response response string of add request
     * @return friend if he was added, otherwise {@code null}
     */
    @Nullable public static Friend gotFriend(Context context, String response) {
        AddResponse params = StringUtils.fromJson(response, AddResponse.class);
        if (params != null && params.getFriendData() != null) {
            return FriendFactory.getFactoryInstance().createWithServerParams(context, params.getFriendData(), true);
        }
        return null;
    }

    public interface SubscriptionsStateCallback {
        void onReceivedSubscriptionsState(SubscriptionState state);
    }

    public enum SubscriptionState {
        SUBSCRIBED("subscribed"),
        UNSUBSCRIBED("unsubscribed"),
        ERROR("");

        private String data;
        SubscriptionState(String s) {
            data = s;
        }

        static SubscriptionState gotState(String status) {
            for (SubscriptionState state : values()) {
                if (state.data.equals(status)) {
                    return state;
                }
            }
            return ERROR;
        }
    }

    private static class GetSubscriptionsState {
        GetSubscriptionsState(final SubscriptionsStateCallback callback) {
            new HttpRequest.Builder()
                    .setUrl(getUrl(SUBSCRIPTIONS_API))
                    .setHost(getServerHost())
                    .setCallbacks(new HttpRequest.Callbacks() {
                        @Override
                        public void success(String response) {
                            if (TextUtils.isEmpty(response)) {
                                error(null);
                            }
                            Gson gson = new Gson();
                            try {
                                callback.onReceivedSubscriptionsState(SubscriptionState.gotState(gson.fromJson(response, SubscriptionStateData.class).getState()));
                            } catch (JsonSyntaxException e) {
                                error(e.getMessage());
                            }
                        }

                        @Override
                        public void error(String errorString) {
                            callback.onReceivedSubscriptionsState(SubscriptionState.ERROR);
                        }
                    }).build();
        }
    }

    private static class SubscriptionStateData {
        private Map<String, Object> data;

        String getState() {
            if (data != null && data.containsKey("status")) {
                return (String) data.get("status");
            }
            return "";
        }
    }

    public interface SuggestionsCallback {
        void onReceivedSuggestions(SuggestionsData data);
    }

    private static class GetSuggestions {
        GetSuggestions(final SuggestionsCallback callback) {
            new HttpRequest.Builder()
                    .setUrl(getUrl(SUGGESTIONS_API))
                    .setHost(getServerHost())
                    .setCallbacks(new HttpRequest.Callbacks() {
                        @Override
                        public void success(String response) {
                            if (TextUtils.isEmpty(response)) {
                                callback.onReceivedSuggestions(null);
                            }
                            Gson gson = new Gson();
                            try {
                                callback.onReceivedSuggestions(gson.fromJson(response, SuggestionsData.class));
                            } catch (JsonSyntaxException e) {
                                callback.onReceivedSuggestions(null);
                            }
                        }

                        @Override
                        public void error(String errorString) {
                            callback.onReceivedSuggestions(null);
                        }
                    }).build();
        }
    }

    public static class SuggestionsData {
        public class Suggestion {
            private int id;
            //private String first_name;
            //private String last_name;
            @SerializedName("display_name")
            private String displayName;
            //private String zazo_mkey;
            //private String zazo_id;
            //private int total_score;
            @SerializedName("phone_numbers")
            private List<String> phoneNumbers;

            public int getId() {
                return id;
            }

            public String getDisplayName() {
                return displayName;
            }
            public List<String> getPhoneNumbers() {
                return phoneNumbers;
            }
        }

        private List<Suggestion> data;

        public List<Suggestion> getSuggestions() {
            return data;
        }
    }

    public static class AddResponse {
        private String status;
        private InviteResponse data;

        public LinkedTreeMap<String, String> getFriendData() {
            return data != null ? data.getFriendData() : null;
        }

        private static class InviteResponse {
            String id;
            String mkey;
            String first_name;
            String last_name;
            String mobile_number;
            String device_platform;
            ArrayList<String> emails;
            String has_app;
            String ckey;
            String cid;
            String connection_created_on;
            String connection_creator_mkey;
            String connection_status;
            String status;

            LinkedTreeMap<String, String> getFriendData() {
                LinkedTreeMap<String, String> data = new LinkedTreeMap<>();
                data.put(FriendFactory.ServerParamKeys.ID, id);
                data.put(FriendFactory.ServerParamKeys.MKEY, mkey);
                data.put(FriendFactory.ServerParamKeys.FIRST_NAME, first_name);
                data.put(FriendFactory.ServerParamKeys.LAST_NAME, last_name);
                data.put(FriendFactory.ServerParamKeys.MOBILE_NUMBER, mobile_number);
                data.put(FriendFactory.ServerParamKeys.HAS_APP, has_app);
                data.put(FriendFactory.ServerParamKeys.CKEY, ckey);
                data.put(FriendFactory.ServerParamKeys.CID, cid);
                data.put(FriendFactory.ServerParamKeys.CONNECTION_CREATED_ON, connection_created_on);
                data.put(FriendFactory.ServerParamKeys.CONNECTION_CREATOR_MKEY, connection_creator_mkey);
                data.put(FriendFactory.ServerParamKeys.CONNECTION_STATUS, connection_status);
                return data;
            }
        }
    }
}
