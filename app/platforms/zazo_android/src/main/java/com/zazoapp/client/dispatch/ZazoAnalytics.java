package com.zazoapp.client.dispatch;

import android.app.Activity;
import android.content.Context;
import com.appsflyer.AppsFlyerLib;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.UserFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by skamenkovych@codeminders.com on 12/11/2015.
 */
public class ZazoAnalytics {
    private static Context applicationContext;

    public static void init(Context context) {
        applicationContext = context.getApplicationContext();
    }

    public static void trackEvent(String eventName, Map<String, Object> eventValues) {
        if (isEnabled()) {
            AppsFlyerLib.trackEvent(applicationContext, eventName, eventValues);
        }
    }

    public static void trackEvent(String eventName) {
        if (isEnabled()) {
            AppsFlyerLib.trackEvent(applicationContext, eventName, getCommonEventData());
        }
    }

    public static void start() {
        if (isEnabled()) {
            AppsFlyerLib.setAppsFlyerKey("3QCqTEK3UQKaZyXbK8bCcC");
            AppsFlyerLib.sendTracking(applicationContext);
        }
    }

    public static void setUser() {
        if (isEnabled()) {
            User user = UserFactory.current_user();
            if (user != null && AppsFlyerLib.getAppUserId() == null) {
                AppsFlyerLib.setCustomerUserId(user.getId());
            }
        }
    }

    public static void onActivityResume(Activity activity) {
        if (isEnabled()) {
            AppsFlyerLib.onActivityResume(activity);
        }
    }

    public static void onActivityPause(Activity activity) {
        if (isEnabled()) {
            AppsFlyerLib.onActivityPause(activity);
        }
    }

    public static Map<String, Object> getCommonEventData() {
        Map<String, Object> eventValue = new HashMap<>();
        User user = UserFactory.current_user();
        if (user != null) {
            eventValue.put("FirstName", user.getFirstName());
            eventValue.put("LastName", user.getLastName());
            eventValue.put("IdTbm", user.getId());
            ArrayList<Friend> friends = FriendFactory.getFactoryInstance().all();
            int everSentFriendsCount = 0;
            for (Friend friend : friends) {
                if (friend.everSent()) {
                    everSentFriendsCount++;
                }
            }
            eventValue.put("NumZazoedFriends", everSentFriendsCount);
        }
        return eventValue;
    }

    private static boolean isEnabled() {
        return !DebugConfig.Bool.USE_CUSTOM_SERVER.get();
    }
}

