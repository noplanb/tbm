package com.zazoapp.client.bench;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import com.zazoapp.client.R;

import java.util.ArrayList;
import java.util.List;

/**
 * http://stackoverflow.com/questions/26755878/how-can-i-fix-the-spinner-style-for-android-4-x-placed-on-top-of-the-toolbar
 * Created by skamenkovych@codeminders.com on 11/20/2015.
 */
public final class ContactsGroupAdapter extends BaseAdapter {

    private List<GeneralContactsGroup> mItems = new ArrayList<>();

    private Context context;

    ContactsGroupAdapter(Context context) {
        this.context = context;
        addItems(GeneralContactsGroup.getActive());
    }

    public void clear() {
        mItems.clear();
    }

    public void addItems(List<GeneralContactsGroup> yourObjectList) {
        mItems.addAll(yourObjectList);
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public GeneralContactsGroup getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getDropDownView(int position, View view, ViewGroup parent) {
        Holder h;
        if (view == null || !((Holder)view.getTag()).isDropdown()) {
            view = View.inflate(context, R.layout.contacts_group_item, null);
            h = new Holder(view, true);
            view.setTag(h);
        } else {
            h = (Holder) view.getTag();
        }
        GeneralContactsGroup item = getItem(position);
        h.text.setText(item.getTitleId());
        h.icon.setImageResource(item.getIconId());
        return view;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        Holder h;
        if (view == null || !((Holder)view.getTag()).isNonDropdown()) {
            view = View.inflate(context, R.layout.menu_list_item, null);
            h = new Holder(view, false);
            view.setTag(h);
        } else {
            h = (Holder) view.getTag();
        }
        GeneralContactsGroup item = getItem(position);
        h.text.setText(item.getTitleId());
        return view;
    }

    static class Holder {
        String tag;
        @Optional @InjectView(R.id.icon) ImageView icon;
        @InjectView(R.id.text) TextView text;

        Holder(View v, boolean isDropdown) {
            ButterKnife.inject(this, v);
            tag = isDropdown ? "DROPDOWN" : "NON_DROPDOWN";
        }

        boolean isDropdown() {
            return "DROPDOWN".equals(tag);
        }

        boolean isNonDropdown() {
            return "NON_DROPDOWN".equals(tag);
        }
    }
}

