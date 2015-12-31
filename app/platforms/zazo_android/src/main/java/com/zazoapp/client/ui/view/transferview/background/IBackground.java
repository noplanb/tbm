package com.zazoapp.client.ui.view.transferview.background;

/**
 * Created by sergii on 14.11.15.
 */
public interface IBackground {

    int getCircleColor();
    void setCircleColor(int aColor);

    int getFirstRingColor();
    void setFirstRingColor(int aColor);

    int getSecondRingColor();
    void setSecondRingColor(int bgRingColorSecond);

    float getRingThickness();
    void setRingThickness(float aThickness);



}
