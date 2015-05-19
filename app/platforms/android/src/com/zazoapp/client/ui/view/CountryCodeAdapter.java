package com.zazoapp.client.ui.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.ButterKnife;
import com.zazoapp.client.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 5/19/2015.
 */
public class CountryCodeAdapter extends BaseAdapter implements Filterable {

    private Context context;
    private String[] countries;
    private String[] phoneCodes;
    private int[] countryFlags;
    private List<Integer> suggestions;

    private SearchFilter filter;

    public CountryCodeAdapter(Context context) {
        this.context = context;
        Resources r = context.getResources();
        countries = r.getStringArray(R.array.countries);
        phoneCodes = r.getStringArray(R.array.phone_codes);
        TypedArray flags = r.obtainTypedArray(R.array.country_flags);
        countryFlags = new int[flags.length()];
        for (int i = 0; i < flags.length(); i++) {
            countryFlags[i] = flags.getResourceId(i, 0);
        }
        flags.recycle();
        suggestions = new ArrayList<>();
        filter = new SearchFilter();
        if (countryFlags.length != phoneCodes.length || phoneCodes.length != countries.length) {
            throw new RuntimeException("Setup codes arrays correspondingly");
        }
    }

    @Override
    public int getCount() {
        return suggestions.size();
    }

    @Override
    public Integer getItem(int position) {
        return suggestions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        if (convertView != null) {
            holder = (Holder) convertView.getTag();
        } else {
            holder = new Holder();
            convertView = View.inflate(context, R.layout.country_list_item, null);
            holder.flag = ButterKnife.findById(convertView, R.id.icon);
            holder.name = ButterKnife.findById(convertView, R.id.text);
            convertView.setTag(holder);
        }
        holder.flag.setImageResource(countryFlags[getItem(position)]);
        holder.name.setText(countries[getItem(position)]);
        return convertView;
    }

    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new SearchFilter();
        }
        return filter;
    }

    private static class Holder {
        ImageView flag;
        TextView name;
    }

    private class SearchFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint != null) {
                ArrayList<Integer> list = new ArrayList<>();
                for (int i = 0; i < phoneCodes.length; i++) {
                    if (phoneCodes[i].startsWith(String.valueOf(constraint))) {
                        list.add(i);
                    }
                }
                results.count = list.size();
                results.values = list;
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (constraint != null) {
                synchronized (this) {
                    suggestions.clear();
                    suggestions.addAll((Collection<? extends Integer>) results.values);
                }
                notifyDataSetChanged();
            }
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            return phoneCodes[(int) resultValue];
        }
    }
}
