package com.zazoapp.client.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.Config;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.utilities.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
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

    public static void unsubscribe(String nkey, @Nullable HttpRequest.Callbacks callbacks) {
        requestNotificationApi("unsubscribe", nkey, callbacks);
    }

    public static void subscribe(String nkey, @Nullable HttpRequest.Callbacks callbacks) {
        requestNotificationApi("subscribe", nkey, callbacks);
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

    private static String getUrl(String... uri) {
        StringBuilder uriCombined = new StringBuilder();
        for (String s : uri) {
            uriCombined.append("/").append(s);
        }
        return Config.fullUrl(getServerUri(), uriCombined.toString());
    }

    private static String getServerUri() {
        return "http://" + getServerHost();
    }

    private static String getServerHost() {
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
        if (params.getFriendData() != null) {
            return FriendFactory.getFactoryInstance().createWithServerParams(context, params.getFriendData());
        }
        return null;
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
            private String display_name;
            //private String zazo_mkey;
            //private String zazo_id;
            //private int total_score;
            private List<String> phone_numbers;

            public int getId() {
                return id;
            }

            public String getDisplayName() {
                return display_name;
            }
            public List<String> getPhoneNumbers() {
                return phone_numbers;
            }
        }

        private List<Suggestion> data;

        public List<Suggestion> getSuggestions() {
            return data;
        }
    }

    public static class AddResponse {
        private String status;
        private LinkedTreeMap<String, String> data;

        public LinkedTreeMap<String, String> getFriendData() {
            return data;
        }
/*        private static class InviteResponse {
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
        }*/
    }
}
