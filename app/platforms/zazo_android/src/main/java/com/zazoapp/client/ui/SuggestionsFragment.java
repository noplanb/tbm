package com.zazoapp.client.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import com.zazoapp.client.features.friendfinder.Suggestion;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.network.FriendFinderRequests;
import com.zazoapp.client.network.HttpRequest;
import com.zazoapp.client.notification.NotificationSuggestion;
import com.zazoapp.client.ui.animations.SlideHorizontalFadeAnimation;
import com.zazoapp.client.utilities.Convenience;
import de.hdodenhof.circleimageview.CircleImageView;

import java.util.ArrayList;

/**
 * Created by skamenkovych@codeminders.com on 8/14/2015.
 */
public class SuggestionsFragment extends ZazoFragment implements SwipeRefreshLayout.OnRefreshListener, SuggestionsAdapter.OnItemStateChangedListener {

    private static final String TAG = SuggestionsFragment.class.getSimpleName();

    @InjectView(R.id.suggestions_list) RecyclerView listView;
    @InjectView(R.id.up) MaterialMenuView up;
    @InjectView(R.id.action_bar_icon) ImageView actionBarIcon;
    @InjectView(R.id.action_bar_title) TextView actionBarTitle;
    @InjectView(R.id.unsubscribed_layout) ViewGroup unsubscribedLayout;
    @InjectView(R.id.added_ignored_layout) ViewGroup addedIgnoredLayout;
    @InjectView(R.id.last_suggestion_card) ViewGroup lastSuggestionCard;
    @InjectView(R.id.last_suggestion_thumb) CircleImageView thumb;
    @InjectView(R.id.add_ignore_mark) ImageView addIgnoreMark;
    @InjectView(R.id.suggestion_name) TextView suggestionName;
    @InjectView(R.id.suggestion_info) TextView suggestionInfo;
    @InjectView(R.id.subscription_label) TextView subscriptionLabel;
    @InjectView(R.id.suggestion_action_btn) Button suggestionActionButton;
    @InjectView(R.id.progress) ProgressBar progressBar;
    @InjectView(R.id.swipe_container) SwipeRefreshLayout swipeRefreshLayout;
    @InjectView(R.id.support_swipe_container) View swipeSupportLayout;
    @InjectView(R.id.multiple_action_btn_layout) ViewGroup multipleActionsButtonLayout;
    @InjectView(R.id.suggestion_action_third_btn) Button thirdButton;
    @InjectView(R.id.suggestion_action_second_btn) Button secondButton;
    @InjectView(R.id.suggestion_action_main_btn) Button mainButton;
    @InjectView(R.id.card_request_progress) ProgressBar cardRequestProgress;
    @InjectView(R.id.empty_suggestions) View emptySuggestions;
    @InjectView(R.id.empty_suggestions_text) TextView emptySuggestionsText;
    @InjectView(R.id.fab) FloatingActionButton fab;

    private SuggestionsAdapter adapter;
    private SuggestionCardType currentCardType = SuggestionCardType.NONE;
    private NotificationSuggestion suggestion;
    private Context context;

    private static final String CARD_TYPE = "card_type";
    private static final String NOTIFICATION_DATA = "notification_data";
    private static final String SUBACTION = "subaction";
    private static final String CHOSEN_PHONE = "chosen_phone";
    static final String FROM_APPLICATION = "from_application";
    static final String ADDED_FRIENDS = "added_friends";
    static final String BACK_TO_APP_ON_FINISH = "back_to_app_on_finish";
    private static final String IS_ANYONE_ADDED = "is_anyone_added";

