package com.zazoapp.client.ui;

import android.content.Intent;
import android.graphics.Bitmap;
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
import com.zazoapp.client.Config;
import com.zazoapp.client.R;
import com.zazoapp.client.core.IntentHandlerService;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.multimedia.CameraManager;
import com.zazoapp.client.multimedia.ThumbnailRetriever;
import com.zazoapp.client.ui.view.ThumbView;
import com.zazoapp.client.ui.view.WelcomeScreenPreview;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.DialogShower;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

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
    @InjectView(R.id.video_thumb) ThumbView videoThumb;
    @InjectView(R.id.video_duration_text) TextView videoDurationText;
    private ArrayList<Friend> friends;

    public static final String EXTRA_FRIEND_IDS = "friend_mkeys";
    public static final String EXTRA_VIDEO_PATH = "video_path";
    private WelcomeScreenPreview contentView;
    private Timer timer;
    private TimerTask updateTimerTask;
    private static final SimpleDateFormat timerFormatter = new SimpleDateFormat("mm:ss");

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
        recordingTimeLabel.setTypeface(Convenience.getTypeface(contentView.getContext()));
        recordBtn.setOnTouchListener(this);
        bottomSheet.setOnTouchListener(this);
        timer = new Timer();
        return contentView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timer.cancel();
        timer = null;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        BaseManagerProvider managers = getManagers();
        if (managers != null) {
            CameraManager.setUsePreferredPreviewSize(true);
            managers.getRecorder().addPreviewTo(contentView, false);
        }
    }

    @Override
    public void onResume() {
        super.onStart();
        CameraManager.setUsePreferredPreviewSize(true);
    }

    @Override
    public void onPause() {
        super.onStop();
        CameraManager.setUsePreferredPreviewSize(false);
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
        stopRecording();
    }

    private void stopRecording() {
        recordingLayout.setVisibility(View.INVISIBLE);
        DialogShower.showToast(getActivity(), "Recording finished");
        BaseManagerProvider managers = getManagers();
        if (managers != null) {
            managers.getRecorder().stop();
        }
        cancelTimerTask();
        actionBar.setVisibility(View.VISIBLE);
        bottomSheet.setVisibility(View.VISIBLE);
        switchCameraIcon.setVisibility(View.VISIBLE);
        setupThumb();
    }

    private void setupThumb() {
        String path = Config.recordingFilePath(contentView.getContext());
        File recordedFile = new File(path);
        if (recordedFile .exists()) {
            videoActionsLayout.setVisibility(View.VISIBLE);
            recordBtn.setVisibility(View.INVISIBLE);
            ThumbnailRetriever retriever = new ThumbnailRetriever();
            try {
                Bitmap thumbnail = retriever.getThumbnail(path);
                videoThumb.setImageBitmap(thumbnail);
                videoThumb.setMapArea(ThumbView.MapArea.FULL);
            } catch (ThumbnailRetriever.ThumbnailBrokenException e) {
                videoThumb.setImageResource(R.drawable.navigation_background_pattern);
                videoThumb.setMapArea(ThumbView.MapArea.LEFT_TOP);
            }
            videoDurationText.setText(recordingTimeLabel.getText());
        } else {
            videoActionsLayout.setVisibility(View.GONE);
            recordBtn.setVisibility(View.VISIBLE);
        }
    }

    private void startRecording() {
        BaseManagerProvider managers = getManagers();
        if (managers != null) {
            if (managers.getRecorder().start(null)) {
                runNewTimerTask();
                bottomSheet.setVisibility(View.INVISIBLE);
                switchCameraIcon.setVisibility(View.INVISIBLE);
                recordBtn.setVisibility(View.INVISIBLE);
                actionBar.setVisibility(View.INVISIBLE);
                recordingLayout.setVisibility(View.VISIBLE);
            } else {
                DialogShower.showToast(getActivity(), R.string.toast_unable_to_start_recording);
            }
        }
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
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (v.getId() == R.id.record_btn) {
                BaseManagerProvider managers = getManagers();
                if (managers != null && !managers.getRecorder().isRecording()) {
                    startRecording();
                }
            }
        }
        return true;
    }

    private void cancelTimerTask() {
        if (updateTimerTask != null) {
            updateTimerTask.cancel();
            timer.purge();
            updateTimerTask = null;
        }
    }

    private void runNewTimerTask() {
        cancelTimerTask();
        updateTimerTask = new TimerTask() {
            Date date = new Date(0);
            @Override
            public void run() {
                BaseManagerProvider managers = getManagers();
                if (managers != null && managers.getRecorder().isRecording()) {
                    recordingTimeLabel.post(new Runnable() {
                        String time = timerFormatter.format(date);
                        @Override
                        public void run() {
                            recordingTimeLabel.setText(time);
                        }
                    });
                    date.setTime(date.getTime() + 1000);
                    if (date.getTime() > 300000) { // limit to 5 min
                        contentView.post(new Runnable() {
                            @Override
                            public void run() {
                                stopRecording();
                            }
                        });
                    }
                } else { // if recording was stopped due to some errors
                    contentView.post(new Runnable() {
                        @Override
                        public void run() {
                            stopRecording();
                        }
                    });
                }
            }
        };
        timer.schedule(updateTimerTask, 0, 1000);
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
