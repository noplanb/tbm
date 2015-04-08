package com.zazoapp.client.dispatch;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.TbmApplication;
import com.zazoapp.client.network.HttpRequest;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Iterator;

/**
 * Created by skamenkovych@codeminders.com on 4/7/2015.
 */
public class TbmTracker implements ErrorTracker {

    private static final String TAG = TbmTracker.class.getSimpleName();
    private static final String DISPATCH_PREFIX = "tbmDispatch_";

    private Context context;
    private boolean includeLogcat;

    @Override
    public void init(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void trackThrowable(Throwable throwable) {
        trackThrowable(throwable, ErrorLevel.ERROR);
    }

    @Override
    public void trackThrowable(Throwable throwable, ErrorLevel level) {
        if (!isInit()) {
            Log.e(TAG, "tracker is not inited");
            return;
        }
        if (throwable == null) {
            Log.e(TAG, "trackThrowable: nothing to track");
            return;
        }
        if (level == null) {
            level = ErrorLevel.ERROR;
        }
        JSONObject data = new JSONObject();
        try {
            data.put("level", level.toString());
            StringBuilder log = new StringBuilder(throwable.toString());
            for (StackTraceElement el : throwable.getStackTrace()) {
                log.append(el.toString());
            }
            data.put("throwable", log.toString());
            StringBuilder msgBuilder = new StringBuilder();
            msgBuilder.append(level.toString()).append(" in v").append(TbmApplication.getVersion());
            msgBuilder.append(throwable.toString()).append("\n").append(log.toString());
            if (includeLogcat) {
                msgBuilder.append("\n").append(LogCatCollector.collectLogCat(null));
            }
            data.put("msg", msgBuilder.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        trackData(data);
    }

    @Override
    public void trackMessage(String message) {
        trackMessage(message, ErrorLevel.DEBUG);
    }

    @Override
    public void trackMessage(String message, ErrorLevel level) {
        if (!isInit()) {
            Log.e(TAG, "tracker is not inited");
            return;
        }
        if (message == null) {
            Log.e(TAG, "trackMessage: nothing to track");
        }
        if (level == null) {
            level = ErrorLevel.DEBUG;
        }
        JSONObject data = new JSONObject();
        try {
            StringBuilder msgBuilder = new StringBuilder();
            msgBuilder.append(level.toString()).append(" in v").append(TbmApplication.getVersion());
            msgBuilder.append(": ").append(message);
            if (includeLogcat) {
                msgBuilder.append("\n").append(LogCatCollector.collectLogCat(null));
            }
            data.put("msg", msgBuilder.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        trackData(data);
    }

    @Override
    public void trackData(JSONObject data) {
        if (!isInit()) {
            Log.e(TAG, "tracker is not inited");
            return;
        }
        String path = buildNewPath();
        writeToFile(data, path);
        trackDataInner(data, path);
    }

    @Override
    public void trackStored() {
        if (!isInit()) {
            Log.e(TAG, "tracker is not inited");
            return;
        }
        String[] list = getStoredFileNames();
        for (String name : list) {
            String path = context.getCacheDir() + File.separator + name;
            JSONObject data = readFromFile(path);
            if (data == null) {
                new File(path).delete();
                continue;
            }
            trackDataInner(data, path);
        }
    }

    @Override
    public void setIncludeLogcat(boolean includeLogcat) {
        this.includeLogcat = includeLogcat;
    }

    private void trackDataInner(JSONObject data, String storedPath) {
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
        String uri = new Uri.Builder().appendPath("dispatch").appendPath("post_dispatch").build().toString();
        new DispatchPost(uri, params, storedPath);
    }

    private void writeToFile(JSONObject data, String path) {
        String j = data.toString();
        File f = new File(path);
        if (f.exists())
            f.delete();
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;
        try {
            fos = new FileOutputStream(f, true);
            osw = new OutputStreamWriter(fos);
            osw.write(j);
        } catch (IOException e) {
        } finally {
            if (osw != null) {
                try {
                    osw.flush();
                    osw.close();
                } catch (IOException e) {
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private JSONObject readFromFile(String storedPath) {
        File f = new File(storedPath);
        if (!f.exists())
            return null;
        String json = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            String s = "";
            StringBuilder sb = new StringBuilder();
            while ( (s = br.readLine()) != null){
                sb.append(s);
            }
            json = sb.toString();
        } catch (IOException e) {
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        }
        if (json == null)
            return null;
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            return null;
        }
    }

    private String buildNewPath() {
        StringBuilder path = new StringBuilder(context.getCacheDir().getPath());
        path.append(File.separator).append(DISPATCH_PREFIX).append(System.currentTimeMillis());
        return path.toString();
    }

    private String[] getStoredFileNames() {
        return context.getCacheDir().list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(DISPATCH_PREFIX);
            }
        });
    }

    private boolean isInit() {
        return context != null;
    }

    private static class DispatchPost extends HttpRequest {

        public DispatchPost(String uri, final LinkedTreeMap<String, String> params, final String storedPath) {
            super(uri, params, "POST", new Callbacks() {

                @Override
                public void success(String response_string) {
                    Log.i(TAG, "DispatchPost " + response_string);
                    Gson g = new Gson();
                    Response r = g.fromJson(response_string, Response.class);
                    if (r.getStatus().equals("success")) {
                        File f = new File(storedPath);
                        f.delete();
                    }
                }

                @Override
                public void error(String errorString) {
                }
            });
        }
    }

    private static class Response{
        private String status;

        public String getStatus() {
            return status;
        }
    }
}