    private boolean isAnyoneAdded = false;
    private ArrayList<String> friendIds = new ArrayList<>();

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
    public void onItemStateChanged(int position, SuggestionsAdapter.ContactSuggestion.State state) {
        if (state == SuggestionsAdapter.ContactSuggestion.State.ADDED) {
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

    private enum SuggestionCardType {
        NONE(0),
        UNSUBSCRIBED(R.string.action_subscribe),
        IGNORED(R.string.action_undo),
        ADDED(R.string.action_got_it),
        NOTIFICATION(R.string.action_add_joined_friend, R.string.action_ignore_joined_friend, R.string.action_unsubscribe),
        SUBSCRIPTION_ON(R.string.action_unsubscribe),
        SUBSCRIPTION_OFF(R.string.action_subscribe);

        private int actionId;
        private int[] extraActionIds;

        SuggestionCardType(@StringRes int mainAction, @StringRes int... extraActions) {
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
                        bundle.putInt(CARD_TYPE, SuggestionCardType.ADDED.ordinal());
                        break;
                    case IntentHandlerService.FriendJoinedActions.IGNORE:
                        bundle.putInt(CARD_TYPE, SuggestionCardType.IGNORED.ordinal());
                        break;
                    case IntentHandlerService.FriendJoinedActions.UNSUBSCRIBE:
                        bundle.putInt(CARD_TYPE, SuggestionCardType.UNSUBSCRIBED.ordinal());
                        break;
                    case IntentHandlerService.FriendJoinedActions.NOTIFY:
                        bundle.putInt(CARD_TYPE, SuggestionCardType.NOTIFICATION.ordinal());
                        bundle.putString(SUBACTION, intent.getStringExtra(IntentHandlerService.FriendJoinedIntentFields.SUBACTION));
                        bundle.putString(CHOSEN_PHONE, intent.getStringExtra(IntentHandlerService.FriendJoinedIntentFields.CHOSEN_PHONE));
                        break;
                    default:
                        bundle.putInt(CARD_TYPE, SuggestionCardType.NONE.ordinal());
                        break;
                }
                bundle.putParcelable(NOTIFICATION_DATA, intent.getParcelableExtra(IntentHandlerService.FriendJoinedIntentFields.DATA));
                bundle.putBoolean(FROM_APPLICATION, false);
                bundle.putBoolean(BACK_TO_APP_ON_FINISH, intent.getBooleanExtra(BACK_TO_APP_ON_FINISH, false));
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
        context = inflater.getContext();
        ButterKnife.inject(this, v);
        adapter = new SuggestionsAdapter(getContext(), this);
        adapter.setOnItemStateChangedListener(this);
        listView.setAdapter(adapter);
        listView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        listView.setAlpha(0f);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.thumb_color_blue, R.color.thumb_color_cyan, R.color.thumb_color_teal, R.color.thumb_color_deep_purple, R.color.thumb_color_indigo);
        if (fromApplication()) {
            up.setState(MaterialMenuDrawable.IconState.ARROW);
            actionBarTitle.setVisibility(View.VISIBLE);
            actionBarIcon.setVisibility(View.GONE);
        } else {
            up.setVisibility(View.GONE);
            actionBarTitle.setVisibility(View.GONE);
            actionBarIcon.setVisibility(View.VISIBLE);
            v.findViewById(R.id.home).setClickable(false);
        }
        cardRequestProgress.setInterpolator(new FastOutSlowInInterpolator());
        suggestion = getArguments().getParcelable(NOTIFICATION_DATA);
        showLastSuggestionCard(SuggestionCardType.values()[getArguments().getInt(CARD_TYPE, 0)]);
        loadSuggestions();
        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            friendIds = savedInstanceState.getStringArrayList(ADDED_FRIENDS);
            if (savedInstanceState.getBoolean(IS_ANYONE_ADDED)) {
                moveToAnyoneAddedState();
            }
        }
    }

