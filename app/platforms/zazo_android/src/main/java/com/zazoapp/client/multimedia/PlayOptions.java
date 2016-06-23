package com.zazoapp.client.multimedia;

/**
 * Created by skamenkovych@codeminders.com on 23.06.2016.
 */
public class PlayOptions {
    public static final int FULLSCREEN = 0x0001;

    public static boolean isFullscreen(@Player.PlayFlags int options) {
        return (options & FULLSCREEN) > 0;
    }

    public static @Player.PlayFlags int clearFlags(@Player.PlayFlags int options, @Player.PlayFlags int flags) {
        options = options & ~flags;
        return options;
    }
}
