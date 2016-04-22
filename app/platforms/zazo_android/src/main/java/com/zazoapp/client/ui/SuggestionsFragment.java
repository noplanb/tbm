package com.zazoapp.client.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.MaterialMenuView;
import com.zazoapp.client.R;
import com.zazoapp.client.core.IntentHandlerService;
import com.zazoapp.client.network.FriendFinderRequests;
import com.zazoapp.client.network.HttpRequest;
import com.zazoapp.client.ui.animations.SlideHorizontalFadeAnimation;
import com.zazoapp.client.utilities.Convenience;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by skamenkovych@codeminders.com on 8/14/2015.
 */
public class SuggestionsFragment extends ZazoFragment implements SwipeRefreshLayout.OnRefreshListener, SuggestionsAdapter.OnItemStateChangedListener {

    private static final String TAG = SuggestionsFragment.class.getSimpleName();

    @InjectView(R.id.suggestions_list) RecyclerView listView;
    @InjectView(R.id.up) MaterialMenuView up;
    @InjectView(R.id.unsubscribed_layout) ViewGroup unsubscribedLayout;
    @InjectView(R.id.added_ignored_layout) ViewGroup addedIgnoredLayout;
    @InjectView(R.id.last_suggestion_card) ViewGroup lastSuggestionCard;
    @InjectView(R.id.last_suggestion_thumb) CircleImageView thumb;
    @InjectView(R.id.add_ignore_mark) ImageView addIgnoreMark;
    @InjectView(R.id.suggestion_name) TextView suggestionName;
    @InjectView(R.id.suggestion_info) TextView suggestionInfo;
    @InjectView(R.id.suggestion_action_btn) Button suggestionActionButton;
    @InjectView(R.id.progress) ProgressBar progressBar;
    @InjectView(R.id.swipe_container) SwipeRefreshLayout swipeRefreshLayout;
    @InjectView(R.id.multiple_action_btn_layout) ViewGroup multipleActionsButtonLayout;
    @InjectView(R.id.suggestion_action_third_btn) Button thirdButton;
    @InjectView(R.id.suggestion_action_second_btn) Button secondButton;
    @InjectView(R.id.suggestion_action_main_btn) Button mainButton;
    @InjectView(R.id.card_request_progress) ProgressBar cardRequestProgress;
    @InjectView(R.id.empty_suggestions) View emptySuggestions;
    @InjectView(R.id.fab) FloatingActionButton fab;

    private SuggestionsAdapter adapter;
    private SuggestionCardResult currentCardType = SuggestionCardResult.NONE;

    private static final String CARD_TYPE = "card_type";
    private static final String NAME = "name";
    private static final String NKEY = "nkey";
    static final String FROM_APPLICATION = "from_application";

    private boolean isAnyoneAdded = true;

    @Override
    public void onRefresh() {
        loadSuggestions();
    }

