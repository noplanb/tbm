package com.zazoapp.client.ui.animations;

import android.graphics.ColorMatrixColorFilter;
import android.support.annotation.IntRange;

/**
 * Created by skamenkovych@codeminders.com on 1/4/2016.
 */
public class AlphaColorFilter extends ColorMatrixColorFilter {

    private AlphaColorFilter(float[] array) {
        super(array);
    }

    public static AlphaColorFilter forAlpha(@IntRange(from=0, to=255) int alpha) {
        return new AlphaColorFilter(new float[]{1, 0, 0, 0, 0, 0, 1, 0, 0, 0,
                0, 0, 1, 0, 0, 0, 0, 0, alpha / 255f, 0});
    }
}
