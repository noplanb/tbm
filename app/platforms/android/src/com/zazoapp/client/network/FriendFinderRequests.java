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
