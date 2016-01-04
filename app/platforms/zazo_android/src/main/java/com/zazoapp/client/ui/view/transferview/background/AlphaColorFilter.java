package com.zazoapp.client.ui.view.transferview.background;

import android.graphics.ColorMatrixColorFilter;
import android.support.annotation.IntRange;

import java.util.Arrays;

/**
 * Created by skamenkovych@codeminders.com on 1/4/2016.
 */
class AlphaColorFilter extends ColorMatrixColorFilter {

    private static final float[] ALPHA_MATRIX = {1, 0, 0, 0, 0, 0, 1, 0, 0, 0,
            0, 0, 1, 0, 0, 0, 0, 0, 1/*18: alpha*/, 0};

    public AlphaColorFilter(float[] array) {
        super(array);
    }

    public static AlphaColorFilter forAlpha(@IntRange(from=0, to=255) int alpha) {
        float[] array = Arrays.copyOf(ALPHA_MATRIX, ALPHA_MATRIX.length);
        array[18] = alpha / 255f;
        return new AlphaColorFilter(array);
    }
}
