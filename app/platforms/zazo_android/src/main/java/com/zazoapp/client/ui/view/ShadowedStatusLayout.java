package com.zazoapp.client.ui.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import com.zazoapp.client.R;
import com.zazoapp.client.utilities.Convenience;

/**
 * Created by skamenkovych@codeminders.com on 1/4/2016.
 */
public class ShadowedStatusLayout extends RelativeLayout {
    private Paint solidBackground;
    private Paint gradient;
    public ShadowedStatusLayout(Context context) {
        this(context, null);
    }

    public ShadowedStatusLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShadowedStatusLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ShadowedStatusLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        int backgroundColor = Color.WHITE;
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ShadowedStatusLayout);
        try {
            if (a.hasValue(R.styleable.ShadowedStatusLayout_solid_background_color)) {
                backgroundColor = a.getColor(R.styleable.ShadowedStatusLayout_solid_background_color, Color.WHITE);
            }
        } finally {
            a.recycle();
        }
        solidBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
        solidBackground.setColor(backgroundColor);
        gradient = new Paint(Paint.ANTI_ALIAS_FLAG);
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), getHeight(), gradient);
        canvas.drawCircle(getWidth()*0.5f, getHeight()*0.5f, getHeight()*0.5f - Convenience.dpToPx(getContext(), 2.5f), solidBackground);
        super.onDraw(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        gradient.setShader(new RadialGradient(w*0.5f, h*0.51f, h*0.49f, Color.parseColor("#55000000"), Color.TRANSPARENT, Shader.TileMode.CLAMP));
    }
}
