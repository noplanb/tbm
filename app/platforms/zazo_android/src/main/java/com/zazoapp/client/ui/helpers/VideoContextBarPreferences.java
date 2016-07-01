package com.zazoapp.client.ui.helpers;

/**
 * Created by skamenkovych@codeminders.com on 7/1/2016.
 */
public class VideoContextBarPreferences {
    public final boolean hasDivider;
    public final boolean showCloseButton;
    public final boolean showMuteButton;

    public VideoContextBarPreferences(boolean hasDivider, boolean showCloseButton, boolean showMuteButton) {
        this.hasDivider = hasDivider;
        this.showCloseButton = showCloseButton;
        this.showMuteButton = showMuteButton;
    }
}
