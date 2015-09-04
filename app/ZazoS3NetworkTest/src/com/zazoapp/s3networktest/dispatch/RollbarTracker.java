package com.zazoapp.s3networktest.dispatch;

import android.content.Context;
import android.util.Log;
import com.google.gson.internal.LinkedTreeMap;
import com.rollbar.android.Rollbar;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by skamenkovych@codeminders.com on 4/8/2015.
 */
public class RollbarTracker implements ErrorTracker {
    private static final String TAG = RollbarTracker.class.getSimpleName();
    private Context context;
    @Override
    public void init(Context context) {
        this.context = context;
        Rollbar.init(context, "1fc7c2e85dfe4c9aa194d6f8e1e88a81", "networkTest", true);
    }

    @Override
    public void trackThrowable(Throwable throwable) {
        trackThrowable(throwable, ErrorLevel.ERROR);
    }

    @Override
    public void trackThrowable(Throwable throwable, ErrorLevel level) {
        if (throwable == null) {
            Log.e(TAG, "trackThrowable: nothing to track");
            return;
        }
        if (level == null) {
            level = ErrorLevel.ERROR;
        }
        setUpUser();
        Rollbar.reportException(throwable, level.name().toLowerCase());
    }

    @Override
    public void trackMessage(String message) {
        trackMessage(message, ErrorLevel.DEBUG);
    }

    @Override
    public void trackMessage(String message, ErrorLevel level) {
        if (message == null) {
            Log.e(TAG, "trackMessage: nothing to track");
        }
        if (level == null) {
            level = ErrorLevel.DEBUG;
        }
        setUpUser();
        Rollbar.reportMessage(message, level.name().toLowerCase());
    }

    @Override
    public void trackData(JSONObject data) {
        LinkedTreeMap<String, String> params = new LinkedTreeMap<>();
        Iterator<String> keys = data.keys();
        try {
            while (keys.hasNext()) {
                String key = keys.next();
                params.put(key, data.getString(key));
            }
        } catch (JSONException e) {
            return;
        }
        setUpUser();
        Rollbar.reportMessage("data", ErrorLevel.INFO.name(), params);
    }

    @Override
    public void trackStored() {
        // tracked automatically on init
    }

    @Override
    public void setIncludeLogcat(boolean includeLogcat) {
        Rollbar.setIncludeLogcat(includeLogcat);
    }

    private void setUpUser() {
        Rollbar.setPersonData("1", "Sani S3 test", "");
        JSONObject person = new JSONObject();
        try {
            person.put("id", "1");
            person.put("username", "Sani S3 test");
            person.put("user_data", UserInfoCollector.collect(context));
            Rollbar.setPersonData(person);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
