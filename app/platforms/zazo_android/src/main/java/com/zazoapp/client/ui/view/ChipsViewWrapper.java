package com.zazoapp.client.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.zazoapp.client.R;
import com.zazoapp.client.ui.helpers.ThumbsHelper;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.StringUtils;

/**
 * Created by skamenkovych@codeminders.com on 4/29/2016.
 */
public class ChipsViewWrapper {
    @InjectView(R.id.title) TextView title;
    @InjectView(R.id.thumb) CircleThumbView thumb;
    @InjectView(R.id.thumb_title) TextView thumbTitle;
    private View layout;

    private final ThumbsHelper tHelper;
    public ChipsViewWrapper(Context context) {
        layout = View.inflate(context, R.layout.chips_layout, null);
        if (layout.getLayoutParams() == null) {
            layout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        ButterKnife.inject(this, layout);
        tHelper = new ThumbsHelper(context);
        Typeface tf = Convenience.getTypeface(context, "Roboto-Regular");
        title.setTypeface(tf);
        thumbTitle.setTypeface(tf);
    }

    public View getView() {
        return layout;
    }

    public void setTitle(String text) {
        title.setText(text);
        thumbTitle.setText(StringUtils.getInitials(text));
        thumb.setImageResource(tHelper.getIcon(text));
        thumb.setFillColor(tHelper.getColor(text));
    }

    public void setTitleWithIcon(String text, Bitmap icon) {
        title.setText(text);
        thumbTitle.setText(null);
        thumb.setImageBitmap(icon);
        thumb.setFillColor(Color.TRANSPARENT);
    }

    public void setMore() {
        thumb.setVisibility(View.GONE);
        thumbTitle.setVisibility(View.GONE);
        title.setText("...");
    }
}
