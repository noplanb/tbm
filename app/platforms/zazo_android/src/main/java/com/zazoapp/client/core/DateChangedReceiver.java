package com.zazoapp.client.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.zazoapp.client.features.friendfinder.ContactsInfoCollector;

/**
 * Created by skamenkovych@codeminders.com on 12/8/2015.
 */
public class DateChangedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ContactsInfoCollector.checkAndSend(context);
    }
}
