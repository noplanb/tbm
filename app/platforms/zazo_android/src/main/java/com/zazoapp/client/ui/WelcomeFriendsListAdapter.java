package com.zazoapp.client.ui;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.zazoapp.client.R;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.ui.WelcomeMultipleFragment.FriendReceiver;
import com.zazoapp.client.ui.view.CircleThumbView;
import com.zazoapp.client.utilities.Convenience;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 3/30/2016.
 */
class WelcomeFriendsListAdapter extends BaseAdapter implements CompoundButton.OnCheckedChangeListener {
    private LayoutInflater inflater;
    private List<FriendReceiver> receivers;
    private final int icons[] = {R.drawable.bgn_thumb_1, R.drawable.bgn_thumb_2, R.drawable.bgn_thumb_3, R.drawable.bgn_thumb_4};
    private final int colors[];
    private ColorMatrix grayedMatrix = new ColorMatrix();
    private ColorMatrixColorFilter disabledFilter;

    private final float MIN_SATURATION = 0.25f;
    private final float MIN_ALPHA = 0.6f;
    private final long ANIM_DURATION = 300;

    private OnItemStateChangedListener onItemStateChangedListener;

    interface OnItemStateChangedListener {
        void onItemStateChanged(int position, FriendReceiver receiver);
    }

    void setOnItemStateChangedListener(@Nullable OnItemStateChangedListener listener) {
        onItemStateChangedListener = listener;
    }

    WelcomeFriendsListAdapter(Activity context, List<FriendReceiver> list) {
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(context.getBaseContext(), R.style.AppTheme_SwitchCompat);
        inflater = (LayoutInflater) contextThemeWrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        receivers = list;
        Collections.sort(receivers, new Comparator<FriendReceiver>() {
            @Override
            public int compare(FriendReceiver lhs, FriendReceiver rhs) {
                return lhs.compareTo(rhs);
            }
        });
        colors = context.getResources().getIntArray(R.array.thumb_colors);
        grayedMatrix.setSaturation(MIN_SATURATION);
        disabledFilter = new ColorMatrixColorFilter(grayedMatrix);
    }

    void setList(List<FriendReceiver> friends) {
        this.receivers = friends;
    }

    @Override
    public int getCount() {
        return receivers.size();
    }

    @Override
    public FriendReceiver getItem(int position) {
        return receivers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder h;
        if (convertView != null) {
            h = (Holder) convertView.getTag();
        } else {
            convertView = inflater.inflate(R.layout.welcome_friends_popup_list_item, null);
            h = new Holder(convertView);
            convertView.setTag(h);
        }
        FriendReceiver r = getItem(position);
        Friend f = r.getFriend();
        h.name.setText(f.getFullName());
        h.button.setTag(h);
        h.button.setTag(R.id.id, position);
        h.button.setChecked(r.isReceiver());
        h.button.setOnCheckedChangeListener(this);
        setState(r.isReceiver(), h.button, position, false);
        return convertView;
    }

    @Override
    public void onCheckedChanged(final CompoundButton v, final boolean isChecked) {
        if (v.getId() == R.id.delete_btn) {
            final int position = (Integer) v.getTag(R.id.id);
            final FriendReceiver receiver = getItem(position);
            if (receiver.isReceiver() != isChecked) {
                receiver.setReceiver(isChecked);
                setState(isChecked, v, position, true);
                if (onItemStateChangedListener != null) {
                    onItemStateChangedListener.onItemStateChanged(position, receiver);
                }
            }
        }
    }

    void setState(final boolean enabled, final View v, final int position, boolean smooth) {
        FriendReceiver r = getItem(position);
        Friend f = r.getFriend();
        final Holder h = (Holder) v.getTag();
        final int posId = (int) v.getTag(R.id.id);
        if (f.thumbExists()) {
            h.thumbTitle.setText(null);
            h.thumb.setImageBitmap(f.thumbBitmap());
            h.thumb.setFillColor(Color.TRANSPARENT);
        } else {
            h.thumb.setImageResource(Convenience.getStringDependentItem(f.getFullName(), icons));
            h.thumb.setFillColor(Convenience.getStringDependentItem(f.getFullName(), colors));
            h.thumbTitle.setText(f.getInitials());
        }
        if (smooth) {
            ValueAnimator animator = new ValueAnimator();
            animator.setDuration(ANIM_DURATION);
            animator.setInterpolator(new AccelerateInterpolator());
            if (enabled) {
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
                        h.thumb.setColorFilter(filter);
                        h.thumb.setFillColorFilter(filter);
                    } else {
                        animation.cancel();
                    }
                }
            });
            animator.start();
        } else {
            if (enabled) {
                h.leftLayout.setAlpha(1f);
                h.thumb.setColorFilter(null);
                h.thumb.setFillColorFilter(null);
            } else {
                h.leftLayout.setAlpha(MIN_ALPHA);
                h.thumb.setColorFilter(disabledFilter);
                h.thumb.setFillColorFilter(disabledFilter);
            }
        }

    }

    class Holder {
        @InjectView(R.id.thumb) CircleThumbView thumb;
        @InjectView(R.id.thumb_title) TextView thumbTitle;
        @InjectView(R.id.delete_btn) SwitchCompat button;
        @InjectView(R.id.name) TextView name;
        @InjectView(R.id.left_layout) View leftLayout;

        Holder(View source) {
            ButterKnife.inject(this, source);
        }
    }
}
