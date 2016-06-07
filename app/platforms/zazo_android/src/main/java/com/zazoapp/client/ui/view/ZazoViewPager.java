package com.zazoapp.client.ui.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.ui.ZazoManagerProvider;

/**
 * This custom view extends ViewPager to support app specific cases
 * Created by skamenkovych@codeminders.com on 1/5/2016.
 */
public class ZazoViewPager extends ViewPager {
    private static final String TAG = ZazoViewPager.class.getSimpleName();
    public ZazoViewPager(Context context) {
        super(context);
    }

    public ZazoViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        ZazoManagerProvider managers = TbmApplication.getInstance().getManagerProvider();
        Log.i(TAG, "" + v + " " + dx + " " + managers.getPlayer().isPlaying());
        if (managers.getPlayer().isPlaying() || managers.getRecorder().isRecording()) {
            return true;
        }
        if (v instanceof NineViewGroup) {
            if (((NineViewGroup) v).getSpinStrategy().isSpinning()) {
                return true;
            }
        }
        return super.canScroll(v, checkV, dx, x, y);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ZazoManagerProvider managers = TbmApplication.getInstance().getManagerProvider();
        if (managers.getPlayer().isPlaying() || managers.getRecorder().isRecording()) {
            return false;
        }
        return super.onInterceptTouchEvent(ev);
    }
}
