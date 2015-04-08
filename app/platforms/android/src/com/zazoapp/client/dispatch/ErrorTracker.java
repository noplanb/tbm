package com.zazoapp.client.dispatch;

import android.content.Context;
import org.json.JSONObject;

/**
 * Created by skamenkovych@codeminders.com on 4/7/2015.
 */
public interface ErrorTracker {
    void init(Context context);
    void trackThrowable(Throwable throwable);
    void trackThrowable(Throwable throwable, ErrorLevel level);
    void trackMessage(String message);
    void trackMessage(String message, ErrorLevel level);
    void trackData(JSONObject data);
    void trackStored();
    void setIncludeLogcat(boolean includeLogcat);
}
