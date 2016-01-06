package com.zazoapp.client.ui.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

/**
 * This custom view extends ViewPager to support app specific cases
 * Created by skamenkovych@codeminders.com on 1/5/2016.
 */
public class ZazoViewPager extends ViewPager {
    public ZazoViewPager(Context context) {
        super(context);
    }

    public ZazoViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        if (v instanceof NineViewGroup) {
            if (((NineViewGroup) v).getSpinStrategy().isSpinning()) {
                return true;
            }
        }
        return super.canScroll(v, checkV, dx, x, y);
    }
}
