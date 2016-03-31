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
import com.zazoapp.client.R;
import com.zazoapp.client.network.FriendFinderRequests;
import com.zazoapp.client.network.HttpRequest;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 3/30/2016.
 */
class SuggestionsAdapter extends RecyclerView.Adapter<SuggestionsFragment.ViewHolder> implements View.OnClickListener {
    private List<SuggestionsFragment.Suggestion> suggestions;
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

    @Override
    public void onClick(final View v) {
        if (v.getId() == R.id.add_all_btn) {
            List<Integer> contactIds = new ArrayList<>();
            for (SuggestionsFragment.Suggestion suggestion : suggestions) {
                if (suggestion.state != SuggestionsFragment.Suggestion.State.IGNORED) {
                    contactIds.add(suggestion.contactId);
                }
            }
            if (!contactIds.isEmpty()) {
                Integer[] ids = new Integer[contactIds.size()];
                for (int i = 0; i < ids.length; i++) {
                    ids[i] = contactIds.get(i);
                }
                FriendFinderRequests.addFriends(new HttpRequest.Callbacks() {
                    @Override
                    public void success(String response) {
                        fragment.onRefresh();
                    }

                    @Override
                    public void error(String errorString) {
                    }
                }, ids);
            }
            return;
        }
        final int position = (Integer) v.getTag(R.id.id);
        final SuggestionsFragment.Suggestion item = suggestions.get(position);
        SuggestionsFragment.ViewHolder h = (SuggestionsFragment.ViewHolder) v.getTag();
        switch (v.getId()) {
            case R.id.add_btn: {
                h.progress.setVisibility(View.VISIBLE);
                FriendFinderRequests.addFriends(new SuggestionRequestCallback(position, v, SuggestionsFragment.Suggestion.State.ADDED), item.contactId);
            }
            break;
            case R.id.ignore_btn: {
                h.progress.setVisibility(View.VISIBLE);
                FriendFinderRequests.ignoreFriends(new SuggestionRequestCallback(position, v, SuggestionsFragment.Suggestion.State.IGNORED), item.contactId);
            }
            break;
            case R.id.undo_btn: {
                setState(SuggestionsFragment.Suggestion.State.NEW, v, position, true);
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
    public SuggestionsFragment.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;
        switch (viewType) {
            case 1:
                itemView = layoutInflater.inflate(R.layout.suggestions_first_list_item, parent, false);
                break;
            default:
                itemView = layoutInflater.inflate(R.layout.suggestions_list_item, parent, false);
                break;
        }
        return new SuggestionsFragment.ViewHolder(itemView, this);
    }

    @Override
    public void onBindViewHolder(SuggestionsFragment.ViewHolder h, int position) {
        SuggestionsFragment.Suggestion item = suggestions.get(position);
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

    public void add(int location, SuggestionsFragment.Suggestion suggestion) {
        suggestions.add(location, suggestion);
        notifyItemInserted(location);
    }

    public void remove(int location) {
        if (location >= suggestions.size())
            return;

        suggestions.remove(location);
        notifyItemRemoved(location);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? 1 : 2;
    }

    void setState(final SuggestionsFragment.Suggestion.State state, final View v, final int position, boolean smooth) {
        final SuggestionsFragment.Suggestion item = suggestions.get(position);
        final SuggestionsFragment.ViewHolder h = (SuggestionsFragment.ViewHolder) v.getTag();
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
            if (state != SuggestionsFragment.Suggestion.State.IGNORED) {
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
            if (state != SuggestionsFragment.Suggestion.State.IGNORED) {
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
        private SuggestionsFragment.Suggestion.State successState;

        SuggestionRequestCallback(int position, View v, SuggestionsFragment.Suggestion.State successState) {
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
                SuggestionsFragment.ViewHolder h = (SuggestionsFragment.ViewHolder) view.getTag();
                h.progress.setVisibility(View.INVISIBLE);
                if (success) {
                    setState(successState, view, position, true);
                }
            }
        }
    }
}
