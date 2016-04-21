package com.zazoapp.client.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.zazoapp.client.R;

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
    private double childToParentRatio = PARENT_WIDTH_RATIO;

    public StatusIndicator(Context context) {
        super(context);
    }

    public StatusIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StatusIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.StatusIndicator);

        if (typedArray.hasValue(R.styleable.StatusIndicator_ratio)) {
            childToParentRatio = typedArray.getFloat(R.styleable.StatusIndicator_ratio, (float) PARENT_WIDTH_RATIO);
        }
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
            return (parentSize * childToParentRatio > maxSize) ? maxSize : (int) (parentSize * childToParentRatio);
        } else {
            return parentSize; // already calculated, just pass
        }
    }

    public void setRatio(double ratio) {
        childToParentRatio = ratio;
    }
}
