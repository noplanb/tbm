package com.zazoapp.client.tutorial;

import android.content.Context;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;
import android.view.ViewParent;
import butterknife.ButterKnife;
import com.zazoapp.client.R;
import com.zazoapp.client.core.PreferencesHelper;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.IncomingVideoFactory;
import com.zazoapp.client.ui.view.NineViewGroup;

import java.util.Random;

/**
 * Created by skamenkovych@codeminders.com on 5/13/2015.
 */
public enum HintType {
    INVITE_1(R.string.tutorial_hint_invite_1, 0) {
        @Override
        boolean shouldShow(TutorialEvent event, HintType current, PreferencesHelper prefs, Bundle params) {
            return event == TutorialEvent.LAUNCH && FriendFactory.getFactoryInstance().count() == 0;
        }

        @Override
        void show(TutorialLayout layout, View view, Tutorial tutorial, PreferencesHelper prefs) {
            NineViewGroup nineViewGroup = getNineViewGroup(view);
            if (nineViewGroup != null) {
                layout.dimExceptForRect(getViewRect(nineViewGroup.getFrame(NineViewGroup.Box.CENTER_RIGHT)));
            }
        }
    },
    FEATURE_SWITCH_CAMERA(R.string.feature_switch_camera_hint, 0, Features.Feature.SWITCH_CAMERA) {
        @Override
        void show(TutorialLayout layout, View view, Tutorial tutorial, PreferencesHelper prefs) {
            NineViewGroup nineViewGroup = getNineViewGroup(view);
            if (nineViewGroup != null) {
                layout.dimExceptForRect(getViewRect(nineViewGroup.getCenterFrame()));
            }
        }
    },
    FEATURE_ABORT_RECORDING(R.string.feature_abort_recording_hint, R.string.tutorial_try_it, Features.Feature.ABORT_RECORDING) {
        @Override
        void show(TutorialLayout layout, View view, Tutorial tutorial, PreferencesHelper prefs) {
            NineViewGroup nineViewGroup = getNineViewGroup(view);
            if (nineViewGroup != null) {
                layout.dimExceptForRect(getViewRect(nineViewGroup.getFrame(NineViewGroup.Box.CENTER_RIGHT)));
            }
        }
    },
    FEATURE_DELETE_FRIEND(R.string.feature_delete_friend_hint, R.string.tutorial_got_it, Features.Feature.DELETE_FRIEND) {
        @Override
        void show(TutorialLayout layout, View view, Tutorial tutorial, PreferencesHelper prefs) {
            View parent = (View) layout.getParent();
            if (parent != null) {
                View button = parent.findViewById(R.id.menu_view);
                if (button != null) {
                    layout.dimExceptForRect(getViewRect(button));
                }
            }
        }
    },
    FEATURE_EARPIECE(R.string.feature_earpiece_hint, R.string.tutorial_try_it, Features.Feature.EARPIECE) {
        @Override
        void show(TutorialLayout layout, View view, Tutorial tutorial, PreferencesHelper prefs) {
            NineViewGroup nineViewGroup = getNineViewGroup(view);
            if (nineViewGroup != null) {
                layout.dimExceptForRect(getViewRect(nineViewGroup.getFrame(NineViewGroup.Box.CENTER_RIGHT)));
            }
        }
    },
    FEATURE_CAROUSEL(R.string.feature_carousel_hint, R.string.tutorial_try_it, Features.Feature.CAROUSEL) {
        @Override
        void show(TutorialLayout layout, View view, Tutorial tutorial, PreferencesHelper prefs) {
            NineViewGroup nineViewGroup = getNineViewGroup(view);
            if (nineViewGroup != null) {
                layout.dimExceptForRect(getViewRect(nineViewGroup.getFrame(NineViewGroup.Box.BOTTOM_LEFT)));
            }
        }
    },
    PLAY_RECORD(R.string.tutorial_hint_play_record, 0) {
        @Override
        boolean shouldShow(TutorialEvent event, HintType current, PreferencesHelper prefs, Bundle params) {
            return event == TutorialEvent.NEW_MESSAGE && current == RECORD && PLAY.shouldShow(event, null, prefs, params);
        }

        @Override
        void show(final TutorialLayout layout, final View view, final Tutorial tutorial, final PreferencesHelper prefs) {
            layout.dismissSoftly(new TutorialLayout.OnTutorialEventListener() {
                @Override
                public void onDismiss() {
                    setExcludedBox(layout, view);
                    layout.dim();
                }

                @Override
                public void onDimmed() {
                }
            });
        }
    },
    PLAY(R.string.tutorial_hint_play, 0) {
        @Override
        boolean shouldShow(TutorialEvent event, HintType current, PreferencesHelper prefs, Bundle params) {
            switch (event) {
                case LAUNCH:
                case NEW_MESSAGE:
                    int unviewedCount = IncomingVideoFactory.getFactoryInstance().allNotViewedCount();
                    return hasOneFriend() && unviewedCount > 0 && prefs.getBoolean(getPrefName(), true) && current == null;
            }
            return false;
        }

        @Override
        void show(TutorialLayout layout, View view, Tutorial tutorial, PreferencesHelper prefs) {
            setExcludedBox(layout, view);
            layout.dim();
        }
    },
    SEND_WELCOME_WITH_RECORD(R.string.tutorial_hint_send_welcome_with_record, 0) {
        @Override
        boolean shouldShow(TutorialEvent event, HintType current, PreferencesHelper prefs, Bundle params) {
            if (event != TutorialEvent.FRIEND_ADDED || params == null || !params.containsKey(Tutorial.FRIEND_KEY)) {
                return false;
            }
            Friend friend = FriendFactory.getFactoryInstance().find(params.getString(Tutorial.FRIEND_KEY));
            return (prefs.getBoolean(RECORD.getPrefName(), true) || !friend.hasApp()) && SEND_WELCOME.shouldShow(event, current, prefs, params);
        }

        @Override
        void show(TutorialLayout layout, View view, Tutorial tutorial, PreferencesHelper prefs) {
            SEND_WELCOME.show(layout, view, tutorial, prefs);
        }
    },
    RECORD(R.string.tutorial_hint_record, 0) {
        @Override
        boolean shouldShow(TutorialEvent event, HintType current, PreferencesHelper prefs, Bundle params) {
            switch (event) {
                case LAUNCH:
                case FRIEND_ADDED:
                case VIDEO_VIEWED:
                    boolean firstInSession = prefs.getBoolean(getPrefSessionName(), true);
                    int unviewedCount = IncomingVideoFactory.getFactoryInstance().allNotViewedCount();
                    return hasOneFriend() && unviewedCount == 0 && firstInSession && current == null && prefs.getBoolean(getPrefName(), true);
            }
            return false;
        }

        @Override
        void show(TutorialLayout layout, View view, Tutorial tutorial, PreferencesHelper prefs) {
            NineViewGroup nineViewGroup = getNineViewGroup(view);
            if (nineViewGroup != null) {
                view = nineViewGroup.getFrame(NineViewGroup.Box.CENTER_RIGHT);
            }
            setExcludedBox(layout, view);
            layout.dim();
            markHintAsShowedForSession(prefs);
        }
    },
    SEND_WELCOME(R.string.tutorial_hint_send_welcome, 0) {
        @Override
        boolean shouldShow(TutorialEvent event, HintType current, PreferencesHelper prefs, Bundle params) {
            return event == TutorialEvent.FRIEND_ADDED && current == null && params != null && params.containsKey(Tutorial.FRIEND_KEY);
        }

        @Override
        void show(TutorialLayout layout, View view, Tutorial tutorial, PreferencesHelper prefs) {
            layout.dimExceptForRect(getViewRect(view));
        }
    },
    SENT(R.string.tutorial_hint_sent, R.string.tutorial_got_it) {
        @Override
        boolean shouldShow(TutorialEvent event, HintType current, PreferencesHelper prefs, Bundle params) {
            return event == TutorialEvent.SENT_INDICATOR_SHOWED && hasOneFriend() && current == null && prefs.getBoolean(getPrefName(), true);
        }

        @Override
        void show(TutorialLayout layout, View view, Tutorial tutorial, PreferencesHelper prefs) {
            View indicator = view.findViewById(R.id.img_viewed); // we use viewed due to animation of uploading indicator at this moment
            layout.clear();
            layout.setArrowAnchorRect(getViewRect(indicator));
            layout.setExcludedRect(getViewRect(view));
            layout.dim();
            delayedDismiss(layout, this, tutorial);
            markHintAsShowed(prefs);
        }
    },
    VIEWED(R.string.tutorial_hint_viewed, R.string.tutorial_got_it) {
        @Override
        boolean shouldShow(TutorialEvent event, HintType current, PreferencesHelper prefs, Bundle params) {
            if (params != null && params.containsKey(Tutorial.BOX_KEY)) {
                if (params.getInt(Tutorial.BOX_KEY, -1) == NineViewGroup.Box.CENTER_RIGHT.ordinal()) {
                    return event == TutorialEvent.VIEWED_INDICATOR_SHOWED && current == null && prefs.getBoolean(getPrefName(), true);
                }
            }
            return false;
        }

        @Override
        void show(TutorialLayout layout, View view, Tutorial tutorial, PreferencesHelper prefs) {
            View indicator = view.findViewById(R.id.img_viewed);
            layout.clear();
            layout.setArrowAnchorRect(getViewRect(indicator));
            layout.setExcludedRect(getViewRect(view));
            layout.dim();
            delayedDismiss(layout, this, tutorial);
            markHintAsShowed(prefs);
        }
    },
    INVITE_2(0, 0) {
        @Override
        boolean shouldShow(TutorialEvent event, HintType current, PreferencesHelper prefs, Bundle params) {
            switch (event) {
                case HINT_DISMISSED:
                    if (params != null && params.containsKey(Tutorial.HINT_TYPE_KEY)) {
                        if (params.getInt(Tutorial.HINT_TYPE_KEY) != SENT.ordinal()) {
                            break;
                        }
                    }
                case MESSAGE_SENT:
                    boolean allViewed = IncomingVideoFactory.getFactoryInstance().allNotViewedCount() == 0;
                    boolean firstInSession = prefs.getBoolean(getPrefSessionName(), true);
                    return hasOneFriend() && firstInSession && allViewed && current == null;
            }
            return false;
        }

        @Override
        void show(TutorialLayout layout, View view, Tutorial tutorial, PreferencesHelper prefs) {
            NineViewGroup nineViewGroup = getNineViewGroup(view);
            if (nineViewGroup != null) {
                layout.clear();
                layout.setExcludedRect(getViewRect(nineViewGroup.getFrame(NineViewGroup.Box.TOP_RIGHT)));
                layout.setIcon(R.drawable.ic_feature_gift);
                layout.dim();
                markHintAsShowedForSession(prefs);
            }
        }

        @Override
        String getHint(Context context) {
            String[] array = context.getResources().getStringArray(R.array.tutorial_hint_invite_2);
            Random random = new Random();
            return array[Math.abs(random.nextInt(array.length))];
        }
    },
    NEXT_FEATURE_AFTER_UNLOCK(0, 0) {
        @Override
        boolean shouldShow(TutorialEvent event, HintType current, PreferencesHelper prefs, Bundle params) {
            switch (event) {
                case HINT_DISMISSED:
                    if (params != null && params.containsKey(Tutorial.HINT_TYPE_KEY)) {
                        return HintType.values()[params.getInt(Tutorial.HINT_TYPE_KEY)].isFeatureHint() && !Features.allFeaturesOpened(prefs);
                    }
                    break;
            }
            return false;
        }

        @Override
        void show(TutorialLayout layout, View view, Tutorial tutorial, PreferencesHelper prefs) {
        }
    },
    NEXT_FEATURE(0, 0) {
        @Override
        boolean shouldShow(TutorialEvent event, HintType current, PreferencesHelper prefs, Bundle params) {
            switch (event) {
                case MESSAGE_SENT:
                case VIDEO_VIEWED:
                    return current == null && prefs.getBoolean(getPrefSessionName(), true) && !Features.allFeaturesOpened(prefs);
            }
            return false;
        }

        @Override
        void show(TutorialLayout layout, View view, Tutorial tutorial, PreferencesHelper prefs) {
        }
    },
    ;

