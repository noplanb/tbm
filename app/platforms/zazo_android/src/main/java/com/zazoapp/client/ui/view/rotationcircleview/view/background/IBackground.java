package com.zazoapp.client.ui.view.rotationcircleview.view.background;

/**
 * Created by sergii on 13.11.15.
 */
public interface IBackground {

    int getCircleColor( );
    void setCircleColor( int aColor );

    int getRingColor( );
    void setRingColor( int aColor );

    float getRingThickness( );
    void setRingThickness( float aThickness );
}
