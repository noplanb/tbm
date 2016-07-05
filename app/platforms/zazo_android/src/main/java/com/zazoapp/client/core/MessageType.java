package com.zazoapp.client.core;

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
}
