package com.zazoapp.client.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.AnimRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.SwitchCompat;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
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
public class SettingsFragment extends ZazoTopFragment implements Settings.LinkClickedCallback {

    private static final String SETTINGS_TOP_TAG = "SettingsTop";
    @InjectView(R.id.settings_list) ScrollView settingsList;
    @InjectView(R.id.up) MaterialMenuView up;

    private ZazoTopFragment topFragment;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Fragment fragment = getChildFragmentManager().findFragmentByTag(SETTINGS_TOP_TAG);
        if (fragment instanceof ZazoTopFragment) {
            topFragment = (ZazoTopFragment) fragment;
            topFragment.setOnBackListener(new OnBackListener() {
                @Override
                public void onBack() {
                    topFragment = null;
                }
            });
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.settings, null);
        ButterKnife.inject(this, v);
        up.setState(MaterialMenuDrawable.IconState.ARROW);
        LinearLayout layout = new LinearLayout(v.getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(getViewForPref(inflater, Settings.Link.UPDATE_PROFILE_PHOTO, this));
        layout.addView(getViewForPref(inflater, Settings.Bool.ALLOW_DATA_IN_ROAMING));
        layout.addView(getViewForPref(inflater, Settings.Bool.LIGHT_SCREEN_FOR_NOTIFICATIONS));
        settingsList.addView(layout);
        return v;
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (nextAnim != R.anim.slide_left_fade_in && nextAnim != R.anim.slide_right_fade_out) {
            return super.onCreateAnimation(transit, enter, nextAnim);
        }
        return SlideHorizontalFadeAnimation.get(getActivity(), nextAnim);
    }

    @OnClick(R.id.up)
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.up:
                if (getOnBackListener() != null) {
                    getOnBackListener().onBack();
                }
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

    private View getViewForPref(LayoutInflater inflater, final Settings.Link pref, final Settings.LinkClickedCallback callback) {
        View v = inflater.inflate(R.layout.setting_link_layout, null);
        LinkSettingViewHolder h = new LinkSettingViewHolder(v);
        h.title.setText(pref.getLabel());
        if (pref.getHint() != 0) {
            h.subtitle.setText(pref.getHint());
            h.subtitle.setVisibility(View.VISIBLE);
        } else {
            h.subtitle.setVisibility(View.GONE);
        }
        h.parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.onLinkClicked(pref);
            }
        });
        return v;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (topFragment != null) {
            return topFragment.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onLinkClicked(Settings.Link link) {
        switch (link) {
            case UPDATE_PROFILE_PHOTO: {
                startActivity(new Intent(getActivity(), ProfileActivity.class));
            }
                break;
        }
    }

    private void showTopFragment(ZazoTopFragment f, @AnimRes int in, @AnimRes int out) {
        FragmentTransaction tr = getChildFragmentManager().beginTransaction();
        tr.setCustomAnimations(in, out, in, out);
        tr.add(R.id.top_frame, f, SETTINGS_TOP_TAG);
        tr.addToBackStack(null);
        tr.commit();
        f.setOnBackListener(new ZazoTopFragment.OnBackListener() {
            @Override
            public void onBack() {
                topFragment = null;
            }
        });
        topFragment = f;
    }

    static class SwitchSettingViewHolder {
        @InjectView(R.id.title) TextView title;
        @InjectView(R.id.subtitle) TextView subtitle;
        @InjectView(R.id.checkbox) SwitchCompat checkbox;

        SwitchSettingViewHolder(View v) {
            ButterKnife.inject(this, v);
        }
    }

    static class LinkSettingViewHolder {
        @InjectView(R.id.title) TextView title;
        @InjectView(R.id.subtitle) TextView subtitle;
        View parent;

        LinkSettingViewHolder(View v) {
            ButterKnife.inject(this, v);
            parent = v;
        }
    }
}
