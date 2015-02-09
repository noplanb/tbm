package com.noplanbees.tbm.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.noplanbees.tbm.R;

/**
 * View is used to show square image status.
 * To measure correctly in xml file it should be declared with
 * <code><pre>
 *  layout_width="match_parent"
 *  layout_height="match_parent"
 * </pre></code>
 *
 * Created by skamenkovych@codeminders.com on 2/9/2015.
 */
public class StatusIndicator extends ImageView {

    private static final double PARENT_WIDTH_RATIO = 0.15f;

    public StatusIndicator(Context context) {
        super(context);
    }

    public StatusIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StatusIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        // As we have square indicator it is calculated only width size
        if (width != height) {
            int size = getSize(width);
            setMeasuredDimension(size, size);
        }
    }

    private int getSize(int parentSize) {
        int maxSize = getResources().getDimensionPixelSize(R.dimen.video_status_indicator_maximum_width);
        if (maxSize < parentSize) {
            return (parentSize * PARENT_WIDTH_RATIO > maxSize) ? maxSize : (int) (parentSize * PARENT_WIDTH_RATIO);
        } else {
            return parentSize; // already calculated, just pass
        }
    }
}
