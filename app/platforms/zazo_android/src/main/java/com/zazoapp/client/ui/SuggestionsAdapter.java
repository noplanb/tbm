package com.zazoapp.client.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.ColorDrawable;
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
import com.zazoapp.client.network.FriendFinderRequests;
import com.zazoapp.client.network.HttpRequest;
import com.zazoapp.client.ui.view.CircleThumbView;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 3/30/2016.
 */
class SuggestionsAdapter extends RecyclerView.Adapter<SuggestionsAdapter.ViewHolder> implements View.OnClickListener {
    private List<Suggestion> suggestions;
    private LayoutInflater layoutInflater;
    private SuggestionsFragment fragment;

    private final int icons[] = {R.drawable.bgn_thumb_1, R.drawable.bgn_thumb_2, R.drawable.bgn_thumb_3, R.drawable.bgn_thumb_4};
    private final int colors[];
    private ColorMatrix grayedMatrix = new ColorMatrix();
    private ColorMatrixColorFilter disabledFilter;
    private ColorDrawable transparentDrawable = new ColorDrawable(Color.TRANSPARENT);

    private final float MIN_SATURATION = 0f;
    private final float MIN_ALPHA = 0.6f;
    private final long ANIM_DURATION = 300;
    private static final int MAX_ITEMS = 10;

    private OnItemStateChangedListener onItemStateChangedListener;

    interface OnItemStateChangedListener {
        void onItemStateChanged(int position, Suggestion.State state);
    }

    void setOnItemStateChangedListener(OnItemStateChangedListener listener) {
        onItemStateChangedListener = listener;
    }

    @Override
    public void onClick(final View v) {
        if (fragment.isRefreshing()) {
            return;
        }
        final int position = (Integer) v.getTag(R.id.id);
        final Suggestion item = suggestions.get(position);
        ViewHolder h = (ViewHolder) v.getTag();
        switch (v.getId()) {
            case R.id.add_btn: {
                h.progress.setVisibility(View.VISIBLE);
                FriendFinderRequests.addFriends(new SuggestionRequestCallback(position, v, Suggestion.State.ADDED), item.contactId);
            }
            break;
            case R.id.ignore_btn: {
                h.progress.setVisibility(View.VISIBLE);
                FriendFinderRequests.ignoreFriends(new SuggestionRequestCallback(position, v, Suggestion.State.IGNORED), item.contactId);
            }
            break;
            case R.id.undo_btn: {
                setState(Suggestion.State.NEW, v, position, true);
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
        colors = context.getResources().getIntArray(R.array.thumb_colors);
        grayedMatrix.setSaturation(MIN_SATURATION);
        disabledFilter = new ColorMatrixColorFilter(grayedMatrix);
        fragment = suggestionsFragment;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = layoutInflater.inflate(R.layout.suggestions_list_item, parent, false);
        return new ViewHolder(itemView, this);
    }

    @Override
    public void onBindViewHolder(ViewHolder h, int position) {
        Suggestion item = suggestions.get(position);
        h.name.setText(item.name);
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

    public void add(int location, Suggestion suggestion) {
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

    static class Suggestion {
        String name;
        Integer contactId;
        State state = State.NEW;

        Suggestion(String name, Integer contactId) {
            this.name = name;
            this.contactId = contactId;
        }

        enum State {
            NEW,
            ADDED,
            IGNORED,
            SYNCING
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
