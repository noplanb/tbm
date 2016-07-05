package com.zazoapp.client.model;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Created by skamenkovych@codeminders.com on 7/4/2016.
 */
public class Transcription {
    public String text = "";
    public String asr = "";
    public String lang = "";
    public String state = State.NONE;
    public String rate = "";

    private static final Transcription EMPTY = new Transcription();

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static Transcription fromJson(String json) {
        Gson gson = new Gson();
        try {
            Transcription t = gson.fromJson(json, Transcription.class);
            if (t == null || t.state == null)
                return EMPTY;
            else
                return t;
        } catch (JsonSyntaxException e) {
            return EMPTY;
        }
    }

    public static class State {
        public static final String NONE = "-";
        public static final String OK = "ok";
        public static final String FAILED = "e";
    }
}
