package com.zazoapp.client.ui.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.zazoapp.client.R;
import com.zazoapp.client.ui.helpers.GridElementMenuOption;

import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 6/22/2016.
 */
public class GridElementMenuAdapter extends BaseAdapter {

    private final List<GridElementMenuOption> menuItems;
    private Context context;

    public GridElementMenuAdapter(Context context, List<GridElementMenuOption> list) {
        menuItems = list;
        this.context = context;
    }

    @Override
    public int getCount() {
        return menuItems.size();
    }

    @Override
    public GridElementMenuOption getItem(int position) {
        return menuItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ListItemHolder h;
        if (convertView != null) {
            h = (ListItemHolder) convertView.getTag();
        } else {
            convertView = View.inflate(context, R.layout.ge_list_item, null);
            h = new ListItemHolder(convertView);
            convertView.setTag(h);
        }
        GridElementMenuOption option = getItem(position);
        h.title.setText(option.getDescription());
        h.icon.setImageResource(option.getIcon());
        return convertView;
    }

    class ListItemHolder { // abc_activity_chooser_view_list_item
        @InjectView(R.id.icon) ImageView icon;
        @InjectView(R.id.title) TextView title;

        ListItemHolder(View v) {
            ButterKnife.inject(this, v);
        }
    }

    public int measureContentWidth() {
        /** Code is taken from {@link android.support.v7.view.menu.MenuPopupHelper} */
        // Menus don't tend to be long, so this is more sane than it looks.
        int maxWidth = 0;
        View itemView = null;
        int itemType = 0;

        final int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        final int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        final int count = getCount();

        int mPopupMaxWidth = Math.max(context.getResources().getDisplayMetrics().widthPixels / 2,
                context.getResources().getDimensionPixelSize(android.support.v7.appcompat.R.dimen.abc_config_prefDialogWidth));
        ViewGroup mMeasureParent = null;
        for (int i = 0; i < count; i++) {
            final int positionType = getItemViewType(i);
            if (positionType != itemType) {
                itemType = positionType;
                itemView = null;
            }

            if (mMeasureParent == null) {
                mMeasureParent = new FrameLayout(context);
            }

            itemView = getView(i, itemView, mMeasureParent);
            itemView.measure(widthMeasureSpec, heightMeasureSpec);

            final int itemWidth = itemView.getMeasuredWidth();
            if (itemWidth >= mPopupMaxWidth) {
                return mPopupMaxWidth;
            } else if (itemWidth > maxWidth) {
                maxWidth = itemWidth;
            }
        }

        return maxWidth;
    }
}