    private static NineViewGroup getNineViewGroup(View view) {
        NineViewGroup nineViewGroup = null;
        ViewParent parentView = view.getParent();
        while (nineViewGroup == null && parentView != null) {
            if (parentView instanceof NineViewGroup) {
                nineViewGroup = (NineViewGroup) parentView;
            } else {
                parentView = parentView.getParent();
            }
        }
        if (nineViewGroup == null && parentView == null) {
            nineViewGroup = ButterKnife.findById(view, R.id.grid_view);
        }
        return nineViewGroup;
    }

    private static final String SESSION = "_session";

    private String prefName;
    private int hintTextId;
    private int buttonTextId;
    private Features.Feature feature;
    HintType(int id, int buttonTextId) {
        prefName = "pref_hint_" + name().toLowerCase();
        hintTextId = id;
        this.buttonTextId = buttonTextId;
    }

    HintType(int id, int buttonTextId, Features.Feature feature) {
        this(id, buttonTextId);
        this.feature = feature;
    }

    public String getPrefName() {
        return prefName;
    }

    public String getPrefSessionName() {
        return prefName + SESSION;
    }

    boolean isFeatureHint() {
        return feature != null;
    }

    String getHint(Context context) {
        return context.getString(hintTextId);
    }

    String getHint(Context context, String... vars) {
        return context.getString(hintTextId, vars);
    }

