package com.zazoapp.client.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.ListPopupWindow;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import com.zazoapp.client.multimedia.VideoIdUtils;
import com.zazoapp.client.ui.view.ChipsViewWrapper;
import com.zazoapp.client.ui.view.ThumbView;
import com.zazoapp.client.ui.view.WelcomeScreenPreview;
import com.zazoapp.client.ui.view.rotationcircleview.view.RotationCircleView;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.DialogShower;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by skamenkovych@codeminders.com on 4/18/2016.
 */
public class WelcomeMultipleFragment extends ZazoTopFragment implements View.OnTouchListener, WelcomeScreenPreview.OnSizeChangedListener, WelcomeFriendsListAdapter.OnItemStateChangedListener {

    private static final String TAG = WelcomeMultipleFragment.class.getSimpleName();

    @InjectView(R.id.up) MaterialMenuView up;
    @InjectView(R.id.zazo_action_bar) View actionBar;
    @InjectView(R.id.video_preview) TextureView preview;
    @InjectView(R.id.switch_camera_icon) ImageView switchCameraIcon;
    @InjectView(R.id.bottom_sheet) View bottomSheet;
    @InjectView(R.id.number_members) TextView numberMembers;
    @InjectView(R.id.recipients_field) ViewGroup recipientsField;
    @InjectView(R.id.record_btn) RotationCircleView recordBtn;
    @InjectView(R.id.recording_layout) View recordingLayout;
    @InjectView(R.id.recording_time_label) TextView recordingTimeLabel;
    @InjectView(R.id.stop_recording_btn) ImageView stopRecordingBtn;
    @InjectView(R.id.video_actions_layout) View videoActionsLayout;
    @InjectView(R.id.video_thumb) ThumbView videoThumb;
    @InjectView(R.id.video_duration_text) TextView videoDurationText;
    private ArrayList<Friend> friendsNotSent;
    private List<FriendReceiver> receivers;

    public static final String EXTRA_FRIEND_IDS = "friend_mkeys";
    public static final String EXTRA_VIDEO_PATH = "video_path";
    private Context context;
    private Handler handler;
    private Timer timer;
    private TimerTask updateTimerTask;
    private static final SimpleDateFormat timerFormatter = new SimpleDateFormat("mm:ss");
    private CancelableTask recordIconAnimation;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        timer = new Timer();
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timer.cancel();
        timer = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        context = inflater.getContext();
        WelcomeScreenPreview view = new WelcomeScreenPreview(context);
        view.setOnSizeChangedListener(this);
        ButterKnife.inject(this, view);
        BaseManagerProvider managers = getManagers();
        if (managers != null) {
            CameraManager.setUsePreferredPreviewSize(true);
            managers.getRecorder().addPreviewTo(view, false);
        }
        up.setState(MaterialMenuDrawable.IconState.X);
        friendsNotSent = FriendFactory.getFactoryInstance().allEnabled();
        Iterator<Friend> it = friendsNotSent.iterator();
        while (it.hasNext()) {
            Friend friend = it.next();
            if (friend.everSent() /*|| friend.isConnectionCreator() || friend.hasIncomingPlayableVideos()*/) {
                it.remove();
            }
        }
        receivers = new ArrayList<>(friendsNotSent.size());
        for (Friend friend : friendsNotSent) {
            receivers.add(new FriendReceiver(friend, true));
        }
        numberMembers.setText(getResources().getQuantityString(R.plurals.welcome_multiple_members_label, friendsNotSent.size(), friendsNotSent.size()));
        recordingTimeLabel.setTypeface(Convenience.getTypeface(context));
        recordBtn.setOnTouchListener(this);
        bottomSheet.setOnTouchListener(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        CameraManager.setUsePreferredPreviewSize(true);
        BaseManagerProvider managers = getManagers();
        if (managers != null) {
            managers.getRecorder().resume();
        }
        startRecordIconAnimation();
    }

    private void startRecordIconAnimation() {
        stopRecordIconAnimation();
        recordIconAnimation = new CancelableTask() {
            @Override
            protected void doTask() {
                if (isResumed() && recordBtn.getVisibility() == View.VISIBLE) {
                    recordBtn.getAnimationController().start();
                    recordBtn.postDelayed(this, 6000);
                }
            }
        };
        recordBtn.postDelayed(recordIconAnimation, 2000);
    }

