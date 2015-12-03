package com.zazoapp.client.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.zazoapp.client.R;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * View to display image and text over it
 * Created by skamenkovych@codeminders.com on 12/1/2015.
 */
public class TextImageView extends FrameLayout {

    private ImageView imageView;
    private TextView textView;
    private int shape = SHAPE_RECTANGLE;

    private static final int SHAPE_RECTANGLE = 0;
    private static final int SHAPE_CIRCLE = 1;

    public TextImageView(Context context) {
        this(context, null);
    }

    public TextImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        init(attrs, defStyleAttr);
        loadAttributes(attrs, defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyleAttr) {
        TypedArray a = getContext().obtainStyledAttributes(attrs,
                R.styleable.TextImageView, defStyleAttr, 0);
        try {
            if (a.hasValue(R.styleable.TextImageView_shape)) {
                shape = a.getInt(R.styleable.TextImageView_shape, SHAPE_RECTANGLE);
            }
        } finally {
            a.recycle();
        }
        switch (shape) {
            case SHAPE_RECTANGLE:
                imageView = new AppCompatImageView(getContext(), attrs);
                break;
            case SHAPE_CIRCLE:
                imageView = new CircleImageView(getContext());
                break;
        }
        textView = new TextView(getContext());
        LayoutParams paramsText = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);
        LayoutParams paramsImage = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);
        addView(imageView, paramsText);
        addView(textView, paramsImage);
    }

    private void loadAttributes(AttributeSet attrs, int defStyleAttr) {
        TypedArray a = getContext().obtainStyledAttributes(attrs,
                R.styleable.TextImageView, defStyleAttr, 0);
        try {
            int textSize = getContext().getResources().getDimensionPixelSize(R.dimen.text_normal);
            int n = a.length();
            for (int i = 0; i < n; i++) {
                if (!a.hasValue(i)) {
                    continue;
                }
                switch (i) {
                    case R.styleable.TextImageView_src:
                        imageView.setImageDrawable(a.getDrawable(i));
                        break;
                    case R.styleable.TextImageView_textSize:
                        textSize = a.getDimensionPixelSize(i, textSize);
                        break;
                    case R.styleable.TextImageView_text:
                        textView.setText(a.getText(i));
                        break;
                    case R.styleable.TextImageView_textColor:
                        textView.setTextColor(a.getColorStateList(i));
                        break;
                    case R.styleable.TextImageView_circular_border_color:
                        if (shape == SHAPE_CIRCLE) ((CircleImageView) imageView).setBorderColor(a.getColor(i, 0));
                        break;
                    case R.styleable.TextImageView_circular_border_overlay:
                        if (shape == SHAPE_CIRCLE) ((CircleImageView) imageView).setBorderOverlay(a.getBoolean(i, false));
                        break;
                    case R.styleable.TextImageView_circular_border_width:
                        if (shape == SHAPE_CIRCLE) ((CircleImageView) imageView).setBorderWidth(a.getDimensionPixelSize(i, 0));
                        break;
                    case R.styleable.TextImageView_circular_fill_color:
                        if (shape == SHAPE_CIRCLE) ((CircleImageView) imageView).setFillColor(a.getColor(i, 0));
                        break;
                }
            }
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            textView.setGravity(Gravity.CENTER);
        } finally {
            a.recycle();
        }
    }

    public boolean isCircular() {
        return shape == SHAPE_CIRCLE;
    }

    public void setText(CharSequence text) {
        textView.setText(text);
    }

    public CharSequence getText() {
        return textView.getText();
    }

    public void setImageDrawable(Drawable drawable) {
        imageView.setImageDrawable(drawable);
    }

    public void setImageResource(@DrawableRes int id) {
        imageView.setImageResource(id);
    }

    public void setImageAndText(Drawable drawable, CharSequence text) {
        imageView.setImageDrawable(drawable);
        textView.setText(text);
    }

    public void setImageAndText(@DrawableRes int id, CharSequence text) {
        imageView.setImageResource(id);
        textView.setText(text);
    }
}
