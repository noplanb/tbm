package com.zazoapp.client.ui.view;

import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.util.AttributeSet;
import de.hdodenhof.circleimageview.CircleImageView;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by skamenkovych@codeminders.com on 3/29/2016.
 */
public class CircleThumbView extends CircleImageView {

    private ColorFilter fillColorFilter;

    public CircleThumbView(Context context) {
        super(context);
    }

    public CircleThumbView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CircleThumbView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setFillColorFilter(ColorFilter filter) {
        if(filter != fillColorFilter) {
            fillColorFilter = filter;
            try {
                Field field = CircleImageView.class.getDeclaredField("mFillPaint");
                field.setAccessible(true);
                Method m = Paint.class.getDeclaredMethod("setColorFilter", ColorFilter.class);
                m.setAccessible(true);
                m.invoke(field.get(this), filter);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            this.invalidate();
        }
    }
}