    private void stopRecordIconAnimation() {
        if (recordIconAnimation != null) {
            recordIconAnimation.cancel();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
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
        String path = Config.recordingFilePath(context);
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
            startRecordIconAnimation();
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
                stopRecordIconAnimation();
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
                final Handler handler = WelcomeMultipleFragment.this.handler;
                if (handler == null) {
                    return;
                }
                if (managers != null && managers.getRecorder().isRecording()) {
                    handler.post(new Runnable() {
                        String time = timerFormatter.format(date);
                        @Override
                        public void run() {
                            recordingTimeLabel.setText(time);
                        }
                    });
                    date.setTime(date.getTime() + 1000);
                    if (date.getTime() > 300000) { // limit to 5 min
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                stopRecording();
                            }
                        });
                    }
                } else { // if recording was stopped due to some errors
                    handler.post(new Runnable() {
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
        ArrayList<String> friendIds = new ArrayList<>();
        for (FriendReceiver receiver : receivers) {
            if (receiver.isReceiver()) {
                friendIds.add(receiver.getFriend().getId());
            }
        }
        String videoId = VideoIdUtils.generateId();
        String filePath = String.format("%s%s%s_%s.mp4", Config.homeDirPath(context), File.separator, "welcome", videoId);
        File dest = new File(filePath);
        File src = Config.recordingFile(context);
        src.renameTo(dest);
        intent.putStringArrayListExtra(EXTRA_FRIEND_IDS, friendIds);
        intent.putExtra(EXTRA_VIDEO_PATH, filePath);
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

    @Override
    public void onSizeChanged(int w, int h) {
        fillChips();
    }

    private void fillChips() {
        int maxWidth = recipientsField.getMeasuredWidth();
        ChipsViewWrapper more = new ChipsViewWrapper(context);
        more.setMore();
        more.getView().measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int moreWidth = more.getView().getMeasuredWidth();
        recipientsField.removeAllViewsInLayout();
        List<FriendReceiver> rs = new ArrayList<>();
        for (FriendReceiver receiver : receivers) {
            if (receiver.isReceiver()) {
                rs.add(receiver);
            }
        }
        int i = 0;
        for (; i < rs.size(); i++) {
            ChipsViewWrapper cvh = new ChipsViewWrapper(context);
            Friend friend = rs.get(i).getFriend();
            if (friend.thumbExists()) {
                cvh.setTitleWithIcon(friend.getFullName(), friend.thumbBitmap());
            } else {
                cvh.setTitle(friend.getFullName());
            }
            cvh.getView().measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (cvh.getView().getMeasuredWidth() + moreWidth < maxWidth
                    || (i == rs.size() - 1) && cvh.getView().getMeasuredWidth() < maxWidth) {
                maxWidth -= cvh.getView().getMeasuredWidth();
                recipientsField.addView(cvh.getView());
            } else {
                break;
            }
        }
        if (i < rs.size()) {
            ChipsViewWrapper cvh = new ChipsViewWrapper(context);
            cvh.setMore();
            recipientsField.addView(cvh.getView());
        }
    }

    @OnClick(R.id.recipients_field)
    public void displayRecipientChooserPopup() {
        final ListPopupWindow listPopupWindow = new ListPopupWindow(context);
        WelcomeFriendsListAdapter adapter = new WelcomeFriendsListAdapter(getActivity(), receivers);
        adapter.setOnItemStateChangedListener(this);
        listPopupWindow.setAdapter(adapter);
        listPopupWindow.setContentWidth(recipientsField.getWidth() + Convenience.dpToPx(context, 8));
        listPopupWindow.setDropDownGravity(Gravity.START);
        listPopupWindow.setListSelector(getResources().getDrawable(R.drawable.options_popup_item_bg));
        listPopupWindow.setAnchorView(recipientsField);
        listPopupWindow.setVerticalOffset(Convenience.dpToPx(context, 16));
        if (receivers.size() > 5) {
            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{android.R.attr.listPreferredItemHeight});
            int itemHeight = a.getDimensionPixelSize(0, -1);
            listPopupWindow.setHeight(itemHeight < 0 ? -1 : itemHeight * 5);
        }
        listPopupWindow.setModal(true);
        listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int phoneIndex, long id) {
                //finishAction.onPhoneItemSelected(phoneIndex);
                DialogShower.showToast(context, "Test");
            }
        });
        listPopupWindow.show();
    }

    @Override
    public void onItemStateChanged(int position, FriendReceiver receiver) {
        int recipientsCount = 0;
        for (FriendReceiver friendReceiver : receivers) {
            if (friendReceiver.isReceiver()) {
                recipientsCount++;
            }
        }
        numberMembers.setText(getResources().getQuantityString(R.plurals.welcome_multiple_members_label, recipientsCount, recipientsCount));
        fillChips();
    }

    public static class FriendReceiver implements Comparable<FriendReceiver> {
        private final Friend friend;
        private boolean receiver;

        public FriendReceiver(@NonNull Friend friend, boolean receiver) {
            this.friend = friend;
            this.receiver = receiver;
        }

        public Friend getFriend() {
            return friend;
        }

        public void setReceiver(boolean receiver) {
            this.receiver = receiver;
        }

        public boolean isReceiver() {
            return receiver;
        }

        @Override
        public int compareTo(@NonNull FriendReceiver another) {
            return this.friend.getFullName().compareTo(another.friend.getFullName());
        }
    }
}
