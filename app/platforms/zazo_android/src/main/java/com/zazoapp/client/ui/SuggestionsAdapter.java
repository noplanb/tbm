package com.zazoapp.client.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.zazoapp.client.R;
import com.zazoapp.client.features.friendfinder.Suggestion;
import com.zazoapp.client.network.FriendFinderRequests;
import com.zazoapp.client.network.HttpRequest;
import com.zazoapp.client.ui.helpers.ThumbsHelper;
import com.zazoapp.client.ui.view.CircleThumbView;
import com.zazoapp.client.utilities.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 3/30/2016.
 */
class SuggestionsAdapter extends RecyclerView.Adapter<SuggestionsAdapter.ViewHolder> implements View.OnClickListener {
    private List<ContactSuggestion> suggestions;
    private LayoutInflater layoutInflater;
    private SuggestionsFragment fragment;

    private final ThumbsHelper tHelper;
    private static final int MAX_ITEMS = 10;
    private static final int LAST = 1;

    private OnItemStateChangedListener onItemStateChangedListener;

    interface OnItemStateChangedListener {
        void onItemStateChanged(int position, ContactSuggestion.State state);
    }

    void setOnItemStateChangedListener(OnItemStateChangedListener listener) {
        onItemStateChangedListener = listener;
    }

    interface OnPhoneItemSelected {
        void onPhoneItemSelected(int index);
    }

    @Override
    public void onClick(final View v) {
        if (fragment.isRefreshing()) {
            return;
        }
        final int position = (Integer) v.getTag(R.id.id);
        final ContactSuggestion item = suggestions.get(position);
        switch (v.getId()) {
            case R.id.add_btn: {
                if (item.hasMultiplePhones()) {
                    fragment.displayPhoneChooserPopup(new OnPhoneItemSelected() {
                        @Override
                        public void onPhoneItemSelected(int index) {
                            FriendFinderRequests.addFriend(new SuggestionRequestCallback(position, v, ContactSuggestion.State.ADDED), item.getId(), item.getPhone(index));
                        }
                    }, item, v);
                } else {
                    FriendFinderRequests.addFriend(new SuggestionRequestCallback(position, v, ContactSuggestion.State.ADDED), item.getId(), null);
                }
            }
            break;
            case R.id.ignore_btn: {
                FriendFinderRequests.ignoreFriend(new SuggestionRequestCallback(position, v, ContactSuggestion.State.IGNORED), item.getId());
            }
            break;
            case R.id.undo_btn: {
                setState(ContactSuggestion.State.NEW, v, position, true);
            }
            break;
        }
    }

    public void clear() {
        suggestions.clear();
        notifyDataSetChanged();
    }

    public SuggestionsAdapter(Context context, SuggestionsFragment suggestionsFragment) {
        layoutInflater = LayoutInflater.from(context);
        suggestions = new ArrayList<>();
        tHelper = new ThumbsHelper(context);
        fragment = suggestionsFragment;
    }

    @Override
    public int getItemViewType(int position) {
        return suggestions.size() == position + 1 ? LAST : 0;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;
        switch (viewType) {
            case LAST:
                itemView = layoutInflater.inflate(R.layout.suggestions_list_last_item, parent, false);
                break;
            default:
                itemView = layoutInflater.inflate(R.layout.suggestions_list_item, parent, false);
                break;
        }

        return new ViewHolder(itemView, this);
    }

    @Override
    public void onBindViewHolder(ViewHolder h, int position) {
        ContactSuggestion item = suggestions.get(position);
        h.name.setText(item.getName());
        h.addButton.setTag(h);
        h.addButton.setTag(R.id.id, position); // FIXME for random item inserting change to save item instead
        h.addButton.setOnClickListener(this);
        h.ignoreButton.setTag(h);
        h.ignoreButton.setTag(R.id.id, position);
        h.ignoreButton.setOnClickListener(this);
        h.undoButton.setTag(h);
        h.undoButton.setTag(R.id.id, position);
        h.undoButton.setOnClickListener(this);
        setState(item.state, h.addButton, position, false);
        h.progress.setVisibility(View.INVISIBLE);
    }

    @Override
    public int getItemCount() {
        return Math.min(suggestions.size(), MAX_ITEMS);
    }

    public int getRealCount() {
        return suggestions.size();
    }

    public void add(int location, ContactSuggestion suggestion) {
        suggestions.add(location, suggestion);
        if (location < MAX_ITEMS) {
            notifyItemInserted(location);
        }
    }

    public void remove(int location) {
        if (location >= suggestions.size())
            return;

        suggestions.remove(location);
        if (location < MAX_ITEMS) {
            notifyItemRemoved(location);
        }
        if (suggestions.size() >= MAX_ITEMS) {
            notifyItemInserted(MAX_ITEMS - 1);
        }
    }

    void setState(final ContactSuggestion.State state, final View v, final int position, boolean smooth) {
        final ContactSuggestion item = suggestions.get(position);
        final ViewHolder h = (ViewHolder) v.getTag();
        final int posId = (int) v.getTag(R.id.id);
        h.thumb.setImageResource(tHelper.getIcon(item.getName()));
        h.thumb.setFillColor(tHelper.getColor(item.getName()));
        h.thumbTitle.setText(StringUtils.getInitials(item.getName()));
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
            animator.setDuration(tHelper.getDuration());
            animator.setInterpolator(new AccelerateInterpolator());
            if (state != ContactSuggestion.State.IGNORED) {
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
                        h.leftLayout.setAlpha(tHelper.getAnimAlpha(value));
                        matrix.setSaturation(tHelper.getAnimSaturation(value));
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
            if (state != ContactSuggestion.State.IGNORED) {
                h.leftLayout.setAlpha(1f);
                h.thumb.setFillColorFilter(null);
            } else {
                h.leftLayout.setAlpha(tHelper.getMinAlpha());
                h.thumb.setFillColorFilter(tHelper.getDisabledFilter());
            }
        }
        if (smooth) {
            if (onItemStateChangedListener != null) {
                onItemStateChangedListener.onItemStateChanged(position, state);
            }
        }
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

        private SuggestionsAdapter adapter;

        public ViewHolder(View itemView, SuggestionsAdapter suggestionsAdapter) {
            super(itemView);
            ButterKnife.inject(this, itemView);
        }
    }

    static class ContactSuggestion extends Suggestion {
        int contactId;
        State state = State.NEW;

        ContactSuggestion(String name, int id) {
            super(name);
        }

        ContactSuggestion(FriendFinderRequests.SuggestionsData.Suggestion s) {
            super(s.getDisplayName(), s.getPhoneNumbers());
            this.contactId = s.getId();
        }

        enum State {
            NEW,
            ADDED,
            IGNORED,
            SYNCING
        }

        int getId() {
            return contactId;
        }
    }

    class SuggestionRequestCallback implements HttpRequest.Callbacks {

        private int position;
        private View view;
        private ContactSuggestion.State successState;

        SuggestionRequestCallback(int position, View v, ContactSuggestion.State successState) {
            this.position = position;
            this.view = v;
            this.successState = successState;
            if (position == (int) view.getTag(R.id.id)) {
                ViewHolder h = (ViewHolder) view.getTag();
                h.progress.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void success(String response) {
            fragment.onReceivedFriend(FriendFinderRequests.gotFriend(layoutInflater.getContext(), response));
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
