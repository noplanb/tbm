package com.zazoapp.client.core;

import android.content.Intent;
import com.zazoapp.client.notification.NotificationHandler;

/**
 * Created by skamenkovych@codeminders.com on 7/4/2016.
 */
public enum MessageType {
    VIDEO("video"),
    TEXT("text");

    private String name;

    MessageType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static MessageType getFromIntent(Intent intent) {
        if (intent.hasExtra(NotificationHandler.DataKeys.CONTENT_TYPE)) {
            String contentType = intent.getStringExtra(NotificationHandler.DataKeys.CONTENT_TYPE);
            for (MessageType type : values()) {
                if (type.getName().equals(contentType)) {
                    return type;
                }
            }
        }
        return VIDEO;
    }
}