    private void loadSuggestions() {
        if (currentCardType == SuggestionCardType.NONE) {
            FriendFinderRequests.getSubscriptionsState(new FriendFinderRequests.SubscriptionsStateCallback() {
                @Override
                public void onReceivedSubscriptionsState(FriendFinderRequests.SubscriptionState state) {
                    switch (state) {
                        case UNSUBSCRIBED:
                            showLastSuggestionCard(SuggestionCardType.SUBSCRIPTION_OFF);
                            break;
                    }
                }
            });
        }
        FriendFinderRequests.getSuggestions(new FriendFinderRequests.SuggestionsCallback() {
            @Override
            public void onReceivedSuggestions(FriendFinderRequests.SuggestionsData data) {
                if (swipeRefreshLayout == null || adapter == null) {
                    return;
                }
                swipeRefreshLayout.setRefreshing(false);
                if (data != null) {
                    adapter.clear();
                    for (FriendFinderRequests.SuggestionsData.Suggestion suggestion : data.getSuggestions()) {
                        adapter.add(adapter.getItemCount(), new SuggestionsAdapter.ContactSuggestion(suggestion));
                    }
                    if (adapter.getItemCount() == 0) {
                        emptySuggestions.setVisibility(View.VISIBLE);
                        emptySuggestionsText.setText(R.string.suggestions_empty);
                        listView.setVisibility(View.VISIBLE);
                        listView.setAlpha(0f);
                    } else {
                        emptySuggestions.setVisibility(View.INVISIBLE);
                    }
                } else {
                    adapter.clear();
                    emptySuggestions.setVisibility(View.VISIBLE);
                    emptySuggestionsText.setText(R.string.suggestions_cant_load);
                    listView.setVisibility(View.VISIBLE);
                    listView.setAlpha(0f);
                }
                doListViewAppearing();
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
                switch (currentCardType) {
                    case UNSUBSCRIBED:
                        FriendFinderRequests.subscribe(new CardLayoutRequestCallback(SuggestionCardType.NOTIFICATION));
                        break;
                    case IGNORED:
                        showLastSuggestionCard(SuggestionCardType.NOTIFICATION);
                        break;
                    case SUBSCRIPTION_OFF:
                        FriendFinderRequests.subscribe(new CardLayoutRequestCallback(SuggestionCardType.SUBSCRIPTION_ON));
                        break;
                    case SUBSCRIPTION_ON:
                        FriendFinderRequests.unsubscribe(new CardLayoutRequestCallback(SuggestionCardType.SUBSCRIPTION_OFF));
                        break;
                    default:
                        hideLastSuggestionCard();
                        dropSuggestionIntent();
                        break;
                }
                break;
            case R.id.suggestion_action_main_btn:
                if (suggestion.hasMultiplePhones()) {
                    displayPhoneChooserPopup(new SuggestionsAdapter.OnPhoneItemSelected() {
                        @Override
                        public void onPhoneItemSelected(int index) {
                            addFriendFromNotification(suggestion.getPhone(index));
                        }
                    }, suggestion, v);
                } else {
                    addFriendFromNotification(null);
                }
                break;
            case R.id.suggestion_action_second_btn:
                ignoreFriendFromNotification();
                break;
            case R.id.suggestion_action_third_btn:
                unsubscribe();
                break;
        }
    }

    private void unsubscribe() {
        FriendFinderRequests.unsubscribe(new CardLayoutRequestCallback(SuggestionCardType.UNSUBSCRIBED));
    }

    public void ignoreFriendFromNotification() {
        FriendFinderRequests.ignoreFriend(suggestion.getNkey(), new CardLayoutRequestCallback(SuggestionCardType.IGNORED));
    }

    public void addFriendFromNotification(String phone) {
        FriendFinderRequests.addFriend(suggestion.getNkey(), new CardLayoutRequestCallback(SuggestionCardType.ADDED), phone);
        cardRequestProgress.setVisibility(View.VISIBLE);
    }

    private void finishInvitation() {
        dropSuggestionIntent();
        if (isAnyoneAdded) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
                Intent intent = new Intent(activity, WelcomeScreenActivity.class);
                intent.putExtra(FROM_APPLICATION, fromApplication() || backToApp());
                intent.putStringArrayListExtra(ADDED_FRIENDS, friendIds);
                activity.startActivity(intent);
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
                showLastSuggestionCard(SuggestionCardType.NONE);
            }
        });
        slideAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                swipeRefreshLayout.setTranslationY(lastSuggestionCard.getHeight()-value);
                swipeSupportLayout.setTranslationY(lastSuggestionCard.getHeight()-value);
            }
        });
        slideAnim.start();
        attachSwipeLayoutTo(R.id.zazo_action_bar);
    }

    private void appearLastSuggestionCard() {
        ValueAnimator slideAnim = ValueAnimator.ofInt(lastSuggestionCard.getHeight(), 0);
        slideAnim.setDuration(400);
        slideAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                swipeRefreshLayout.setTranslationY(0);
                swipeSupportLayout.setTranslationY(0);
                attachSwipeLayoutTo(R.id.last_suggestion_card);
            }
        });
        slideAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                swipeRefreshLayout.setTranslationY(lastSuggestionCard.getHeight()-value);
                swipeSupportLayout.setTranslationY(lastSuggestionCard.getHeight()-value);
            }
        });
        slideAnim.start();
    }

    private void attachSwipeLayoutTo(int id) {
        RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) swipeRefreshLayout.getLayoutParams();
        p.height = ViewGroup.LayoutParams.MATCH_PARENT;
        p.addRule(RelativeLayout.BELOW, id);
        swipeRefreshLayout.setLayoutParams(p);
        p = (RelativeLayout.LayoutParams) swipeSupportLayout.getLayoutParams();
        p.height = ViewGroup.LayoutParams.MATCH_PARENT;
        p.addRule(RelativeLayout.BELOW, id);
        swipeSupportLayout.setLayoutParams(p);
    }

    @OnClick({R.id.test_add, R.id.test_remove})
    public void testOnClick(View v) {
        switch (v.getId()) {
            case R.id.test_add:
                adapter.add(adapter.getItemCount(), new SuggestionsAdapter.ContactSuggestion("Test " + (adapter.getRealCount()), -1));
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putStringArrayList(ADDED_FRIENDS, friendIds);
        super.onSaveInstanceState(outState);
    }

    private void showLastSuggestionCard(SuggestionCardType cardType) {
        switch (cardType) {
            case UNSUBSCRIBED:
                unsubscribedLayout.setVisibility(View.VISIBLE);
                addedIgnoredLayout.setVisibility(View.GONE);
                lastSuggestionCard.setVisibility(View.VISIBLE);
                multipleActionsButtonLayout.setVisibility(View.GONE);
                break;
            case IGNORED:
            case ADDED: {
                boolean added = cardType == SuggestionCardType.ADDED;
                if (added) {
                    moveToAnyoneAddedState();
                }
                unsubscribedLayout.setVisibility(View.GONE);
                addedIgnoredLayout.setVisibility(View.VISIBLE);
                String name = suggestion.getName();
                suggestionName.setText(name);
                suggestionInfo.setText(added ? context.getString(R.string.ff_add_success_message, name) : context.getString(R.string.ff_ignore_success_message, name));
                addIgnoreMark.setEnabled(added);
                addIgnoreMark.setVisibility(View.VISIBLE);
                lastSuggestionCard.setVisibility(View.VISIBLE);
                multipleActionsButtonLayout.setVisibility(View.GONE);
            }
                break;
            case NOTIFICATION: {
                multipleActionsButtonLayout.setVisibility(View.VISIBLE);
                unsubscribedLayout.setVisibility(View.GONE);
                addedIgnoredLayout.setVisibility(View.VISIBLE);
                String name = suggestion.getName();
                suggestionName.setText(name);
                suggestionInfo.setText(context.getString(R.string.new_friend_suggestion, name));
                mainButton.setText(R.string.action_add_joined_friend);
                secondButton.setText(R.string.action_ignore_joined_friend);
                thirdButton.setText(R.string.action_unsubscribe);
                addIgnoreMark.setVisibility(View.INVISIBLE);
                String subaction = getArguments().getString(SUBACTION);
                if (subaction != null) {
                    switch (subaction) {
                        case IntentHandlerService.FriendJoinedActions.ADD:
                            addFriendFromNotification(getArguments().getString(CHOSEN_PHONE));
                            break;
                        case IntentHandlerService.FriendJoinedActions.IGNORE:
                            ignoreFriendFromNotification();
                            break;
                        case IntentHandlerService.FriendJoinedActions.UNSUBSCRIBE:
                            unsubscribe();
                            break;
                    }
                    getArguments().putString(SUBACTION, null);
                }
            }
                break;
            case SUBSCRIPTION_OFF:
            case SUBSCRIPTION_ON:
                boolean on = cardType == SuggestionCardType.SUBSCRIPTION_ON;
                unsubscribedLayout.setVisibility(View.VISIBLE);
                subscriptionLabel.setText(on ? R.string.ff_subscribed_message : R.string.ff_unsubscribed_message);
                addedIgnoredLayout.setVisibility(View.GONE);
                lastSuggestionCard.setVisibility(View.VISIBLE);
                multipleActionsButtonLayout.setVisibility(View.GONE);
                if (currentCardType == SuggestionCardType.NONE) {
                    appearLastSuggestionCard();
                }
                break;
            default:
                lastSuggestionCard.setVisibility(View.INVISIBLE);
                attachSwipeLayoutTo(R.id.zazo_action_bar);
                break;
        }
        suggestionActionButton.setText(cardType.actionId != 0 ? context.getString(cardType.actionId) : "");
        currentCardType = cardType;
    }

    private void doListViewAppearing() {
        if (listView != null && (listView.getVisibility() == View.INVISIBLE || listView.getAlpha() < 1)) {
            final float offset = Convenience.dpToPx(listView.getContext(), 50);
            progressBar.animate().alpha(0).translationY(-offset).setListener(
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            if (listView != null && adapter != null && adapter.getItemCount() > 0) {
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

    private boolean backToApp() {
        return getArguments().getBoolean(BACK_TO_APP_ON_FINISH, false);
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

        SuggestionCardType successCard;

        CardLayoutRequestCallback(SuggestionCardType cardType) {
            successCard = cardType;
            cardRequestProgress.setVisibility(View.VISIBLE);
            setButtonsEnabled(false);
        }

        @Override
        public void success(String response) {
            if (successCard == SuggestionCardType.ADDED) {
                onReceivedFriend(FriendFinderRequests.gotFriend(context, response));
            }
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

    public void displayPhoneChooserPopup(@NonNull final SuggestionsAdapter.OnPhoneItemSelected finishAction,
                                         @NonNull Suggestion suggestion, @NonNull View anchor) {
        displayPhoneChooserPopup(finishAction, suggestion, anchor, anchor.getContext());
    }

    public static void displayPhoneChooserPopup(@NonNull final SuggestionsAdapter.OnPhoneItemSelected finishAction,
                                         @NonNull Suggestion suggestion, @NonNull View anchor, @NonNull Context context) {
        final ListPopupWindow listPopupWindow = new ListPopupWindow(context);
        listPopupWindow.setAdapter(new ArrayAdapter<>(context, R.layout.phone_popup_list_item, R.id.phone, suggestion.getPhones()));
        listPopupWindow.setContentWidth(Convenience.dpToPx(context, 170));
        listPopupWindow.setDropDownGravity(Gravity.END);
        listPopupWindow.setAnchorView(anchor);
        listPopupWindow.setModal(true);
        listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int phoneIndex, long id) {
                finishAction.onPhoneItemSelected(phoneIndex);
                listPopupWindow.dismiss();
            }
        });
        listPopupWindow.show();
    }

    public void onReceivedFriend(Friend friend) {
        if (friend != null && friend != Friend.EMPTY) {
            friendIds.add(friend.getId());
        }
    }
}
