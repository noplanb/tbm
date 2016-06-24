package com.zazoapp.client.multimedia;

/**
 * Created by skamenkovych@codeminders.com on 23.06.2016.
 */
public class PlayOptions {
    public static final int FULLSCREEN = 0x0001;
    public static final int TRANSCRIPT = 0x0002;

    @Player.PlayFlags private int options;
    public PlayOptions(@Player.PlayFlags int options) {
        this.options = options;
    }

    public boolean hasFlags(@Player.PlayFlags int flags) {
        return (options & flags) > 0;
    }

    public void clearFlags(@Player.PlayFlags int flags) {
        options = options & ~flags;
    }
}
