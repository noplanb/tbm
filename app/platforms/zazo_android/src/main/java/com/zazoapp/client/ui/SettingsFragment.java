package com.zazoapp.client.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.MaterialMenuView;
import com.zazoapp.client.R;
import com.zazoapp.client.core.Settings;
import com.zazoapp.client.ui.animations.SlideHorizontalFadeAnimation;

/**
 * Created by skamenkovych@codeminders.com on 1/22/2016.
 */
public class SettingsFragment extends Fragment {

    @InjectView(R.id.settings_list) ScrollView settingsList;
    @InjectView(R.id.up) MaterialMenuView up;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.settings, null);
        ButterKnife.inject(this, v);
        up.setState(MaterialMenuDrawable.IconState.ARROW);
        settingsList.addView(getViewForPref(inflater, Settings.Bool.ALLOW_DATA_IN_ROAMING));
        return v;
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (nextAnim != R.anim.slide_left_fade_in && nextAnim != R.anim.slide_right_fade_out) {
            return super.onCreateAnimation(transit, enter, nextAnim);
        }
        return SlideHorizontalFadeAnimation.get(getActivity(), nextAnim);
    }

    @OnClick(R.id.home)
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.home:
                getFragmentManager().popBackStack();
                break;
        }
    }

    private View getViewForPref(LayoutInflater inflater, final Settings.Bool pref) {
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(inflater.getContext(), R.style.AppTheme_SwitchCompat);
        LayoutInflater themedInflater = (LayoutInflater) contextThemeWrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = themedInflater.inflate(R.layout.setting_bool_layout, null);
        SwitchSettingViewHolder h = new SwitchSettingViewHolder(v);
        h.checkbox.setChecked(pref.isSet());
        h.title.setText(pref.getLabel());
        if (pref.getHint() != 0) {
            h.subtitle.setText(pref.getHint());
            h.subtitle.setVisibility(View.VISIBLE);
        } else {
            h.subtitle.setVisibility(View.GONE);
        }
        h.checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Settings.set(pref, isChecked);
            }
        });
        return v;
    }

    static class SwitchSettingViewHolder {
        @InjectView(R.id.title) TextView title;
        @InjectView(R.id.subtitle) TextView subtitle;
        @InjectView(R.id.checkbox) SwitchCompat checkbox;

        SwitchSettingViewHolder(View v) {
            ButterKnife.inject(this, v);
        }
    }
}