    String getButtonText(Context context) {
        if (buttonTextId == 0) {
            return null;
        } else {
            return context.getString(buttonTextId);
        }
    }

    boolean shouldShow(TutorialEvent event, HintType current, PreferencesHelper prefs, Bundle params) {
        if (isFeatureHint() && event == TutorialEvent.FEATURE_AWARD_DISMISSED && params != null) {
            return params.getInt(Tutorial.FEATURE_KEY, -1) == feature.ordinal();
        }
        return false;
    }

    abstract void show(TutorialLayout layout, View view, Tutorial tutorial, PreferencesHelper prefs);

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

    public static HintType shouldShowHintByPriority(TutorialEvent event, HintType current, PreferencesHelper prefs, Bundle params) {
        for (HintType hint : values()) {
            if (hint.shouldShow(event, current, prefs, params)) {
                return hint;
            }
        }
        return null;
    }

    public void markHintAsShowed(PreferencesHelper preferences) {
        if (preferences.getBoolean(getPrefName(), true)) {
            preferences.putBoolean(getPrefName(), false);
        }
    }

    public void markHintAsShowedForSession(PreferencesHelper preferences) {
        if (preferences.getBoolean(getPrefSessionName(), true)) {
            preferences.putBoolean(getPrefSessionName(), false);
        }
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

    private static void delayedDismiss(final TutorialLayout layout, final HintType hint, final Tutorial tutorial) {
        layout.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (tutorial.getCurrent() == hint) {
                    layout.dismiss();
                }
            }
        }, 3500);
    }

    private static boolean hasOneFriend() {
        return FriendFactory.getFactoryInstance().count() == 1;
    }
}