package com.zazoapp.client.network;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.zazoapp.client.Config;
import com.zazoapp.client.debug.DebugConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
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

    public static void addFriend(String nkey, @Nullable HttpRequest.Callbacks callbacks) {
        requestNotificationApi("add", nkey, callbacks);
    }

    public static void ignoreFriend(String nkey, @Nullable HttpRequest.Callbacks callbacks) {
        requestNotificationApi("ignore", nkey, callbacks);
    }

    public static void addFriends(@Nullable HttpRequest.Callbacks callbacks, Integer... ids) {
        if (DebugConfig.Bool.USE_CUSTOM_SERVER.get()) {
            testRequest(callbacks);
            return;
        }
        if (ids != null && ids.length > 0) {
            JSONObject object = new JSONObject();
            try {
                List<Integer> list = Arrays.asList(ids);
                object.put("added", list);
                new HttpRequest.Builder()
                        .setUrl(getUrl(CONTACTS_API, "add"))
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
        } else if (callbacks != null) {
            callbacks.error("No contacts added");
        }
    }

    public static void ignoreFriends(@Nullable HttpRequest.Callbacks callbacks, Integer... ids) {
        if (DebugConfig.Bool.USE_CUSTOM_SERVER.get()) {
            testRequest(callbacks);
            return;
        }
        if (ids != null && ids.length > 0) {
            JSONObject object = new JSONObject();
            try {
                List<Integer> list = Arrays.asList(ids);
                object.put("rejected", list);
                new HttpRequest.Builder()
                        .setUrl(getUrl(CONTACTS_API, "reject"))
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
        new HttpRequest.Builder()
                .setUrl(getUrl(NOTIFICATION_API, nkey, action))
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
        return "http://" + getServerHost();
    }

    private static String getServerHost() {
        return DebugConfig.Bool.USE_CUSTOM_SERVER.get() ? STAGING_HOST : PROD_HOST;
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

            public int getId() {
                return id;
            }

            public String getDisplayName() {
                return display_name;
            }
        }

        private List<Suggestion> data;

        public List<Suggestion> getSuggestions() {
            return data;
        }
    }
}
