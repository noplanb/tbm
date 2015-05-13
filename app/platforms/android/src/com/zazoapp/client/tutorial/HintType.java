package com.zazoapp.client.tutorial;

import android.content.Context;
import android.graphics.RectF;
import android.view.View;
import com.zazoapp.client.PreferencesHelper;
import com.zazoapp.client.R;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.VideoFactory;

/**
 * Created by skamenkovych@codeminders.com on 5/13/2015.
 */
public enum HintType {
    INVITE_1(R.string.tutorial_hint_invite_1) {
        @Override
        boolean shouldShow(HintType current, PreferencesHelper prefs) {
            return FriendFactory.getFactoryInstance().count() == 0;
        }

        @Override
        void show(TutorialLayout layout, View view) {
            layout.dimExceptForRect(getViewRect(view));
        }
    },
    INVITE_2(R.string.tutorial_hint_invite_2) {
        @Override
        boolean shouldShow(HintType current, PreferencesHelper prefs) {
            boolean oneFriend = FriendFactory.getFactoryInstance().count() == 1;
            boolean allViewed = VideoFactory.getFactoryInstance().allNotViewedCount() == 0;
            boolean playHintShowed = !prefs.getBoolean(HintType.PLAY.getPrefName(), true);
            boolean recordHintShowed = !prefs.getBoolean(HintType.RECORD.getPrefName(), true);
            boolean sentHintShowed = !prefs.getBoolean(HintType.SENT.getPrefName(), true);
            boolean viewedHintShowed = !prefs.getBoolean(HintType.VIEWED.getPrefName(), true);

            return oneFriend && allViewed && playHintShowed && recordHintShowed && sentHintShowed && viewedHintShowed;
        }

        @Override
        void show(TutorialLayout layout, View view) {
            layout.dimExceptForRect(getViewRect(view));
        }
    },
    PLAY(R.string.tutorial_hint_play) {
        @Override
        boolean shouldShow(HintType current, PreferencesHelper prefs) {
            int friendsCount = FriendFactory.getFactoryInstance().count();
            int unviewedCount = VideoFactory.getFactoryInstance().allNotViewedCount();
            return friendsCount > 0 && unviewedCount > 0 && prefs.getBoolean(getPrefName(), true);
        }

        @Override
        void show(TutorialLayout layout, View view) {
            setExcludedBox(layout, view);
            layout.dim();
        }
    },
    RECORD(R.string.tutorial_hint_record) {
        @Override
        boolean shouldShow(HintType current, PreferencesHelper prefs) {
            int friendsCount = FriendFactory.getFactoryInstance().count();
            return friendsCount > 0 && current != HintType.PLAY && prefs.getBoolean(getPrefName(), true);
        }

        @Override
        void show(TutorialLayout layout, View view) {
            setExcludedBox(layout, view);
            layout.dim();
        }
    },
    SENT(R.string.tutorial_hint_sent) {
        @Override
        boolean shouldShow(HintType current, PreferencesHelper prefs) {
            int friendsCount = FriendFactory.getFactoryInstance().count();
            return friendsCount > 0 && current == null && prefs.getBoolean(getPrefName(), true);
        }

        @Override
        void show(TutorialLayout layout, View view) {
            View indicator = view.findViewById(R.id.img_viewed); // we use viewed due to animation of uploading indicator at this moment
            layout.dimExceptForRect(getViewRect(indicator));
            delayedDismiss(layout, HintType.SENT);
        }
    },
    VIEWED(R.string.tutorial_hint_viewed) {
        @Override
        boolean shouldShow(HintType current, PreferencesHelper prefs) {
            int friendsCount = FriendFactory.getFactoryInstance().count();
            return friendsCount > 0 && current == null && prefs.getBoolean(getPrefName(), true);
        }

        @Override
        void show(TutorialLayout layout, View view) {
            View indicator = view.findViewById(R.id.img_viewed);
            layout.dimExceptForRect(getViewRect(indicator));
        }
    };

    private String prefName;
    private int hintTextId;

    HintType(int id) {
        prefName = "pref_hint_" + name().toLowerCase();
        hintTextId = id;
    }

    public String getPrefName() {
        return prefName;
    }

    String getHint(Context context) {
        return context.getString(hintTextId);
    }

    abstract boolean shouldShow(HintType current, PreferencesHelper prefs);

    abstract void show(TutorialLayout layout, View view);

    static RectF getViewRect(View view) {
        int[] location = new int[2];
        view.getLocationInWindow(location);
        int left = location[0];
        int top = location[1];
        int right = left + view.getWidth();
        int bottom = top + view.getHeight();
        return new RectF(left, top, right, bottom);
    }

    static int[] getViewInnerCircle(View view) {
        int[] circleData = new int[3];
        int[] location = new int[2];
        view.getLocationInWindow(location);
        circleData[0] = location[0] + view.getWidth() / 2;
        circleData[1] = location[1] + view.getHeight() / 2;
        circleData[2] = Math.min(view.getWidth(), view.getHeight()) / 2;
        return circleData;
    }

    static int[] getViewOuterCircle(View view) {
        int[] circleData = new int[3];
        int[] location = new int[2];
        view.getLocationInWindow(location);
        circleData[0] = location[0] + view.getWidth() / 2;
        circleData[1] = location[1] + view.getHeight() / 2;
        circleData[2] = (int) (Math.max(view.getWidth(), view.getHeight()) * 0.8);
        return circleData;
    }

    private static void setExcludedBox(TutorialLayout layout, View view) {
        View indicator = view.findViewById(R.id.tw_unread_count);
        if (indicator.getVisibility() == View.VISIBLE) {
            layout.setExcludedCircle(getViewInnerCircle(indicator));
        } else {
            layout.setExcludedCircle(0, 0, 0);
        }
        layout.setExcludedRect(getViewRect(view));
    }

    private static void delayedDismiss(final TutorialLayout layout, final HintType hint) {
        layout.postDelayed(new Runnable() {
            @Override
            public void run() {
                layout.dismiss();
            }
        }, 3500);
    }
}
