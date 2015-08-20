package com.zazoapp.client.ui.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import butterknife.ButterKnife;
import com.zazoapp.client.R;
import com.zazoapp.client.utilities.Convenience;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 8/14/2015.
 */
public class MainMenuPopup {

    private ListPopupWindow options;
    private Context context;
    private MenuItemListener menuItemListener;
    private List<Integer> disabledOptions;

    public interface MenuItemListener {
        void onMenuItemSelected(int id);
    }

    public MainMenuPopup(Context context, List<Integer> disabledOptions) {
        this.context = context;
        this.disabledOptions = disabledOptions;
        options = new ListPopupWindow(context);
        options.setAdapter(new MenuAdapter());
        options.setListSelector(context.getResources().getDrawable(R.drawable.options_popup_item_bg));
        options.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.options_popup_bg));
        options.setHorizontalOffset(-Convenience.dpToPx(context, 15));
        options.setModal(true);
    }

    public void setAnchorView(View anchor) {
        options.setAnchorView(anchor);
    }

    public void show() {
        options.show();
        options.getListView().setDivider(null);
        options.getListView().setDividerHeight(0);
    }

    public void setMenuItemListener(MenuItemListener listener) {
        menuItemListener = listener;
        options.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                menuItemListener.onMenuItemSelected((int) id);
                options.dismiss();
            }
        });
    }

    private class MenuAdapter extends BaseAdapter {
        private final int[][] allMenuItems = {
                {R.id.menu_manage_friends, R.string.manage_friends},
                {R.id.menu_send_feedback, R.string.send_feedback},
        };

        private List<int[]> menuItems = new ArrayList<>();

        MenuAdapter() {
            for (int[] item : allMenuItems) {
                if (!disabledOptions.contains(item[0])) {
                    menuItems.add(item);
                }
            }
        }

        @Override
        public int getCount() {
            return menuItems.size();
        }

        @Override
        public int[] getItem(int position) {
            return menuItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position)[0];
        }

        //@Override
        //public boolean isEnabled(int position) {
        //    return !disabledOptions.contains(getItem(position)[0]);
        //}

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder;
            if (convertView != null) {
                holder = (Holder) convertView.getTag();
            } else {
                convertView = View.inflate(context, R.layout.menu_list_item, null);
                holder = new Holder();
                holder.text = ButterKnife.findById(convertView, R.id.text);
                convertView.setTag(holder);
            }
            holder.text.setText(getItem(position)[1]);
            convertView.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            View anchor = options.getAnchorView();
            if (options.getWidth() < holder.text.getMeasuredWidth() + anchor.getWidth()) {
                options.setContentWidth(holder.text.getMeasuredWidth() + anchor.getWidth());
            }
            if (options.getVerticalOffset() == 0) {
                options.setVerticalOffset(-(anchor.getHeight() + convertView.getMeasuredHeight()) / 2);
            }
            convertView.setEnabled(isEnabled(position));
            return convertView;
        }

        private class Holder {
            TextView text;
        }
    }
}
