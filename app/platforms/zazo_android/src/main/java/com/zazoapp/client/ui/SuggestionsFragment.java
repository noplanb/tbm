package com.zazoapp.client.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Optional;
import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.MaterialMenuView;
import com.zazoapp.client.R;
import com.zazoapp.client.core.IntentHandlerService;
import com.zazoapp.client.network.FriendFinderRequests;
import com.zazoapp.client.network.HttpRequest;
import com.zazoapp.client.ui.animations.SlideHorizontalFadeAnimation;
import com.zazoapp.client.ui.view.CircleThumbView;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.DialogShower;
import com.zazoapp.client.utilities.StringUtils;
import de.hdodenhof.circleimageview.CircleImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 8/14/2015.
 */
public class SuggestionsFragment extends ZazoTopFragment {

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

    private SuggestionsAdapter adapter;
    private SuggestionCardResult currentCardType = SuggestionCardResult.NONE;

    private static final String CARD_TYPE = "card_type";
    private static final String NAME = "name";
    private static final String NKEY = "nkey";


    private enum SuggestionCardResult {
        NONE(0),
        UNSUBSCRIBED(R.string.action_subscribe),
        IGNORED(R.string.action_undo),
        ADDED(R.string.action_got_it);

        private int actionId;

        SuggestionCardResult(@StringRes int action) {
            actionId = action;
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
                }
                bundle.putString(NAME, intent.getStringExtra(IntentHandlerService.FriendJoinedIntentFields.NAME));
                bundle.putString(NKEY, intent.getStringExtra(IntentHandlerService.FriendJoinedIntentFields.NKEY));
            }
        }
        SuggestionsFragment fragment = new SuggestionsFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.suggestions_layout, null);
        ButterKnife.inject(this, v);
        adapter = new SuggestionsAdapter(getContext());
        listView.setAdapter(adapter);
        listView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        up.setState(MaterialMenuDrawable.IconState.ARROW);
        showLastSuggestionCard(SuggestionCardResult.values()[getArguments().getInt(CARD_TYPE, 0)]);
        return v;
    }

    @OnClick({R.id.home, R.id.suggestion_action_btn, R.id.done_btn})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.home:
                dropSuggestionIntent();
                getFragmentManager().popBackStack();
                break;
            case R.id.done_btn:
                dropSuggestionIntent();
                getFragmentManager().popBackStack();
                // TODO do invite
                break;
        }
    }

    @OnClick({R.id.test_add, R.id.test_remove})
    public void testOnClick(View v) {
        switch (v.getId()) {
            case R.id.test_add:
                adapter.add(adapter.getItemCount(), new Suggestion("Test " + (adapter.getItemCount())));
                break;
            case R.id.test_remove:
                adapter.remove(adapter.getItemCount() - 1);
                break;
        }
    }

    private void dropSuggestionIntent() {
        Intent intent = getActivity().getIntent();
        if (intent != null && IntentHandlerService.IntentActions.SUGGESTIONS.equals(intent.getAction())) {
            intent.setAction(IntentHandlerService.IntentActions.NONE);
        }
    }

    @Override
    protected void onBackPressed() {
        super.onBackPressed();
        dropSuggestionIntent();
    }

    private void showLastSuggestionCard(SuggestionCardResult cardType) {
        String name = getArguments().getString(NAME);
        switch (cardType) {
            case UNSUBSCRIBED:
                unsubscribedLayout.setVisibility(View.VISIBLE);
                addedIgnoredLayout.setVisibility(View.GONE);
                lastSuggestionCard.setVisibility(View.VISIBLE);
                break;
            case IGNORED:
            case ADDED:
                boolean added = cardType == SuggestionCardResult.ADDED;
                unsubscribedLayout.setVisibility(View.GONE);
                addedIgnoredLayout.setVisibility(View.VISIBLE);
                suggestionName.setText(name);
                suggestionInfo.setText(added ? R.string.ff_add_success_message : R.string.ff_ignore_success_message);
                // TODO change mark
                lastSuggestionCard.setVisibility(View.VISIBLE);
                break;
            default:
                lastSuggestionCard.setVisibility(View.GONE);
                break;
        }
        suggestionActionButton.setText(cardType.actionId != 0 ? getString(0) : "");
        currentCardType = cardType;
    }

    private static class SuggestionsAdapter extends RecyclerView.Adapter<ViewHolder> implements View.OnClickListener {
        private List<Suggestion> suggestions;
        private OnItemClickListener onItemClickListener;
        private LayoutInflater layoutInflater;

        private final int icons[] = {R.drawable.bgn_thumb_1, R.drawable.bgn_thumb_2, R.drawable.bgn_thumb_3, R.drawable.bgn_thumb_4};
        private final int colors[];
        private ColorMatrix grayedMatrix = new ColorMatrix();
        private ColorMatrixColorFilter disabledFilter;
        private ColorDrawable transparentDrawable = new ColorDrawable(Color.TRANSPARENT);

        private final float MIN_SATURATION = 0f;
        private final float MIN_ALPHA = 0.6f;
        private final long ANIM_DURATION = 300;

        @Override
        public void onClick(final View v) {
            if (v.getId() == R.id.add_all_btn) {
                DialogShower.showToast(layoutInflater.getContext(), "Add All");
                return;
            }
            final int position = (Integer) v.getTag(R.id.id);
            final Suggestion item = suggestions.get(position);
            ViewHolder h = (ViewHolder) v.getTag();
            switch (v.getId()) {
                case R.id.add_btn: {
                    h.progress.setVisibility(View.VISIBLE);
                    FriendFinderRequests.testRequest(new SuggestionRequestCallback(position, v, Suggestion.State.ADDED));
                }
                break;
                case R.id.ignore_btn: {
                    h.progress.setVisibility(View.VISIBLE);
                    FriendFinderRequests.testRequest(new SuggestionRequestCallback(position, v, Suggestion.State.IGNORED));
                }
                break;
                case R.id.undo_btn: {
                    setState(Suggestion.State.NEW, v, position, true);
                }
                break;
            }
        }

        public interface OnItemClickListener{
            void onItemClick(ViewHolder item, int position, int actionId);
        }

        public SuggestionsAdapter(Context context){
            layoutInflater = LayoutInflater.from(context);
            suggestions = new ArrayList<>();
            colors = context.getResources().getIntArray(R.array.thumb_colors);
            grayedMatrix.setSaturation(MIN_SATURATION);
            disabledFilter = new ColorMatrixColorFilter(grayedMatrix);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView;
            switch (viewType) {
                case 1:
                    itemView = layoutInflater.inflate(R.layout.suggestions_first_list_item, parent, false);
                    break;
                default:
                    itemView = layoutInflater.inflate(R.layout.suggestions_list_item, parent, false);
                    break;
            }
            return new ViewHolder(itemView, this);
        }

        @Override
        public void onBindViewHolder(ViewHolder h, int position) {
            Suggestion item = suggestions.get(position);
            h.name.setText(item.name);
            h.addButton.setTag(h);
            h.addButton.setTag(R.id.id, position);
            h.addButton.setOnClickListener(this);
            h.ignoreButton.setTag(h);
            h.ignoreButton.setTag(R.id.id, position);
            h.ignoreButton.setOnClickListener(this);
            h.undoButton.setTag(h);
            h.undoButton.setTag(R.id.id, position);
            h.undoButton.setOnClickListener(this);
            if (getItemViewType(position) == 1) {
                h.addAllButton.setOnClickListener(this);
            }
            setState(item.state, h.addButton, position, false);
            h.progress.setVisibility(View.INVISIBLE);
        }

        @Override
        public int getItemCount() {
            return suggestions.size();
        }

        public void add(int location, Suggestion suggestion){
            suggestions.add(location, suggestion);
            notifyItemInserted(location);
        }

        public void remove(int location){
            if(location >= suggestions.size())
                return;

            suggestions.remove(location);
            notifyItemRemoved(location);
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? 1 : 2;
        }

        void setState(final Suggestion.State state, final View v, final int position, boolean smooth) {
            final Suggestion item = suggestions.get(position);
            final ViewHolder h = (ViewHolder) v.getTag();
            final int posId = (int) v.getTag(R.id.id);
            h.thumb.setImageResource(Convenience.getStringDependentItem(item.name, icons));
            h.thumb.setFillColor(Convenience.getStringDependentItem(item.name, colors));
            h.thumbTitle.setText(StringUtils.getInitials(item.name));
            item.state = state;
            switch (item.state) {
                case NEW:
                    h.addButton.setVisibility(View.VISIBLE);
                    h.ignoreButton.setVisibility(View.VISIBLE);
                    h.undoButton.setVisibility(View.INVISIBLE);
                    h.checkbox.setVisibility(View.INVISIBLE);
                    break;
                case ADDED:
                    h.addButton.setVisibility(View.GONE);
                    h.ignoreButton.setVisibility(View.GONE);
                    h.undoButton.setVisibility(View.INVISIBLE);
                    h.checkbox.setVisibility(View.VISIBLE);
                    break;
                case IGNORED:
                    h.addButton.setVisibility(View.GONE);
                    h.ignoreButton.setVisibility(View.GONE);
                    h.undoButton.setVisibility(View.VISIBLE);
                    h.checkbox.setVisibility(View.INVISIBLE);
                    break;
            }
            if (smooth) {
                ValueAnimator animator = new ValueAnimator();
                animator.setDuration(ANIM_DURATION);
                animator.setInterpolator(new AccelerateInterpolator());
                if (state != Suggestion.State.IGNORED) {
                    animator.setFloatValues(0f, 1f);
                } else {
                    animator.setFloatValues(1f, 0f);
                }
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    private ColorMatrix matrix = new ColorMatrix();
                    private ColorMatrixColorFilter filter;

                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        if (posId == position) {
                            float value = (float) animation.getAnimatedValue();
                            h.leftLayout.setAlpha(value * (1 - MIN_ALPHA) + MIN_ALPHA);
                            matrix.setSaturation(value * (1 - MIN_SATURATION) + MIN_SATURATION);
                            filter = new ColorMatrixColorFilter(matrix);
                            h.thumb.setFillColorFilter(filter);
                            h.thumb.invalidate();
                        } else {
                            animation.cancel();
                        }
                    }
                });
                animator.start();
            } else {
                if (state != Suggestion.State.IGNORED) {
                    h.leftLayout.setAlpha(1f);
                    h.thumb.setFillColorFilter(null);
                } else {
                    h.leftLayout.setAlpha(MIN_ALPHA);
                    h.thumb.setFillColorFilter(disabledFilter);
                }
            }

        }

        class SuggestionRequestCallback implements HttpRequest.Callbacks {

            private int position;
            private View view;
            private Suggestion.State successState;

            SuggestionRequestCallback(int position, View v, Suggestion.State successState) {
                this.position = position;
                this.view = v;
                this.successState = successState;
            }

            @Override
            public void success(String response) {
                finishRequest(true);
            }

            @Override
            public void error(String errorString) {
                finishRequest(false);
            }

            private void finishRequest(boolean success) {
                if (position == (int) view.getTag(R.id.id)) {
                    ViewHolder h = (ViewHolder) view.getTag();
                    h.progress.setVisibility(View.INVISIBLE);
                    if (success) {
                        setState(successState, view, position, true);
                    }
                }
            }
        }
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (nextAnim != R.anim.slide_left_fade_in && nextAnim != R.anim.slide_right_fade_out) {
            return super.onCreateAnimation(transit, enter, nextAnim);
        }
        return SlideHorizontalFadeAnimation.get(getActivity(), nextAnim);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        @InjectView(R.id.thumb) CircleThumbView thumb;
        @InjectView(R.id.thumb_title) TextView thumbTitle;
        @InjectView(R.id.add_btn) Button addButton;
        @InjectView(R.id.ignore_btn) Button ignoreButton;
        @InjectView(R.id.undo_btn) Button undoButton;
        @InjectView(R.id.checkbox) ImageView checkbox;
        @InjectView(R.id.name) TextView name;
        @InjectView(R.id.progress_layout) View progress;
        @InjectView(R.id.left_layout) View leftLayout;
        @Optional @InjectView(R.id.add_all_btn) Button addAllButton;

        private SuggestionsAdapter adpater;

        public ViewHolder(View itemView, SuggestionsAdapter suggestionsAdapter) {
            super(itemView);
            ButterKnife.inject(this, itemView);
        }
    }

    static class Suggestion {
        String name;
        String nkey;
        State state = State.NEW;

        Suggestion(String name) {
            this.name = name;
        }

        enum State {
            NEW,
            ADDED,
            IGNORED,
            SYNCING
        }
    }
}
