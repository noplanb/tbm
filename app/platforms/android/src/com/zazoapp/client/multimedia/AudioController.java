package com.zazoapp.client.multimedia;

/**
 * Created by skamenkovych@codeminders.com on 4/21/2015.
 */
public interface AudioController {
    /**
     * Request audio focus
     * @return true if audio focus has been granted
     */
    boolean gainFocus();

    /**
     * Abandon audio focus. May not be successful
     */
    void abandonFocus();

    /**
     *
     * @return true if it is has gained audio focus.
     */
    boolean hasFocus();

    /**
     *
     * @return true if audio system currently playing not through phone earpiece
     */
    boolean isSpeakerPhoneOn();

    /**
     * True to make audio system playing not through phone earpiece.
     * False to play through phone earpiece
     */
    void setSpeakerPhoneOn(boolean on);

    /**
     * Should be called to reset audio settings
     */
    void reset();
}
