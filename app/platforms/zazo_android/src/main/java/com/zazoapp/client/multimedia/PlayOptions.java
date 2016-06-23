package com.zazoapp.client.multimedia;

/**
 * Created by skamenkovych@codeminders.com on 23.06.2016.
 */
public class PlayOptions {
    public static final int FULLSCREEN = 0x0001;

    public static boolean isFullscreen(int options) {
        return (options & FULLSCREEN) > 0;
    }
}
