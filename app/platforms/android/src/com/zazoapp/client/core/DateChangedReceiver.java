package com.zazoapp.client.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.zazoapp.client.features.friendfinder.ContactsInfoCollector;
import com.zazoapp.client.network.FriendFinderRequests;
import com.zazoapp.client.network.HttpRequest;
import org.json.JSONArray;

/**
 * Created by skamenkovych@codeminders.com on 12/8/2015.
 */
public class DateChangedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final PreferencesHelper prefs = new PreferencesHelper(context);
        final String lastSendKey = "contacts_last_send_time_key";
        long lastTime = Long.parseLong(prefs.getString(lastSendKey, "0"));
        long week = 604800000; // 7 * 24 * 60 * 60 * 1000
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTime > week) {
            ContactsInfoCollector.collectContacts(context.getContentResolver(), new ContactsInfoCollector.ContactsInfoCollectedCallback() {
                @Override
                public void onInfoCollected(final JSONArray contacts) {
                    FriendFinderRequests.sendContacts(contacts, new HttpRequest.Callbacks() {
                        @Override
                        public void success(String response) {
                            prefs.putString(lastSendKey, String.valueOf(System.currentTimeMillis()));
                        }

                        @Override
                        public void error(String errorString) {
                        }
                    });
                }
            });
        }
    }
}
