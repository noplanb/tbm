package com.zazoapp.client.ui.helpers;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import com.zazoapp.client.R;
import com.zazoapp.client.ui.view.ThumbView;
import com.zazoapp.client.utilities.Convenience;

/**
 * Created by skamenkovych@codeminders.com on 5/4/2016.
 */
public class ThumbsHelper {
    private static final float MIN_SATURATION = 0.25f;
    private static final float MIN_ALPHA = 0.6f;
    private static final long ANIM_DURATION = 300;

    private static final @DrawableRes int icons[] = {R.drawable.bgn_thumb_1, R.drawable.bgn_thumb_2, R.drawable.bgn_thumb_3, R.drawable.bgn_thumb_4};
    private static final ThumbView.MapArea areas[] = {ThumbView.MapArea.LEFT_BOTTOM,  ThumbView.MapArea.RIGHT_TOP, ThumbView.MapArea.LEFT_TOP,  ThumbView.MapArea.LEFT_TOP, ThumbView.MapArea.RIGHT_BOTTOM, ThumbView.MapArea.LEFT_BOTTOM, ThumbView.MapArea.RIGHT_TOP, ThumbView.MapArea.RIGHT_BOTTOM};
    private final int colors[];
    private static ColorMatrix grayedMatrix = new ColorMatrix();
    private static ColorMatrixColorFilter disabledFilter;
    static {
        grayedMatrix.setSaturation(MIN_SATURATION);
        disabledFilter = new ColorMatrixColorFilter(grayedMatrix);
    }
    private static ColorDrawable transparentDrawable = new ColorDrawable(Color.TRANSPARENT);

    public ThumbsHelper(Context context) {
        colors = context.getResources().getIntArray(R.array.thumb_colors);
    }

    public ColorDrawable getTransparentDrawable() {
        return transparentDrawable;
    }

    public ColorMatrixColorFilter getDisabledFilter() {
        return disabledFilter;
    }

    public @DrawableRes int getIcon(CharSequence name) {
        return Convenience.getStringDependentItem(name, icons);
    }

    public @ColorInt int getColor(CharSequence name) {
        return Convenience.getStringDependentItem(name, colors);
    }

    public long getDuration() {
        return ANIM_DURATION;
    }

    public float getAnimAlpha(float value) {
        return value * (1 - MIN_ALPHA) + MIN_ALPHA;
    }

    public float getAnimSaturation(float value) {
        return value * (1 - MIN_SATURATION) + MIN_SATURATION;
    }

    public float getMinAlpha() {
        return MIN_ALPHA;
    }

    public ThumbView.MapArea getMapArea(CharSequence name) {
        return Convenience.getStringDependentItem(name, areas);
    }
}
