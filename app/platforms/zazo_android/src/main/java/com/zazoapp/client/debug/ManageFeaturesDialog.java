package com.zazoapp.client.debug;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.zazoapp.client.R;
import com.zazoapp.client.core.RemoteStorageHandler;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.ui.dialogs.AbstractDialogFragment;

/**
 * Created by skamenkovych@codeminders.com on 3/10/2016.
 */
public class ManageFeaturesDialog extends AbstractDialogFragment implements View.OnClickListener {

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle("Manage features");
        View testView = View.inflate(getActivity(), R.layout.dialog_listview, null);
        ListView listView = (ListView) testView.findViewById(R.id.list);
        setCustomView(testView);
        ListAdapter adapter = new ManageFeatureAdapter();
        listView.setAdapter(adapter);
        setPositiveButton(getString(R.string.dialog_action_ok), this);
    }

    @Override
    public void onClick(View v) {
        saveFeatures();
        dismiss();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        saveFeatures();
    }

    private void saveFeatures() {
        RemoteStorageHandler.setUserSettings();
    }

    private class ManageFeatureAdapter extends BaseAdapter {
        private Features features;
        private LayoutInflater inflater;
        public ManageFeatureAdapter() {
            features = new Features(getActivity());
            ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.AppTheme_SwitchCompat);
            inflater = (LayoutInflater) contextThemeWrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return Features.Feature.values().length;
        }

        @Override
        public Features.Feature getItem(int position) {
            return Features.Feature.values()[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            SwitchViewHolder h;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.setting_bool_layout, null);
                h = new SwitchViewHolder(convertView);
                convertView.setTag(h);
            } else {
                h = (SwitchViewHolder) convertView.getTag();
            }
            final Features.Feature feature = getItem(position);
            h.checkbox.setOnCheckedChangeListener(null);
            h.checkbox.setChecked(features.isUnlockedPref(feature));
            h.checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        features.unlock(feature);
                    } else {
                        features.lock(feature);
                    }
                }
            });
            h.title.setText(feature.name());
            h.subtitle.setText(feature.getAction(getContext()));
            return convertView;
        }

    }

    static class SwitchViewHolder {
        @InjectView(R.id.title) TextView title;
        @InjectView(R.id.subtitle) TextView subtitle;
        @InjectView(R.id.checkbox) SwitchCompat checkbox;

        SwitchViewHolder(View v) {
            ButterKnife.inject(this, v);
        }
    }
}
