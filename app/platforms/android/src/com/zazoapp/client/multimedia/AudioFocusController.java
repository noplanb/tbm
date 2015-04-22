package com.zazoapp.client.multimedia;

/**
 * Created by skamenkovych@codeminders.com on 4/21/2015.
 */
public interface AudioFocusController {
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
}
