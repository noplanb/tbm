package com.zazoapp.client.dispatch;

import android.content.Context;
import android.util.Log;
import com.google.gson.internal.LinkedTreeMap;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.rollbar.android.Rollbar;
import com.zazoapp.client.BuildConfig;
import com.zazoapp.client.Config;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.UserFactory;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by skamenkovych@codeminders.com on 4/8/2015.
 */
public class RollbarTracker implements ErrorTracker {
    private static final String TAG = RollbarTracker.class.getSimpleName();

    @Override
    public void init(Context context) {
        Rollbar.init(context, "1fc7c2e85dfe4c9aa194d6f8e1e88a81", BuildConfig.DISPATCH_ENVIRONMENT, false);
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
        String messageTag = "data";
        try {
            while (keys.hasNext()) {
                String key = keys.next();
                if (Dispatch.MESSAGE_TAG.equals(key)) {
                    messageTag = data.getString(key);
                } else {
                    params.put(key, data.getString(key));
                }
            }
        } catch (JSONException e) {
            return;
        }
        setUpUser();
        Rollbar.reportMessage(messageTag, ErrorLevel.INFO.name(), params);
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
        User user = UserFactory.current_user();
        if (user != null && !user.getId().isEmpty()) {
            Phonenumber.PhoneNumber phone = user.getPhoneNumberObj();
            String phoneNumber = PhoneNumberUtil.getInstance().format(phone, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
            Rollbar.setPersonData(user.getId(), user.getFullName(), phoneNumber);
            JSONObject person = new JSONObject();
            try {
                person.put("id", user.getId());
                person.put("username", user.getFullName());
                person.put("phone_number", phoneNumber);
                person.put("host", Config.getServerHost());
                person.put("user_data", UserInfoCollector.collect(TbmApplication.getInstance()));
                Rollbar.setPersonData(person);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Rollbar.setPersonData("", "", "");
        }
    }
}
