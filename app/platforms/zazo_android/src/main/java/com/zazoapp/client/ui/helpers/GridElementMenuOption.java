package com.zazoapp.client.ui.helpers;

import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import com.zazoapp.client.BuildConfig;
import com.zazoapp.client.R;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.features.Features;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 6/23/2016.
 */
public enum GridElementMenuOption {
    FULLSCREEN(false, R.string.ge_menu_fullscreen, R.drawable.ic_menu_fullscreen) {
        @Override
        public boolean isEnabled() {
            return super.isEnabled() && Features.Feature.PLAY_FULLSCREEN.isUnlocked(TbmApplication.getContext());
        }
    },
    TRANSCRIPT(false, R.string.ge_menu_transcript, R.drawable.ic_menu_transcript) {
        @Override
        public boolean isEnabled() {
            return super.isEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && BuildConfig.TRANSCRIPTION_TEST/*&& TRANSCRIPT_FEATURE_ENABLED*/;
        }
    },
    CHAT(false, R.string.ge_menu_chat, R.drawable.ic_menu_chat),
    DETAILS(true, R.string.ge_menu_details, R.drawable.ic_menu_friend_info);

    final boolean notForceDisabled;
    final int description;
    final int icon;

    GridElementMenuOption(boolean forceDisabled, @StringRes int description, @DrawableRes int icon) {
        this.notForceDisabled = !forceDisabled;
        this.description = description;
        this.icon = icon;
    }

    public boolean isEnabled() {
        return notForceDisabled;
    }

    public int getDescription() {
        return description;
    }

    public int getIcon() {
        return icon;
    }

    public static List<GridElementMenuOption> getAllEnabled() {
        List<GridElementMenuOption> list = new ArrayList<>();
        for (GridElementMenuOption option : values()) {
            if (option.isEnabled()) {
                list.add(option);
            }
        }
        return list;
    }
}
