package com.zazoapp.client.network;

import android.support.annotation.Nullable;
import com.zazoapp.client.Config;
import com.zazoapp.client.debug.DebugConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by skamenkovych@codeminders.com on 12/7/2015.
 */
public class FriendFinderRequests {
    private static final String PROD_HOST = "ff.zazoapp.com";
    private static final String STAGING_HOST = "ff-staging.zazoapp.com";

    private static final String NOTIFICATION_API = "api/v1/notifications";

    public static void sendContacts(JSONArray contacts, @Nullable HttpRequest.Callbacks callbacks) {
        JSONObject object = new JSONObject();
        try {
            object.put("contacts", contacts);
            new HttpRequest.Builder()
                    .setUrl(getUrl("api/v1/contacts"))
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

    public static void unsubscribe(String nkey, @Nullable HttpRequest.Callbacks callbacks) {
        requestNotificationApi("unsubscribe", nkey, callbacks);
    }

    public static void subscribe(String nkey, @Nullable HttpRequest.Callbacks callbacks) {
        requestNotificationApi("subscribe", nkey, callbacks);
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
        return "https://" + getServerHost();
    }

    private static String getServerHost() {
        return DebugConfig.getInstance().shouldUseCustomServer() ? STAGING_HOST : PROD_HOST;
    }
}
