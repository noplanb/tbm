package com.zazoapp.client.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
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
import com.zazoapp.client.core.IntentHandlerService;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.ui.view.WelcomeScreenPreview;
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
    @InjectView(R.id.video_actions_layout) View videoActionsLayout;
    @InjectView(R.id.video_thumb) ImageView videoThumb;
    @InjectView(R.id.video_duration_text) TextView videoDurationText;
    private ArrayList<Friend> friends;

    public static final String EXTRA_FRIEND_IDS = "friend_mkeys";
    public static final String EXTRA_VIDEO_PATH = "video_path";
    private WelcomeScreenPreview contentView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        contentView = new WelcomeScreenPreview(inflater.getContext());
        ButterKnife.inject(this, contentView);
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
        return contentView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        BaseManagerProvider managers = getManagers();
        if (managers != null) {
            managers.getRecorder().addPreviewTo(contentView, false);
        }
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
        videoActionsLayout.setVisibility(View.VISIBLE);
        switchCameraIcon.setVisibility(View.VISIBLE);
    }

    @OnClick({R.id.redo_btn, R.id.send_btn})
    public void onVideoActionClicked(View v) {
        switch (v.getId()) {
            case R.id.redo_btn:
                startRecording();
                break;
            case R.id.send_btn:
                sendRecording();
                break;
        }
    }

    @OnClick({R.id.video_preview, R.id.switch_camera_icon})
    public void onPreviewClicked(View v) {
        BaseManagerProvider managers = getManagers();
        if (managers != null) {
            managers.getRecorder().switchCamera();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        startRecording();
        return true;
    }

    private void startRecording() {
        bottomSheet.setVisibility(View.INVISIBLE);
        switchCameraIcon.setVisibility(View.INVISIBLE);
        recordBtn.setVisibility(View.INVISIBLE);
        actionBar.setVisibility(View.INVISIBLE);
        // TODO start recording
        recordingTimeLabel.setText("00:00");
        recordingLayout.setVisibility(View.VISIBLE);
    }

    private void sendRecording() {
        getActivity().finish();
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setAction(IntentHandlerService.IntentActions.SEND_VIDEO);
        intent.putExtra(EXTRA_FRIEND_IDS, "ids");
        intent.putExtra(EXTRA_VIDEO_PATH, "path");
        startActivity(intent);
    }

    @Override
    protected void onBackPressed() {
        super.onBackPressed();
        if (fromApplication()) {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
        }
        getActivity().finish();
    }

    private boolean fromApplication() {
        Bundle args = getArguments();
        return args != null && args.getBoolean(SuggestionsFragment.FROM_APPLICATION, false);
    }

    private BaseManagerProvider getManagers() {
        final FragmentActivity activity = getActivity();
        if (activity instanceof WelcomeScreenActivity) {
            return ((WelcomeScreenActivity) activity).getManagerProvider();
        }
        return null;
    }
}
