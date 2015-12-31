package com.zazoapp.client.ui.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import com.zazoapp.client.ui.view.transferview.animation.TransferAnimationController;

/**
 * Created by Serhii on 31.12.2015.
 */
public abstract class ITransferView extends RelativeLayout {
    public ITransferView(Context context) {
        super(context);
    }

    public ITransferView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ITransferView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ITransferView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public abstract TransferAnimationController getAnimationController();
}
