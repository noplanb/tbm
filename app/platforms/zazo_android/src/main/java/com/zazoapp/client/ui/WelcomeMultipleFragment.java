package com.zazoapp.client.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.MaterialMenuView;
import com.zazoapp.client.R;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.utilities.DialogShower;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by skamenkovych@codeminders.com on 4/18/2016.
 */
public class WelcomeMultipleFragment extends ZazoTopFragment implements View.OnTouchListener {

    private static final String TAG = WelcomeMultipleFragment.class.getSimpleName();

    @InjectView(R.id.up) MaterialMenuView up;
    @InjectView(R.id.zazo_action_bar) View actionBar;
    @InjectView(R.id.video_preview) TextureView preview;
    @InjectView(R.id.switch_camera_icon) ImageView switchCameraIcon;
    @InjectView(R.id.bottom_sheet) View bottomSheet;
    @InjectView(R.id.number_members) TextView numberMembers;
    @InjectView(R.id.recipients_field) ViewGroup recipientsField;
    @InjectView(R.id.record_btn) FloatingActionButton recordBtn;
    @InjectView(R.id.recording_layout) View recordingLayout;
    @InjectView(R.id.recording_time_label) TextView recordingTimeLabel;
    @InjectView(R.id.stop_recording_btn) ImageView stopRecordingBtn;
    private ArrayList<Friend> friends;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.welcome_multiple_layout, null);
        ButterKnife.inject(this, v);
        up.setState(MaterialMenuDrawable.IconState.ARROW);
        friends = FriendFactory.getFactoryInstance().allEnabled();
        Iterator<Friend> it = friends.iterator();
        while (it.hasNext()) {
            Friend friend = it.next();
            if (friend.everSent() || friend.isConnectionCreator() || friend.hasIncomingPlayableVideos()) {
                it.remove();
            }
        }

        numberMembers.setText(getResources().getQuantityString(R.plurals.welcome_multiple_members_label, friends.size(), friends.size()));

        recordBtn.setOnTouchListener(this);
        return v;
    }

    private void hideActionBar() {
        actionBar.animate().alpha(0).start();
    }

    private void showActionBar() {
        actionBar.animate().alpha(1).start();
    }

    @OnClick(R.id.home)
    public void onBackClicked() {
        onKeyDown(KeyEvent.KEYCODE_BACK, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
    }

    @OnClick(R.id.stop_recording_btn)
    public void onStopRecordClicked() {
        recordingLayout.setVisibility(View.INVISIBLE);
        DialogShower.showToast(getActivity(), "Recording finished");
        // TODO stop recording
        actionBar.setVisibility(View.VISIBLE);
        bottomSheet.setVisibility(View.VISIBLE);
        switchCameraIcon.setVisibility(View.VISIBLE);
        recordBtn.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        bottomSheet.setVisibility(View.INVISIBLE);
        switchCameraIcon.setVisibility(View.INVISIBLE);
        recordBtn.setVisibility(View.INVISIBLE);
        actionBar.setVisibility(View.INVISIBLE);
        // TODO start recording
        recordingTimeLabel.setText("00:00");
        recordingLayout.setVisibility(View.VISIBLE);
        return true;
    }
}
