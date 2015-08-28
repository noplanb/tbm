package com.zazoapp.client.ui;

import android.app.Fragment;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.zazoapp.client.R;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.GridElementFactory;
import com.zazoapp.client.model.GridManager;
import com.zazoapp.client.network.DeleteFriendRequest;
import com.zazoapp.client.network.HttpRequest;
import com.zazoapp.client.utilities.DialogShower;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 8/14/2015.
 */
public class ManageFriendsFragment extends Fragment {

    @InjectView(R.id.friends_list) ListView listView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.manage_friends_layout, null);
        ButterKnife.inject(this, v);
        listView.setAdapter(new FriendsAdapter(getActivity().getApplicationContext()));
        return v;
    }

    @OnClick(R.id.home)
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.home:
                getFragmentManager().popBackStack();
                break;
        }
    }

    private void onFriendDeleteStatusChanged(Friend friend) {
        if (friend.isDeleted()) {
            if (GridElementFactory.getFactoryInstance().friendIsOnGrid(friend)) {
                GridManager.getInstance().moveNextFriendTo(GridElementFactory.getFactoryInstance().findWithFriendId(friend.getId()));
            }
        } else {
            GridManager.getInstance().moveFriendToGrid(friend);
        }
    }

    class FriendsAdapter extends BaseAdapter implements CompoundButton.OnCheckedChangeListener {

        private Context context;
        private List<Friend> friends;

        FriendsAdapter(Context context) {
            this.context = context;
            friends = FriendFactory.getFactoryInstance().all();
            Collections.sort(friends, new Comparator<Friend>() {
                @Override
                public int compare(Friend lhs, Friend rhs) {
                    return lhs.getFullName().compareTo(rhs.getFullName());
                }
            });
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
                convertView = View.inflate(context, R.layout.manage_friends_list_item, null);
                h = new Holder(convertView);
                convertView.setTag(h);
            }
            Friend f = getItem(position);
            if (f.thumbExists()) {
                h.thumb.setImageBitmap(f.sqThumbBitmap());
            } else if (f.hasApp()) {
                h.thumb.setImageBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_zazo_letter));
            } else {
                h.thumb.setImageBitmap(null);
            }
            h.name.setText(f.getFullName());
            h.phone.setText(f.get(Friend.Attributes.MOBILE_NUMBER));
            h.button.setTag(h);
            h.button.setTag(R.id.id, position);
            h.button.setChecked(f.isDeleted());
            h.button.setOnCheckedChangeListener(this);
            h.itemBg.setVisibility(f.isDeleted() ? View.VISIBLE : View.INVISIBLE);
            h.progress.setVisibility(View.INVISIBLE);
            return convertView;
        }

        @Override
        public void onCheckedChanged(final CompoundButton v, final boolean isChecked) {
            if (v.getId() == R.id.delete_btn) {
                final int position = (Integer) v.getTag(R.id.id);
                final Friend friend = getItem(position);
                if (friend.isDeleted() != isChecked) {
                    ((Holder) v.getTag()).progress.setVisibility(View.VISIBLE);
                    DeleteFriendRequest.makeRequest(friend, isChecked, new HttpRequest.Callbacks() {
                        @Override
                        public void success(String response) {
                            friend.setDeleted(isChecked);
                            onFriendDeleteStatusChanged(friend);
                            finishRequest();
                        }

                        @Override
                        public void error(String errorString) {
                            DialogShower.showToast(TbmApplication.getInstance(), R.string.toast_could_not_sync);
                            v.setChecked(friend.isDeleted());
                            finishRequest();
                        }

                        private void finishRequest() {
                            if (position == (Integer) v.getTag(R.id.id)) {
                                Holder h = (Holder) v.getTag();
                                h.progress.setVisibility(View.INVISIBLE);
                                h.itemBg.setVisibility(friend.isDeleted() ? View.VISIBLE : View.INVISIBLE);
                            }
                        }
                    });
                }

            }
        }

        class Holder {
            @InjectView(R.id.thumb) ImageView thumb;
            @InjectView(R.id.delete_btn) ToggleButton button;
            @InjectView(R.id.name) TextView name;
            @InjectView(R.id.phone) TextView phone;
            @InjectView(R.id.deleted_bg) View itemBg;
            @InjectView(R.id.progress_layout) View progress;

            Holder(View source) {
                ButterKnife.inject(this, source);
            }
        }
    }

}
