package com.zazoapp.client.debug;

import android.content.Context;
import android.content.res.AssetManager;
import com.zazoapp.client.Config;
import com.zazoapp.client.utilities.Convenience;

import java.io.File;

/**
 * Created by skamenkovych@codeminders.com on 3/24/2015.
 */
public final class DebugUtils {
    private DebugUtils() {}

    private static final String BROKEN_VIDEO_NAME = "test_video_broken.mp4";

    public static File getBrokenVideoFile(Context context) {
        AssetManager am = context.getAssets();
        String path = Config.homeDirPath(context) + BROKEN_VIDEO_NAME;
        try {
            return Convenience.createFileFromAssets(am, path, BROKEN_VIDEO_NAME);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
