package com.zazoapp.client.ui;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.MaterialMenuView;
import com.zazoapp.client.R;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.GridManager;
import com.zazoapp.client.network.DeleteFriendRequest;
import com.zazoapp.client.network.HttpRequest;
import com.zazoapp.client.ui.animations.SlideHorizontalFadeAnimation;
import com.zazoapp.client.ui.view.FilterWatcher;
import com.zazoapp.client.ui.view.SearchPanel;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.DialogShower;
import de.hdodenhof.circleimageview.CircleImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 8/14/2015.
 */
public class ManageFriendsFragment extends ZazoTopFragment implements View.OnTouchListener {

    @InjectView(R.id.friends_list) ListView listView;
    @InjectView(R.id.up) MaterialMenuView up;

    private SearchPanel searchPanel;
    private FriendsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.manage_friends_layout, null);
        ButterKnife.inject(this, v);
        adapter = new FriendsAdapter(getActivity());
        listView.setAdapter(adapter);
        listView.setOnTouchListener(this);
        up.setState(MaterialMenuDrawable.IconState.ARROW);
        searchPanel = new SearchPanel(v);
        searchPanel.addTextChangedListener(new FilterWatcher() {
            @Override
            protected void applyFilter(CharSequence text) {
                filter(text);
            }
        });
        return v;
    }

    @OnClick(R.id.home)
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.home:
                searchPanel.hideKeyboard();
                getFragmentManager().popBackStack();
                break;
        }
    }

    private void filter(CharSequence text) {
        if (listView.getAdapter() != null) {
            adapter.getFilter().filter(text);
        }
    }

    class FriendsAdapter extends BaseAdapter implements CompoundButton.OnCheckedChangeListener, Filterable {

        private LayoutInflater inflater;
        private List<Friend> friends;
        private final int icons[] = {R.drawable.bgn_thumb_1, R.drawable.bgn_thumb_2, R.drawable.bgn_thumb_3, R.drawable.bgn_thumb_4};
        private final int colors[];
        private ColorMatrix grayedMatrix = new ColorMatrix();
        private ColorMatrixColorFilter disabledFilter;
        private ColorDrawable transparentDrawable = new ColorDrawable(Color.TRANSPARENT);

        private final float MIN_SATURATION = 0.25f;
        private final float MIN_ALPHA = 0.6f;
        private final long ANIM_DURATION = 300;
        private Filter filter;

        FriendsAdapter(Activity context) {
            ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(context.getBaseContext(), R.style.AppTheme_SwitchCompat);
            inflater = (LayoutInflater) contextThemeWrapper.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            friends = FriendFactory.getFactoryInstance().all();
            Collections.sort(friends, new Comparator<Friend>() {
                @Override
                public int compare(Friend lhs, Friend rhs) {
                    return lhs.getFullName().compareTo(rhs.getFullName());
                }
            });
            colors = context.getResources().getIntArray(R.array.thumb_colors);
            grayedMatrix.setSaturation(MIN_SATURATION);
            disabledFilter = new ColorMatrixColorFilter(grayedMatrix);
        }

        void setList(List<Friend> friends) {
            this.friends = friends;
        }

        @Override
        public int getCount() {
            return friends.size();
        }

        @Override
        public Friend getItem(int position) {
            return friends.get(position);
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
                convertView = inflater.inflate(R.layout.manage_friends_list_item, null);
                h = new Holder(convertView);
                convertView.setTag(h);
            }
            Friend f = getItem(position);
            h.name.setText(f.getFullName());
            h.phone.setText(f.get(Friend.Attributes.MOBILE_NUMBER));
            h.button.setTag(h);
            h.button.setTag(R.id.id, position);
            h.button.setChecked(!f.isDeleted());
            h.button.setOnCheckedChangeListener(this);
            setState(!f.isDeleted(), h.button, position, false);
            h.progress.setVisibility(View.INVISIBLE);
            return convertView;
        }

        @Override
        public void onCheckedChanged(final CompoundButton v, final boolean isChecked) {
            if (v.getId() == R.id.delete_btn) {
                final int position = (Integer) v.getTag(R.id.id);
                final Friend friend = getItem(position);
                if (friend.isDeleted() == isChecked) {
                    ((Holder) v.getTag()).progress.setVisibility(View.VISIBLE);
                    DeleteFriendRequest.makeRequest(friend, !isChecked, new HttpRequest.Callbacks() {
                        @Override
                        public void success(String response) {
                            friend.setDeleted(!isChecked);
                            GridManager.onFriendDeleteStatusChanged(friend);
                            finishRequest();
                        }

                        @Override
                        public void error(String errorString) {
                            DialogShower.showToast(TbmApplication.getInstance(), R.string.toast_could_not_sync);
                            v.setChecked(!friend.isDeleted());
                            finishRequest();
                        }

                        private void finishRequest() {
                            if (position == (int) v.getTag(R.id.id)) {
                                Holder h = (Holder) v.getTag();
                                h.progress.setVisibility(View.INVISIBLE);
                                setState(!friend.isDeleted(), v, position, true);
                            }
                        }
                    });
                }

            }
        }

        void setState(final boolean enabled, final View v, final int position, boolean smooth) {
            Friend f = getItem(position);
            final Holder h = (Holder) v.getTag();
            final int posId = (int) v.getTag(R.id.id);
            if (f.thumbExists()) {
                h.thumbTitle.setText(null);
                h.thumb.setImageBitmap(f.thumbBitmap());
                h.thumb.setFillColor(Color.TRANSPARENT);
            } else if (f.hasApp()) {
                h.thumb.setImageResource(Convenience.getStringDependentItem(f.getFullName(), icons));
                h.thumb.setFillColor(Convenience.getStringDependentItem(f.getFullName(), colors));
                h.thumbTitle.setText(f.getInitials());
            } else {
                h.thumb.setImageDrawable(transparentDrawable);
                h.thumb.setFillColor(Color.GRAY);
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
                } else {
                    h.leftLayout.setAlpha(MIN_ALPHA);
                    h.thumb.setColorFilter(disabledFilter);
                }
            }

        }

        @Override
        public Filter getFilter() {
            if (filter == null) {
                filter = new SearchFilter();
            }
            return filter;
        }

        class Holder {
            @InjectView(R.id.thumb) CircleImageView thumb;
            @InjectView(R.id.thumb_title) TextView thumbTitle;
            @InjectView(R.id.delete_btn) SwitchCompat button;
            @InjectView(R.id.name) TextView name;
            @InjectView(R.id.phone) TextView phone;
            @InjectView(R.id.progress_layout) View progress;
            @InjectView(R.id.left_layout) View leftLayout;

            Holder(View source) {
                ButterKnife.inject(this, source);
            }
        }

        private class SearchFilter extends Filter {

            @Override
            protected FilterResults performFiltering(CharSequence filterString) {

                FilterResults results = new FilterResults();
                List<Friend> listCopy = FriendFactory.getFactoryInstance().all();
                Collections.sort(listCopy, new Comparator<Friend>() {
                    @Override
                    public int compare(Friend lhs, Friend rhs) {
                        return lhs.getFullName().compareTo(rhs.getFullName());
                    }
                });
                if (filterString == null || filterString.length() == 0) {
                    results.values = listCopy;
                    results.count = listCopy.size();
                } else {
                    String prefixString = filterString.toString().toLowerCase();

                    // find all matching objects here and add
                    // them to allMatching, use filterString.
                    List<Friend> allMatching = new ArrayList<>();
                    for (Friend friend : listCopy) {
                        if (friend != null && friend.getFullName().toLowerCase().contains(prefixString)) {
                            allMatching.add(friend);
                        }
                    }

                    results.values = allMatching;
                    results.count = allMatching.size();
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                setList((List<Friend>) results.values);
                if (results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
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

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.friends_list && searchPanel != null) {
            searchPanel.hideKeyboard();
        }
        return false;
    }
}
