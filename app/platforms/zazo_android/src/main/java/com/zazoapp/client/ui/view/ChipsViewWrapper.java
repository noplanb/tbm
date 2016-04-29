package com.zazoapp.client.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.zazoapp.client.R;
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

    private final int icons[] = {R.drawable.bgn_thumb_1, R.drawable.bgn_thumb_2, R.drawable.bgn_thumb_3, R.drawable.bgn_thumb_4};
    private final int colors[];

    public ChipsViewWrapper(Context context) {
        layout = View.inflate(context, R.layout.chips_layout, null);
        ButterKnife.inject(this, layout);
        colors = context.getResources().getIntArray(R.array.thumb_colors);
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
        thumb.setImageResource(Convenience.getStringDependentItem(text, icons));
        thumb.setFillColor(Convenience.getStringDependentItem(text, colors));
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