    public void setRefreshing(boolean refreshing) {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(refreshing);
        }
    }

    public boolean isRefreshing() {
        return swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing();
    }

    @Override
    public void onItemStateChanged(int position, SuggestionsAdapter.Suggestion.State state) {
        if (state == SuggestionsAdapter.Suggestion.State.ADDED) {
            moveToAnyoneAddedState();
        }
    }

    private void moveToAnyoneAddedState() {
        if (!isAnyoneAdded) {
            isAnyoneAdded = true;
            fab.setVisibility(View.VISIBLE);
            fab.animate().setInterpolator(new FastOutSlowInInterpolator()).scaleX(1).scaleY(1).start();
        }
    }

    private enum SuggestionCardResult {
        NONE(0),
        UNSUBSCRIBED(R.string.action_subscribe),
        IGNORED(R.string.action_undo),
        ADDED(R.string.action_got_it),
        NOTIFICATION(R.string.action_add_joined_friend, R.string.action_ignore_joined_friend, R.string.action_unsubscribe_joined_friend);

        private int actionId;
        private int[] extraActionIds;

        SuggestionCardResult(@StringRes int mainAction, @StringRes int... extraActions) {
            actionId = mainAction;
            extraActionIds = extraActions;
        }
    }

    @Deprecated public SuggestionsFragment() {}

    public static SuggestionsFragment getInstance(@Nullable Intent intent) {
        Bundle bundle = new Bundle();
        if (intent != null) {
            String action = intent.getStringExtra(IntentHandlerService.FriendJoinedIntentFields.ACTION);
            if (action != null) {
                switch (action) {
                    case IntentHandlerService.FriendJoinedActions.ADD:
                        bundle.putInt(CARD_TYPE, SuggestionCardResult.ADDED.ordinal());
                        break;
                    case IntentHandlerService.FriendJoinedActions.IGNORE:
                        bundle.putInt(CARD_TYPE, SuggestionCardResult.IGNORED.ordinal());
                        break;
                    case IntentHandlerService.FriendJoinedActions.UNSUBSCRIBE:
                        bundle.putInt(CARD_TYPE, SuggestionCardResult.UNSUBSCRIBED.ordinal());
                        break;
                    case IntentHandlerService.FriendJoinedActions.NOTIFY:
                        bundle.putInt(CARD_TYPE, SuggestionCardResult.NOTIFICATION.ordinal());
                        break;
                    default:
                        bundle.putInt(CARD_TYPE, SuggestionCardResult.NONE.ordinal());
                        break;
                }
                bundle.putString(NAME, intent.getStringExtra(IntentHandlerService.FriendJoinedIntentFields.NAME));
                bundle.putString(NKEY, intent.getStringExtra(IntentHandlerService.FriendJoinedIntentFields.NKEY));
                bundle.putBoolean(FROM_APPLICATION, false);
            }
        }
        //noinspection deprecation
        SuggestionsFragment fragment = new SuggestionsFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.suggestions_layout, null);
        ButterKnife.inject(this, v);
        adapter = new SuggestionsAdapter(getContext(), this);
        adapter.setOnItemStateChangedListener(this);
        listView.setAdapter(adapter);
        listView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        listView.setAlpha(0f);
        loadSuggestions();
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.thumb_color_blue, R.color.thumb_color_cyan, R.color.thumb_color_teal, R.color.thumb_color_deep_purple, R.color.thumb_color_indigo);
        if (fromApplication()) {
            up.setState(MaterialMenuDrawable.IconState.ARROW);
        } else {
            up.setVisibility(View.GONE);
            v.findViewById(R.id.home).setClickable(false);
        }
        cardRequestProgress.setInterpolator(new FastOutSlowInInterpolator());
        showLastSuggestionCard(SuggestionCardResult.values()[getArguments().getInt(CARD_TYPE, 0)]);
        return v;
    }

    private void loadSuggestions() {
        FriendFinderRequests.getSuggestions(new FriendFinderRequests.SuggestionsCallback() {
            @Override
            public void onReceivedSuggestions(FriendFinderRequests.SuggestionsData data) {
                if (swipeRefreshLayout == null || adapter == null) {
                    return;
                }
                swipeRefreshLayout.setRefreshing(false);
                doListViewAppearing();
                if (data != null) {
                    adapter.clear();
                    for (FriendFinderRequests.SuggestionsData.Suggestion suggestion : data.getSuggestions()) {
                        adapter.add(adapter.getItemCount(), new SuggestionsAdapter.Suggestion(suggestion.getDisplayName(), suggestion.getId()));
                    }
                    if (adapter.getItemCount() == 0) {
                        emptySuggestions.setVisibility(View.VISIBLE);
                    } else {
                        emptySuggestions.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
    }

    @OnClick({R.id.home, R.id.done_btn, R.id.fab, R.id.suggestion_action_btn,
            R.id.suggestion_action_main_btn, R.id.suggestion_action_second_btn,
            R.id.suggestion_action_third_btn})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.home:
            case R.id.done_btn:
            case R.id.fab:
                finishInvitation();
                break;
            case R.id.suggestion_action_btn:
                switch (currentCardType.actionId) {
                    case R.string.action_subscribe:
                        FriendFinderRequests.subscribe(getArguments().getString(NKEY), new CardLayoutRequestCallback(SuggestionCardResult.NOTIFICATION));
                        break;
                    case R.string.action_undo:
                        showLastSuggestionCard(SuggestionCardResult.NOTIFICATION);
                        break;
                    default:
                        hideLastSuggestionCard();
                        dropSuggestionIntent();
                        break;
                }
                break;
            case R.id.suggestion_action_main_btn:
                FriendFinderRequests.addFriend(getArguments().getString(NKEY), new CardLayoutRequestCallback(SuggestionCardResult.ADDED));
                cardRequestProgress.setVisibility(View.VISIBLE);
                break;
            case R.id.suggestion_action_second_btn:
                FriendFinderRequests.ignoreFriend(getArguments().getString(NKEY), new CardLayoutRequestCallback(SuggestionCardResult.IGNORED));
                break;
            case R.id.suggestion_action_third_btn:
                FriendFinderRequests.unsubscribe(getArguments().getString(NKEY), new CardLayoutRequestCallback(SuggestionCardResult.UNSUBSCRIBED));
                break;
        }
    }

    private void finishInvitation() {
        dropSuggestionIntent();
        if (isAnyoneAdded) {
            Activity activity = getActivity();
            if (activity != null) {
                Intent intent = new Intent(activity, WelcomeScreenActivity.class);
                intent.putExtra(FROM_APPLICATION, fromApplication());
                // TODO add friends to intent
                activity.startActivity(intent);
                activity.finish();
            }
        } else {
            super.onKeyDown(KeyEvent.KEYCODE_BACK, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
            if (!fromApplication()) {
                publishResult(0, null);
            }
        }
    }

    private void hideLastSuggestionCard() {
        ValueAnimator slideAnim = ValueAnimator.ofInt(0, lastSuggestionCard.getHeight());
        slideAnim.setDuration(400);
        slideAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                showLastSuggestionCard(SuggestionCardResult.NONE);
            }
        });
        slideAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                swipeRefreshLayout.setTranslationY(lastSuggestionCard.getHeight()-value);
            }
        });
        slideAnim.start();
        RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) swipeRefreshLayout.getLayoutParams();
        p.height = ViewGroup.LayoutParams.MATCH_PARENT;
        p.addRule(RelativeLayout.BELOW, R.id.zazo_action_bar);
        swipeRefreshLayout.setLayoutParams(p);
    }

    @OnClick({R.id.test_add, R.id.test_remove})
    public void testOnClick(View v) {
        switch (v.getId()) {
            case R.id.test_add:
                adapter.add(adapter.getItemCount(), new SuggestionsAdapter.Suggestion("Test " + (adapter.getRealCount()), -1));
                break;
            case R.id.test_remove:
                adapter.remove(adapter.getItemCount() - 1);
                break;
        }
    }

    private void dropSuggestionIntent() {
        Activity activity = getActivity();
        if (activity != null) {
            Intent intent = activity.getIntent();
            if (intent != null && IntentHandlerService.IntentActions.SUGGESTIONS.equals(intent.getAction())) {
                intent.setAction(IntentHandlerService.IntentActions.NONE);
            }
        }
    }

    private void showLastSuggestionCard(SuggestionCardResult cardType) {
        String name = getArguments().getString(NAME);
        switch (cardType) {
            case UNSUBSCRIBED:
                unsubscribedLayout.setVisibility(View.VISIBLE);
                addedIgnoredLayout.setVisibility(View.GONE);
                lastSuggestionCard.setVisibility(View.VISIBLE);
                multipleActionsButtonLayout.setVisibility(View.GONE);
                break;
            case IGNORED:
            case ADDED:
                boolean added = cardType == SuggestionCardResult.ADDED;
                if (added) {
                    moveToAnyoneAddedState();
                }
                unsubscribedLayout.setVisibility(View.GONE);
                addedIgnoredLayout.setVisibility(View.VISIBLE);
                suggestionName.setText(name);
                suggestionInfo.setText(added ? getString(R.string.ff_add_success_message, name) : getString(R.string.ff_ignore_success_message, name));
                addIgnoreMark.setEnabled(added);
                addIgnoreMark.setVisibility(View.VISIBLE);
                lastSuggestionCard.setVisibility(View.VISIBLE);
                multipleActionsButtonLayout.setVisibility(View.GONE);
                break;
            case NOTIFICATION:
                multipleActionsButtonLayout.setVisibility(View.VISIBLE);
                unsubscribedLayout.setVisibility(View.GONE);
                addedIgnoredLayout.setVisibility(View.VISIBLE);
                suggestionName.setText(name);
                suggestionInfo.setText(getString(R.string.new_friend_joined, name));
                mainButton.setText(R.string.action_add_joined_friend);
                secondButton.setText(R.string.action_ignore_joined_friend);
                thirdButton.setText(R.string.action_unsubscribe_joined_friend);
                addIgnoreMark.setVisibility(View.INVISIBLE);
                break;
            default:
                lastSuggestionCard.setVisibility(View.GONE);
                break;
        }
        suggestionActionButton.setText(cardType.actionId != 0 ? getString(cardType.actionId) : "");
        currentCardType = cardType;
    }

    private void doListViewAppearing() {
        if (listView != null && listView.getAlpha() == 0) {
            final float offset = Convenience.dpToPx(listView.getContext(), 50);
            progressBar.animate().alpha(0).translationY(-offset).setListener(
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            if (listView != null) {
                                listView.setTranslationY(offset);
                                listView.setVisibility(View.VISIBLE);
                                listView.setAlpha(0f);
                                listView.animate().alpha(1f).translationY(0).start();
                            }
                        }
                    }).start();
        }
    }

    private boolean fromApplication() {
        return getArguments().getBoolean(FROM_APPLICATION, true);
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (nextAnim != R.anim.slide_left_fade_in && nextAnim != R.anim.slide_right_fade_out) {
            return super.onCreateAnimation(transit, enter, nextAnim);
        }
        return SlideHorizontalFadeAnimation.get(getActivity(), nextAnim);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finishInvitation();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private class CardLayoutRequestCallback implements HttpRequest.Callbacks {

        SuggestionCardResult successCard;

        CardLayoutRequestCallback(SuggestionCardResult cardType) {
            successCard = cardType;
            cardRequestProgress.setVisibility(View.VISIBLE);
            setButtonsEnabled(false);
        }

        @Override
        public void success(String response) {
            cardRequestProgress.setVisibility(View.INVISIBLE);
            showLastSuggestionCard(successCard);
            setButtonsEnabled(true);
        }

        @Override
        public void error(String errorString) {
            cardRequestProgress.setVisibility(View.INVISIBLE);
            setButtonsEnabled(true);
        }

        private void setButtonsEnabled(boolean enable) {
            suggestionActionButton.setEnabled(enable);
            mainButton.setEnabled(enable);
            secondButton.setEnabled(enable);
            thirdButton.setEnabled(enable);
        }
    }
}
