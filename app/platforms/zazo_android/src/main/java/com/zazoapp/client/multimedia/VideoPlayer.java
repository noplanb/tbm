package com.zazoapp.client.multimedia;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnTouch;
import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.MaterialMenuView;
import com.zazoapp.client.Config;
import com.zazoapp.client.R;
import com.zazoapp.client.core.MessageContainer;
import com.zazoapp.client.core.MessageType;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.IncomingMessage;
import com.zazoapp.client.network.FileDownloadService;
import com.zazoapp.client.network.FileTransferService;
import com.zazoapp.client.ui.BaseManagerProvider;
import com.zazoapp.client.ui.ViewGroupGestureRecognizer;
import com.zazoapp.client.ui.animations.TextAnimations;
import com.zazoapp.client.ui.animations.VideoProgressBarAnimation;
import com.zazoapp.client.ui.helpers.VideoContextBarPreferences;
import com.zazoapp.client.ui.view.GestureControlledLayout;
import com.zazoapp.client.ui.view.MessageAdapter;
import com.zazoapp.client.ui.view.NineViewGroup;
import com.zazoapp.client.ui.view.TouchBlockScreen;
import com.zazoapp.client.ui.view.VideoProgressBar;
import com.zazoapp.client.ui.view.VideoView;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.DialogShower;
import com.zazoapp.client.utilities.StringUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class VideoPlayer implements OnCompletionListener, OnPreparedListener, Player {

    private static final String TAG = VideoPlayer.class.getSimpleName();

    private FragmentActivity activity;
    private String videoId;
    private String friendId;
    private Friend friend;
    private int currentVideoNumber = 0;
    private int numberOfMessages = 0;
    @InjectView(R.id.video_root_layout) GestureControlledLayout videoRootLayout;
    private TouchBlockScreen blockScreen;
    private VideoContextBar contextBar;
    private View actionBarDivider;
    @InjectView(R.id.grid_view) NineViewGroup nineViewGroup;
    private boolean videosAreDownloading;
    private BaseManagerProvider managerProvider;
    private Timer timer = new Timer();
    private TimerTask onStartTask;
    private ZoomController zoomController;
    private float mediaVolume = 1.0f;
    private WeakReference<View> targetViewRef;
    private PlayOptions playOptions;
    private PresenterHelper presenterHelper;

    private Set<StatusCallbacks> statusCallbacks = new HashSet<StatusCallbacks>();
    private List<MessageContainer<IncomingMessage>> playingMessages;
    private volatile boolean isSeekAllowed = true;
    private boolean needToNotifyCompletion;

    public VideoPlayer(FragmentActivity activity, BaseManagerProvider managerProvider) {
        this.activity = activity;
        this.managerProvider = managerProvider;
        blockScreen = ButterKnife.findById(activity, R.id.block_screen);
        contextBar = new VideoContextBar(ButterKnife.findById(activity, R.id.zazo_action_context_bar));
        actionBarDivider = ButterKnife.findById(activity, R.id.zazo_action_bar_divider);
        presenterHelper = new PresenterHelper();
    }

    @Override
    public void init(View rootView) {
        ButterKnife.inject(this, rootView);
        blockScreen.setUnlockListener(new TouchBlockScreen.UnlockListener() {
            @Override
            public void onUnlockGesture() {
                blockScreen.unlock(false);
            }
        });
    }

    @Override
	public void registerStatusCallbacks(StatusCallbacks statusCallback){
		this.statusCallbacks.add(statusCallback);
	}

    @Override
	public void unregisterStatusCallbacks(StatusCallbacks statusCallback){
		this.statusCallbacks.remove(statusCallback);
	}

    //-----------------
	// Notify observers
	//-----------------
	private void notifyStartPlaying(){
		for (StatusCallbacks callbacks : statusCallbacks) {
			callbacks.onVideoPlaying(friendId, videoId);
		}
	}

	private void notifyStopPlaying(){
		for (StatusCallbacks callbacks : statusCallbacks) {
			callbacks.onVideoStopPlaying(friendId);
		}
	}

    private void notifyPlaybackError(){
        for (StatusCallbacks callbacks : statusCallbacks) {
            callbacks.onVideoPlaybackError(friendId, videoId);
        }
    }

    private void notifyCompletion() {
        for (StatusCallbacks callbacks : statusCallbacks) {
            callbacks.onCompletion(friendId);
        }
        needToNotifyCompletion = false;
    }

    @Override
    public boolean togglePlayOverView(View view, String friendId, @PlayFlags int options) {
        boolean needToPlay = !(isPlaying() && friendId.equals(this.friendId));

        // Always stop first so that the notification goes out to reset the view we were on in case it was still playing and we are switching to another view.
        stop();

        this.friendId = friendId;
        friend = FriendFactory.getFactoryInstance().find(friendId);

        if (needToPlay) {
            playOptions = new PlayOptions(options);
            targetViewRef = new WeakReference<>(view);
            float oldVolume = mediaVolume;
            if (playOptions.hasFlags(PlayOptions.TRANSCRIPT)) {
                presenterHelper.setCurrentPresentation(Presenter.Type.TRANSCRIPTION);
                mediaVolume = 0f;
            } else {
                presenterHelper.setCurrentPresentation(Presenter.Type.PLAYER);
                mediaVolume = 1f;
            }
            zoomController.clearState();
            presenterHelper.initStateForTarget();
            boolean result = start();
            if (!result) {
                mediaVolume = oldVolume;
            }
            return result;
        }
        return false;
    }

    @Override
    public void stop(){
        Log.i(TAG, "stop");
        presenterHelper.stopPresentation();
        blockScreen.unlock(true);
        managerProvider.getAudioController().setSpeakerPhoneOn(false);
        cancelWaitingForStart();
        notifyStopPlaying();
    }

    @Override
	public void release(){
	    stop();
	}

    @Override
    public void setVolume(float volume) {
        presenterHelper.setVolume(volume);
        mediaVolume = volume;
    }

    //----------------------
	// Private state machine
	//----------------------
	private boolean start(){
		Log.i(TAG, "start");
        needToNotifyCompletion = false;
		setCurrentVideoToFirst();

		if (videoId == null){
		    if (videosAreDownloading){
		        if (friend.hasRetryingDownload()){
                    FileTransferService.reset(activity, FileDownloadService.class);
		            DialogShower.showBadConnection(activity);
		        } else {
		            DialogShower.showToast(activity, R.string.toast_downloading);
		        }
		    } else {
		        Log.w(TAG, "No playable video.");
		    }
		    return false;
		}
		play();
        return true;
	}

    private void play() {
        play(0f);
    }

    private void play(@FloatRange(from = 0.0f, to = 1.0f, toInclusive = false) float progress) {
        Log.i(TAG, "play");
        if (!managerProvider.getAudioController().gainFocus()) {
            DialogShower.showToast(activity, R.string.toast_could_not_get_audio_focus);
            return;
        }
        MessageContainer<IncomingMessage> messageContainer = playingMessages.get(currentVideoNumber - 1);
        MessageType playingType = messageContainer.getType();
        switch (playingType) {
            case VIDEO:
                // Always set it to viewed whether it is playable or not so it eventually gets deleted.
                friend.setAndNotifyIncomingVideoStatus(videoId, IncomingMessage.Status.VIEWED);

                if (videoIsPlayable()) {
                    managerProvider.getAudioController().setSpeakerPhoneOn(true);
                    String path = friend.videoFromPath(videoId);
                    presenterHelper.startPresentation(path, progress);
                } else {
                    onCompletion(null);
                }
                break;
            case TEXT:
                for (int i = 0; i < messageContainer.getSize(); i++) {
                    // Mark all messages as viewed as soon as they will be shown to user
                    friend.setAndNotifyIncomingVideoStatus(messageContainer.getAt(i).getId(), IncomingMessage.Status.VIEWED);
                }
                switch (presenterHelper.getCurrentPresenter().getType()) {
                    case TRANSCRIPTION:
                        if (videoIsPlayable()) {
                            managerProvider.getAudioController().setSpeakerPhoneOn(true);
                            String path = friend.videoFromPath(videoId);
                            presenterHelper.startPresentation(path, progress);
                        } else {
                            onCompletion(null);
                        }
                        break;
                    case PLAYER: {
                        PlayerPresenter presenter = (PlayerPresenter) presenterHelper.getCurrentPresenter();
                        presenter.startMessagesPresentation(activity, messageContainer, this);
                    }
                        break;
                }
                break;
        }

    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "onPrepared");
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.i(TAG, "play complete.");
        setCurrentVideoToNext();

        if (videoId != null)
            play();
        else {
            if (presenterHelper.getCurrentPresenter().getType() != Presenter.Type.TRANSCRIPTION) {
                stop();
                zoomController.clearState();
                notifyCompletion();
            } else {
                needToNotifyCompletion = true;
            }
        }
    }

    //---------------
    // Helper methods
    //---------------
    private void determineIfDownloading() {
        videosAreDownloading = friend.hasDownloadingVideo();
    }

    private void setCurrentVideoToFirst() {
        determineIfDownloading();
        ArrayList<IncomingMessage> messages = (videosAreDownloading) ? friend.getSortedIncomingNotViewedMessages() : friend.getSortedIncomingPlayableMessages();
        playingMessages = MessageContainer.splitToMessageContainer(messages);
        setCurrentVideoId(MessageContainer.getFirstMessageIdInList(playingMessages));
        currentVideoNumber = (videoId != null) ? 1 : 0;
        numberOfMessages = playingMessages.size();
    }

    private int calculateNumberOfVideos() {
        int calculateNumberOfVideos = 0;
        List<MessageContainer<IncomingMessage>> list = playingMessages;
        for (MessageContainer<IncomingMessage> container : list) {
            if (MessageType.VIDEO.equals(container.getType())) {
                calculateNumberOfVideos++;
            }
        }
        return calculateNumberOfVideos;
    }

    private void setCurrentVideoToNext() {
        determineIfDownloading();
        ArrayList<IncomingMessage> messages = (videosAreDownloading) ? friend.getSortedIncomingNotViewedMessages() : friend.getSortedIncomingPlayableMessages();
        playingMessages = MessageContainer.splitToMessageContainer(messages);
        int posId = MessageContainer.getNextMessagePositionInList(videoId, playingMessages);
        currentVideoNumber = posId + 1;
        numberOfMessages = playingMessages.size();
        setCurrentVideoId((posId >= 0) ? playingMessages.get(posId).getAt(0).getId() : null);
    }

    private void setCurrentVideoToPrevious() {
        determineIfDownloading();
        ArrayList<IncomingMessage> messages = (videosAreDownloading) ? friend.getSortedIncomingNotViewedMessages() : friend.getSortedIncomingPlayableMessages();
        playingMessages = MessageContainer.splitToMessageContainer(messages);
        int posId = MessageContainer.getCurrentMessagePositionInList(videoId, playingMessages) - 1;
        if (posId < 0) {
            return;
        }
        currentVideoNumber = posId + 1;
        numberOfMessages = playingMessages.size();
        setCurrentVideoId((posId >= 0) ? playingMessages.get(posId).getAt(0).getId() : null);
    }

    private void jumpToVideo(int pos) {
        if (playingMessages == null || playingMessages.isEmpty() || pos < 0 || playingMessages.size() < pos || pos + 1 == currentVideoNumber) {
            return;
        }
        currentVideoNumber = pos + 1;
        setCurrentVideoId(playingMessages.get(pos).getAt(0).getId());
    }

    private void setCurrentVideoId(String videoId) {
        this.videoId = videoId;
        presenterHelper.refresh();
    }

    public boolean isPlaying(){
        return presenterHelper != null && presenterHelper.isPlaying();
    }

    @Override
    public void rewind(int msec) {
        if (isPlaying()) {
            VideoView view = presenterHelper.getVideoView();
            int current = view.getCurrentPosition();
            int length = view.getDuration();
            if (length > 0) {
                int next = current + msec;
                if (next < 0) {
                    next = 0;
                } else if (next > length) {
                    next = length - 1;
                }
                view.seekTo(next);
            }
        }
    }

    @Override
    public void restartAfter(int delay) {
        final VideoView view = presenterHelper.getVideoView();
        if (view != null) {
            view.pause();
            view.postDelayed(new Runnable() {
                @Override
                public void run() {
                    view.seekTo(0);
                    view.start();
                }
            }, delay);
        }
    }

    @Override
    public void changeAudioStream() {
        if (isPlaying() && !managerProvider.getAudioController().isSpeakerPhoneOn()) {
            blockScreen.lock();
        } else if (blockScreen.isLocked()) {
            blockScreen.unlock(true);
        }
        final VideoView view = presenterHelper.getVideoView();
        if (view != null) {
            view.changeAudioStream(managerProvider.getAudioController().isSpeakerPhoneOn());
        }
    }

    @Override
    public void updatePlayerPosition() {
        presenterHelper.initStateForTarget();
    }

    private boolean videoIsPlayable(){
        File f = friend.videoFromFile(videoId);
        return f.exists() && f.length() > Config.getMinVideoSize();
    }

    private void waitAndNotifyWhenStart() {
        cancelWaitingForStart();
        timer.purge();
        onStartTask = new TimerTask() {
            @Override
            public void run() {
                videoRootLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        final VideoView videoView = presenterHelper.getVideoView();
                        if (onStartTask != null && videoView.getCurrentPosition() > 0) {
                            videoRootLayout.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    // checks if it is playing to eliminate the case of released player
                                    if (isPlaying()) {
                                        isSeekAllowed = true;
                                        presenterHelper.showContent();
                                        notifyStartPlaying();
                                    }
                                }
                            }, 100);
                            cancel();
                        }
                    }
                });

            }
        };
        timer.schedule(onStartTask, 30, 30);
    }

    private void cancelWaitingForStart() {
        if (onStartTask != null) {
            onStartTask.cancel();
            onStartTask = null;
        }
    }

    /**
     *
     * @param fullProgress
     * @param allowLoad
     * @param seek
     * @return currentVideoProgress
     */
    private float setVideoProgress(float fullProgress, boolean allowLoad, boolean seek) {
        contextBar.progressBar.setProgress(fullProgress);
        VideoProgressBar.ItemInfo info = contextBar.progressBar.getCurrentItemInfo();
        int pos = info.position;
        final float curProgress = info.currentProgress;
        final VideoView videoView = presenterHelper.getVideoView();
        if (videoView == null) {
            return 0;
        }
        if ((currentVideoNumber != pos + 1)) {
            jumpToVideo(pos);
            if (allowLoad) {
                contextBar.progressBar.setCurrent(currentVideoNumber, false);
                isSeekAllowed = false;
                final String path = friend.videoFromPath(videoId);
                videoView.setOnPreparedListener(new OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        videoView.seekTo((int) (curProgress * videoView.getDuration()));
                        isSeekAllowed = true;
                    }
                });
                videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        mp.reset();
                        isSeekAllowed = true;
                        return true;
                    }
                });
                videoView.setVideoPath(path);
            }

        } else if (seek) {
            videoView.seekTo((int) (curProgress * videoView.getDuration()));
        }
        return curProgress;
    }

    private class ZoomController {
        private boolean zoomed;
        private Runnable zoomRollback;
        private float zoomRatio = 0f;
        private PlayControlGestureRecognizer gestureRecognizer;
        private float initialX;
        private float initialY;
        private int initialWidth;
        private int initialHeight;
        private ValueAnimator zoomAnimator;
        VideoView videoView;
        ViewGroup videoParentView;
        private Presenter presenter;

        ZoomController(Presenter presenter) {
            this.presenter = presenter;
            ArrayList<View> views = new ArrayList<>();
            videoParentView = presenter.getVideoViewParent();
            videoView = presenter.getVideoView();
            switch (presenter.getType()) {
                case TRANSCRIPTION:
                    views.add(((TranscriptionPresenter) presenter).transcription);
                    break;
                case PLAYER:
                    views.add(((PlayerPresenter) presenter).fab);
                    views.add(((PlayerPresenter) presenter).messages);
                    break;
            }
            views.add(videoParentView);
            if (presenter.getType() == Presenter.Type.PLAYER) {
                views.addAll(nineViewGroup.getNineViews());
            }
            views.add(videoRootLayout);
            gestureRecognizer = new PlayControlGestureRecognizer(activity, videoRootLayout, views);
            videoRootLayout.setGestureRecognizer(gestureRecognizer);
        }

        void setEnabled(boolean enabled) {
            if (enabled) {
                gestureRecognizer.enable();
            } else {
                gestureRecognizer.disable(true);
            }
        }

        public void clearState() {
            videoView.setCropFraction(1f);
            if (zoomAnimator != null) {
                zoomAnimator.cancel();
            }
            zoomRatio = 0f;
            zoomed = false;
        }

        private class PlayControlGestureRecognizer extends ViewGroupGestureRecognizer.Stub {
            private long lastTime;
            private long previousLastTime;
            private long startTime;
            private boolean isInited;
            private int gestureDirection;
            private double gestureSign;
            private double startOffsetX;
            private double startOffsetY;
            private double previousOffsetX;
            private double previousOffsetY;
            private boolean isFirstMove = false;
            private boolean isNextPreviousAdvancePossible = false;

            public PlayControlGestureRecognizer(Activity a, ViewGroup vg, ArrayList<View> tvs) {
                super(a, tvs);
            }

            @Override
            public boolean click(View v) {
                if (videoParentView.equals(v)) {
                    if (videoView.isPlaying()) {
                        videoView.pause();
                        contextBar.progressBar.pause();
                    } else {
                        videoView.start();
                        int duration = videoView.getDuration() - videoView.getCurrentPosition();
                        contextBar.progressBar.animateProgress(-1, 0, duration);
                    }
                } else {
                    if (isPlaying()) {
                        animateZoom(false);
                        stop();
                        notifyCompletion();
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean shouldHandle(View view) {
                boolean result;
                if (presenter.getType() == Presenter.Type.TRANSCRIPTION) {
                    result = ((TranscriptionPresenter) presenter).transcription != view;
                } else {
                    if (presenter.getType() == Presenter.Type.PLAYER) {
                        switch (view.getId()) {
                            case R.id.fab:
                            case R.id.messages:
                                return false;
                            default:
                                return super.shouldHandle(view);
                        }
                    }
                    result = super.shouldHandle(view);
                }
                return result;
            }

            @Override
            public boolean startLongpress(View v) {
                if (videoRootLayout.equals(v)) {
                    stop();
                    return true;
                } else if (zoomed) {
                    return false; // Ignore longpress if it is in fullscreen
                } else if (videoParentView.equals(v)) {
                    Object boxId = videoParentView.getTag(R.id.box_id);
                    if (boxId != null) {
                        v = nineViewGroup.getFrame(NineViewGroup.Box.values()[(Integer) boxId]);
                    }
                }
                return nineViewGroup.getGestureRecognizer().startLongpress(v);
            }

            @Override
            public boolean endLongpress(View v) {
                if (videoRootLayout.equals(v)) {
                    return true;
                }
                if (zoomed) {
                    return false; // Ignore longpress if it is in fullscreen
                }
                if (videoParentView.equals(v)) {
                    Object boxId = videoParentView.getTag(R.id.box_id);
                    if (boxId != null) {
                        v = nineViewGroup.getFrame(NineViewGroup.Box.values()[(Integer) boxId]);
                    }
                }
                return nineViewGroup.getGestureRecognizer().endLongpress(v);
            }

            @Override
            public boolean bigMove(View v) {
                if (videoRootLayout.equals(v)) {
                    return false;
                }
                if (videoParentView.equals(v)) {
                    Object boxId = videoParentView.getTag(R.id.box_id);
                    if (boxId != null) {
                        v = nineViewGroup.getFrame(NineViewGroup.Box.values()[(Integer) boxId]);
                    }
                }
                return nineViewGroup.getGestureRecognizer().bigMove(v);
            }

            @Override
            public boolean abort(View v, int reason) {
                if (videoRootLayout.equals(v)) {
                    return false;
                }
                if (videoParentView.equals(v)) {
                    Object boxId = videoParentView.getTag(R.id.box_id);
                    if (boxId != null) {
                        v = nineViewGroup.getFrame(NineViewGroup.Box.values()[(Integer) boxId]);
                    }
                }
                return nineViewGroup.getGestureRecognizer().abort(v, reason);
            }

            @Override
            public void notifyMove(View target, double startX, double startY, double offsetX, double offsetY) {
                if (!isInited) {
                    return;
                }
                previousLastTime = lastTime;
                lastTime = System.nanoTime();
                if (gestureDirection == DIRECTION_HORIZONTAL) {
                    if (!isFirstMove) {
                        if (Math.signum(offsetX - previousOffsetX) != gestureSign
                                || (lastTime - startTime) > FLING_TIME * 1000000) {
                            isNextPreviousAdvancePossible = false;
                        }
                    }
                    if (!isNextPreviousAdvancePossible) {
                        MotionEvent event = MotionEvent.obtain(startTime, lastTime, MotionEvent.ACTION_MOVE, (float) (startX + offsetX), 0f, 0);
                        contextBar.handleProgressBarTouchEvent(event);
                    }
                }
                previousOffsetX = offsetX;
                previousOffsetY = offsetY;
                isFirstMove = false;
            }

            @Override
            public void startMove(View target, double startX, double startY, double offsetX, double offsetY) {
                if (!(zoomAnimator != null && zoomAnimator.isStarted())) {
                    startOffsetX = offsetX;
                    startOffsetY = offsetY;
                    previousOffsetX = offsetX;
                    previousOffsetY = offsetY;
                    isInited = true;
                    isFirstMove = true;
                    if (Math.abs(offsetX) <= Math.abs(offsetY)) {
                        if (videoParentView.equals(target) && isSlidingSupported(DIRECTION_VERTICAL)) {
                            gestureSign = Math.signum(offsetY);
                            if (zoomed) {
                                animateZoom(false);
                            } else {
                                animateToFullscreen();
                            }
                        }
                    } else if (isSlidingSupported(DIRECTION_HORIZONTAL)) {
                        gestureDirection = DIRECTION_HORIZONTAL;
                        isNextPreviousAdvancePossible = Features.FLING_FEATURE_ALLOWED;
                        gestureSign = Math.signum(offsetX);
                        startTime = System.nanoTime();
                        lastTime = previousLastTime;
                        MotionEvent event = MotionEvent.obtain(startTime, lastTime, MotionEvent.ACTION_DOWN, (float) (startX + offsetX), 0f, 0);
                        contextBar.handleProgressBarTouchEvent(event);
                    }
                }
            }

            @Override
            public void endMove(double startX, double startY, double offsetX, double offsetY) {
                if (!isInited) {
                    return;
                }
                Log.i(TAG, "endMove");
                if ((System.nanoTime() - startTime) > FLING_TIME * 1000000) {
                    isNextPreviousAdvancePossible = false;
                }
                if (gestureDirection == DIRECTION_HORIZONTAL) {
                    if (isNextPreviousAdvancePossible) {
                        if (offsetX > 0) {      // Swipe right
                            if (currentVideoNumber != numberOfMessages) {
                                setCurrentVideoToNext();
                                play();
                            }
                        } else {                // Swipe left
                            if (videoView.getCurrentPosition() < 5000) {
                                setCurrentVideoToPrevious();
                            }
                            play();
                        }
                    } else {
                        MotionEvent event = MotionEvent.obtain(startTime, lastTime, MotionEvent.ACTION_UP, (float) (startX + offsetX), 0f, 0);
                        contextBar.handleProgressBarTouchEvent(event);
                    }
                }
                gestureDirection = 0;
                isNextPreviousAdvancePossible = false;
                isInited = false; // TODO after parking animation
            }

            @Override
            public boolean isSliding() {
                return false;
            }

            @Override
            public boolean isSlidingSupported(int direction) {
                switch (direction) {
                    case DIRECTION_HORIZONTAL:
                        return managerProvider.getFeatures().isUnlocked(Features.Feature.PAUSE_PLAYBACK);
                    case DIRECTION_VERTICAL:
                        return presenterHelper.getCurrentPresenter().getType() == Presenter.Type.PLAYER && managerProvider.getFeatures().isUnlocked(Features.Feature.PLAY_FULLSCREEN);
                }
                return false;
            }

            @Override
            public boolean isAbortGestureAllowed() {
                return nineViewGroup.getGestureRecognizer().isAbortGestureAllowed();
            }
        }

        public void animateToFullscreen() {
            initialWidth = videoParentView.getWidth();
            initialHeight = videoParentView.getHeight();
            initialX = videoParentView.getTranslationX();
            initialY = videoParentView.getTranslationY();
            animateZoom(true);
        }

        private void animateZoom(final boolean zoomIn) {
            if (zoomed == zoomIn) {
                return;
            }
            zoomAnimator = ValueAnimator.ofFloat(zoomRatio, (zoomIn) ? 1 : 0);
            zoomAnimator.setInterpolator(new DecelerateInterpolator());
            zoomAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                int w = initialWidth;
                int h = initialHeight;
                float trX = initialX;
                float trY = initialY;
                Point maxVideoSize = videoView.getMaxVideoSize(
                        videoRootLayout.getWidth() - videoView.getLeft() - videoParentView.getWidth() + videoView.getRight(),
                        videoRootLayout.getHeight() - videoView.getTop() - videoParentView.getHeight() + videoView.getBottom());
                int maxVideoBodyWidth = maxVideoSize.x + videoView.getLeft() + videoParentView.getWidth() - videoView.getRight();
                int maxVideoBodyHeight = maxVideoSize.y + videoView.getTop() + videoParentView.getHeight() - videoView.getBottom();
                int zoomInHorizontalPadding = (videoRootLayout.getWidth() - maxVideoBodyWidth) / 2;
                int zoomInVerticalPadding = (videoRootLayout.getHeight() - maxVideoBodyHeight) / 2;

                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float value = (float) animation.getAnimatedValue();
                    videoView.setCropFraction(1 - value);
                    videoParentView.setTranslationX(trX * (1 - value) + zoomInHorizontalPadding * value);
                    videoParentView.setTranslationY(trY * (1 - value) + zoomInVerticalPadding * value);
                    ViewGroup.LayoutParams p = videoParentView.getLayoutParams();
                    p.width = (int) (w + (maxVideoBodyWidth - w) * value);
                    p.height = (int) (h + (maxVideoBodyHeight - h) * value);
                    videoParentView.setLayoutParams(p);
                    zoomRatio = value;
                }
            });
            zoomAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    zoomed = zoomIn;
                }
            });
            zoomAnimator.setDuration(500);
            zoomAnimator.start();
        }
    }

    static class TranscriptionPresenter extends BasePresenter {
        @InjectView(R.id.video_view) VideoView videoView;
        @InjectView(R.id.video_body) ViewGroup videoBody;
        @InjectView(R.id.transcription) RecyclerView transcription;
        private boolean isAnimated = false;
        private boolean isAnimating = false;

        private float targetX;
        private float targetY;
        private int targetWidth;
        private int targetHeight;

        TranscriptionPresenter(ViewStub viewStub) {
            rootLayout = viewStub.inflate();
            ButterKnife.inject(this, rootLayout);
            transcription.setLayoutManager(new LinearLayoutManager(rootLayout.getContext(), LinearLayoutManager.VERTICAL, false));
        }

        @Override
        public Type getType() {
            return Type.TRANSCRIPTION;
        }

        @Override
        public VideoView getVideoView() {
            return videoView;
        }

        @Override
        public ViewGroup getVideoViewParent() {
            return videoBody;
        }

        @Override
        public void setupPlayerOverTarget(@NonNull View targetView, @NonNull View rootView) {
            targetX = targetView.getX();
            targetY = targetView.getY();
            targetWidth = targetView.getWidth();
            targetHeight = targetView.getHeight();
            videoBody.setTag(R.id.box_id, targetView.getId());
            isAnimated = false;
            if (!isAnimating) {
                videoBody.setX(targetX);
                videoBody.setY(targetY);
                ViewGroup.LayoutParams p = videoBody.getLayoutParams();
                p.width = targetWidth;
                p.height = targetHeight;
                videoBody.setLayoutParams(p);
                transcription.setAlpha(0f);
            }
        }

        @Override
        public void update(final VideoPlayer player) {
            //player.timer.schedule(new TimerTask() {
            //    @Override
            //    public void run() {
            //        player.activity.runOnUiThread(new Runnable() {
            //            @Override
            //            public void run() {
            //                transcriptionViewHolder.setInMode(TranscriptionViewHolder.ViewMode.MESSAGE,
            //                        "Some message text\nSecond row...",
            //                        StringUtils.getEventTime(String.valueOf(System.currentTimeMillis())));
            //            }
            //        });
            //    }
            //}, 2500);
        }

        @Override
        public void doContentAppearing(Context context) {
            super.doContentAppearing(context);
            if (isAnimated) {
                return;
            }
            Resources res = context.getResources();
            final int endW = res.getDimensionPixelSize(R.dimen.transcription_video_width);
            final int endH = res.getDimensionPixelSize(R.dimen.transcription_video_height);
            ValueAnimator zoomAnimator = ValueAnimator.ofFloat(0, 1);
            zoomAnimator.setInterpolator(new FastOutSlowInInterpolator());
            zoomAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                int w = targetWidth;
                int h = targetHeight;
                float startX = targetX;
                float startY = targetY;
                int maxVideoBodyWidth = endW;
                int maxVideoBodyHeight = endH;

                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float value = (float) animation.getAnimatedValue();
                    //videoView.setCropFraction(0);
                    videoBody.setX(startX * (1 - value));
                    videoBody.setY(startY * (1 - value));
                    ViewGroup.LayoutParams p = videoBody.getLayoutParams();
                    p.width = (int) (w + (maxVideoBodyWidth - w) * value);
                    p.height = (int) (h + (maxVideoBodyHeight - h) * value);
                    videoBody.setLayoutParams(p);
                    transcription.setPivotX(transcription.getX());
                    transcription.setPivotY(transcription.getY());
                    transcription.setScaleX(0.7f + value * 0.3f);
                    transcription.setScaleY(0.7f + value * 0.3f);
                    transcription.setAlpha(value);
                }
            });
            zoomAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    isAnimated = true;
                    isAnimating = false;
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    isAnimating = true;
                }
            });
            zoomAnimator.setDuration(600);
            zoomAnimator.start();
        }
    }

    static class PlayerPresenter extends BasePresenter {
        @InjectView(R.id.video_view) VideoView videoView;
        @InjectView(R.id.video_body) ViewGroup videoBody;
        @InjectView(R.id.tw_date) TextView date;
        @InjectView(R.id.fab) FloatingActionButton fab;
        @InjectView(R.id.messages) RecyclerView messages;

        PlayerPresenter(ViewStub viewStub) {
            rootLayout = viewStub.inflate();
            ButterKnife.inject(this, rootLayout);
            date.setTypeface(Convenience.getTypeface(date.getContext()));
            messages.setLayoutManager(new LinearLayoutManager(rootLayout.getContext(), LinearLayoutManager.VERTICAL, false));
            messages.setVisibility(View.INVISIBLE);
        }

        @Override
        public Type getType() {
            return Type.PLAYER;
        }

        @Override
        public VideoView getVideoView() {
            return videoView;
        }

        @Override
        public ViewGroup getVideoViewParent() {
            return videoBody;
        }

        @Override
        public void setupPlayerOverTarget(@NonNull View targetView, @NonNull View rootView) {
            videoBody.setX(targetView.getX());
            videoBody.setY(targetView.getY());
            videoBody.setTag(R.id.box_id, targetView.getId());
            if (targetView.getY() < rootView.getHeight() / 4) {
                date.setY(targetView.getY() + targetView.getHeight()); // for top row place it below
            } else {
                int dateHeight = date.getLineHeight() + Convenience.dpToPx(date.getContext(), 3);
                date.setY(targetView.getY() - dateHeight); // for other place it above
            }

            date.setX(targetView.getX());
            ViewGroup.LayoutParams p = videoBody.getLayoutParams();
            p.width = targetView.getWidth();
            p.height = targetView.getHeight();
            videoBody.setLayoutParams(p);

            NineViewGroup.Box box = NineViewGroup.Box.values()[(int) videoBody.getTag(R.id.box_id)];
            Resources res = targetView.getResources();
            int width = rootView.getWidth() - res.getDimensionPixelSize(R.dimen.messages_list_horizontal_padding) * 2;
            int height = rootView.getHeight() - res.getDimensionPixelSize(R.dimen.messages_list_top_padding)
                    - res.getDimensionPixelSize(R.dimen.messages_list_bottom_padding);

            messages.setPivotX(width * ((box.isLeft() ? 0 : (box.isRight() ? 1 : 0.5f))));
            messages.setPivotY(height * ((box.isTop() ? 0 : (box.isBottom() ? 1 : 0.5f))));
        }

        @Override
        public void update(VideoPlayer player) {
            TextAnimations.animateAlpha(date, StringUtils.getEventTime(player.videoId));
        }

        @Override
        public void doContentAppearing(Context context) {
            super.doContentAppearing(context);
            //messages.setPivotX(messages.getX());
            //messages.setPivotY(messages.getY());
            //messages.setScaleX(0.7f + value * 0.3f);
            //messages.setScaleY(0.7f + value * 0.3f);
            //messages.setAlpha(value);
        }

        @Override
        public void startPlayback(String path, float progress, VideoPlayer player) {
            fab.hide();
            if (messages.getVisibility() != View.INVISIBLE) {
                messages.animate().alpha(0f).scaleX(0.5f).scaleY(0f).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        messages.setVisibility(View.INVISIBLE);
                    }
                }).start();
            }
            super.startPlayback(path, progress, player);
        }

        public void startMessagesPresentation(Context context, MessageContainer<IncomingMessage> messageContainer, final VideoPlayer player) {
            List<MessageContainer<IncomingMessage>> list = new ArrayList<>(1);
            list.add(messageContainer);
            messages.setAdapter(new MessageAdapter(list, context));
            if (messages.getVisibility() != View.VISIBLE) {
                messages.setVisibility(View.VISIBLE);
                messages.setAlpha(0);
                messages.setScaleX(0.5f);
                messages.setScaleY(0);
                messages.animate().alpha(1f).scaleX(1f).scaleY(1f).setListener(null).start();
            }
            fab.show();
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    player.onCompletion(null);
                }
            });
            player.isSeekAllowed = true;
            player.presenterHelper.showContent();
            player.notifyStartPlaying();
        }

        @Override
        public void stopPlayback() {
            super.stopPlayback();
            messages.setVisibility(View.INVISIBLE);
        }
    }

    static abstract class BasePresenter implements Presenter {
        View rootLayout;
        private boolean isPresenting;
        @Override
        public void stopPlayback() {
            VideoView videoView = getVideoView();
            ViewGroup videoRoot = getVideoViewParent();
            if (videoView != null && videoRoot != null) {
                //need to clear videoView because of last frame of already viewed video appear before new one start playing
                //TODO need to fix delay with black frame (or first video frame)
                //videoView.setVisibility(View.INVISIBLE);
                //videoView.setVisibility(View.VISIBLE);
                videoView.stopPlayback();
                videoView.setVideoURI(null);
                videoView.suspend();
            }
            isPresenting = false;
        }

        @Override
        public void startPlayback(final String path, final float progress, final VideoPlayer player) {
            final VideoView videoView = getVideoView();
            if (videoView != null) {
                videoView.setOnPreparedListener(new OnPreparedListener() {
                    boolean firstOpening = true;
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        Log.i(TAG, "video duration " + videoView.getDuration() + " " + path);
                        if (firstOpening) {
                            videoView.seekTo((int) (videoView.getDuration() * progress));
                            firstOpening = false;
                        }
                        videoView.setVolume(player.mediaVolume);
                        videoView.start();
                        player.waitAndNotifyWhenStart();
                    }
                });
                videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        final String brokenVideoId = player.videoId;
                        player.cancelWaitingForStart();
                        mp.reset();
                        player.onCompletion(mp);
                        //friend.setAndNotifyIncomingVideoStatus(brokenVideoId, IncomingMessage.Status.FAILED_PERMANENTLY);
                        player.notifyPlaybackError();
                        Dispatch.dispatch(String.format("Error while playing video %s %d %d", brokenVideoId, what, extra));
                        return true;
                    }
                });
                videoView.setVideoPath(path);
            }
        }

        @Override
        public boolean isPlaying() {
            VideoView view = getVideoView();
            return view != null && (view.isPlaying() || view.isPaused()) || isPresenting;
        }

        @Override
        public void setVisible(boolean visible) {
            if (rootLayout != null) {
                rootLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        }

        @Override
        public void doContentAppearing(Context context) {
            isPresenting = true;
        }
    }

    private interface Presenter {
        enum Type {
            PLAYER(R.id.player_layout_stub, PlayerPresenter.class, new VideoContextBarPreferences(true, false, false)),
            TRANSCRIPTION(R.id.transcription_layout_stub, TranscriptionPresenter.class, new VideoContextBarPreferences(false, true, true));

            int stubId;

            Class<? extends Presenter> presenterClass;

            VideoContextBarPreferences barPreferences;

            Type(int stubId, Class<? extends Presenter> clazz, VideoContextBarPreferences barPrefs) {
                this.stubId = stubId;
                this.barPreferences = barPrefs;
                presenterClass = clazz;
            }

            Presenter getInstance(ViewStub stub) {
                try {
                    Constructor<? extends Presenter> constructor = presenterClass.getDeclaredConstructor(ViewStub.class);
                    return constructor.newInstance(stub);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }
        Type getType();

        VideoView getVideoView();
        ViewGroup getVideoViewParent();

        void stopPlayback();
        void startPlayback(String path, float progress, VideoPlayer player);
        void setupPlayerOverTarget(@NonNull View targetView, @NonNull View rootView);
        boolean isPlaying();
        void setVisible(boolean visible);
        void update(VideoPlayer player);
        void doContentAppearing(Context context);
    }

    private class PresenterHelper {
        Map<String, Presenter> presenters = new HashMap<>();

        private Presenter currentPresenter;

        Presenter setCurrentPresentation(Presenter.Type type) {
            String key = type.presenterClass.getSimpleName();
            Presenter presenter = presenters.get(key);
            if (presenter == null) {
                ViewStub viewStub = ButterKnife.findById(videoRootLayout, type.stubId);
                if (viewStub != null) {
                    presenter = type.getInstance(viewStub);
                    presenters.put(key, presenter);
                    presenter.getVideoView().setOnCompletionListener(VideoPlayer.this);
                    presenter.getVideoView().setOnPreparedListener(VideoPlayer.this);
                } else {
                    Dispatch.dispatch(new RuntimeException("viewStub was inflated already"), "" + type);
                }
            }
            if (presenter != currentPresenter) {
                currentPresenter = presenter;
                zoomController = new ZoomController(presenter);
                for (Presenter p : presenters.values()) {
                    p.setVisible(p == presenter);
                }
            }
            return presenter;
        }

        void stopPresentation() {
            if (currentPresenter != null) {
                VideoContextBarPreferences contextBarPrefs = currentPresenter.getType().barPreferences;
                if (!contextBarPrefs.hasDivider) {
                    actionBarDivider.animate().alpha(1f).start();
                }
                videoRootLayout.animate().alpha(0f).start();
                zoomController.setEnabled(false);
                contextBar.hide();
                currentPresenter.stopPlayback();
            }
        }

        void startPresentation(final String path, final float progress) {
            if (currentPresenter != null) {
                currentPresenter.startPlayback(path, progress, VideoPlayer.this);
            }
        }

        void initStateForTarget() {
            if (currentPresenter == null) {
                return;
            }
            View view = (targetViewRef != null) ? targetViewRef.get() : null;
            if (view != null) {
                currentPresenter.setupPlayerOverTarget(view, videoRootLayout);
            }
            currentPresenter.update(VideoPlayer.this);
        }

        Presenter getCurrentPresenter() {
            return currentPresenter;
        }

        void setVolume(float volume) {
            VideoView view = getVideoView();
            if (view != null) {
                view.setVolume(volume);
            }
        }

        void pause() {
            VideoView view = getVideoView();
            if (view != null) {
                view.pause();
            }
        }

        public boolean isPlaying() {
            return currentPresenter != null && currentPresenter.isPlaying();
        }

        VideoView getVideoView() {
            return (currentPresenter != null) ? currentPresenter.getVideoView() : null;
        }

        public void refresh() {
            if (currentPresenter != null) {
                currentPresenter.update(VideoPlayer.this);
            }
        }

        public void showContent() {
            videoRootLayout.animate().alpha(1).start();
            zoomController.setEnabled(true);
            if (playOptions.hasFlags(PlayOptions.FULLSCREEN)) {
                if (!zoomController.zoomed) {
                    zoomController.animateToFullscreen();
                }
                playOptions.clearFlags(PlayOptions.FULLSCREEN);
            }
            if (currentPresenter != null) {
                VideoContextBarPreferences contextBarPrefs = currentPresenter.getType().barPreferences;
                if (!contextBarPrefs.hasDivider) {
                    actionBarDivider.animate().alpha(0).start();
                }
                VideoView view = getVideoView();
                if (view != null) {
                    contextBar.setPreferences(contextBarPrefs);
                    contextBar.show(view.getDuration(), view.getCurrentPosition());
                    if (currentPresenter.getType() == Presenter.Type.TRANSCRIPTION) {
                        RecyclerView rv = ((TranscriptionPresenter) currentPresenter).transcription;
                        MessageAdapter adapter = (MessageAdapter) rv.getAdapter();
                        if (adapter == null) {
                            adapter = new MessageAdapter(playingMessages, view.getContext());
                            rv.setAdapter(adapter);
                        } else {
                            adapter.setList(playingMessages);
                            adapter.notifyDataSetChanged();
                        }
                    }
                    currentPresenter.doContentAppearing(view.getContext());
                }
            }
        }
    }

    class VideoContextBar implements View.OnTouchListener {
        @InjectView(R.id.progress_bar) VideoProgressBar progressBar;
        @InjectView(R.id.menu_view) MaterialMenuView menuView;
        @InjectView(R.id.mute) View mute;
        @InjectView(R.id.title) TextView title;

        private Animator appearingAnimation;
        private View rootView;

        public VideoContextBar(View contextBar) {
            this.rootView = contextBar;
            View.inflate(activity, R.layout.video_action_bar, (ViewGroup) contextBar);
            ButterKnife.inject(this, contextBar);
            menuView.setState(MaterialMenuDrawable.IconState.X);
            rootView.setOnTouchListener(this);
        }

        @Override
        @OnTouch({R.id.progress_bar})
        public boolean onTouch(View v, MotionEvent event) {
            if (v.getId() == R.id.progress_bar) {
                return !managerProvider.getFeatures().isUnlocked(Features.Feature.PAUSE_PLAYBACK) || handleProgressBarTouchEvent(event);
            }
            return true;
        }

        @OnClick(R.id.mute)
        public void onMuteClicked(View v) {
            v.setSelected(!v.isSelected());
            setVolume(v.isSelected() ? 1f : 0f);
        }

        @OnClick(R.id.menu_view)
        public void onMenuClicked(View v) {
            stop();
            zoomController.clearState();
            if (needToNotifyCompletion) {
                notifyCompletion();
            }
        }

        public void show(int videoDuration, int currentPosition) {
            VideoProgressBar.Scheme.SchemeBuilder schemeBuilder = new VideoProgressBar.Scheme.SchemeBuilder();
            List<MessageContainer<IncomingMessage>> containers = playingMessages;
            for (MessageContainer<IncomingMessage> container : containers) {
                switch (container.getType()) {
                    case TEXT:
                        schemeBuilder.addPoint(R.drawable.ic_menu_chat);
                        break;
                    case VIDEO:
                        schemeBuilder.addBar();
                        break;
                }
            }
            VideoProgressBar.Scheme scheme = schemeBuilder.build();
            if (scheme.getBarCount() == 0) {
                progressBar.setVisibility(View.GONE);
                title.setVisibility(View.VISIBLE);
                title.setText(friend.getFirstName());
                menuView.setVisibility(View.VISIBLE); // Override preference
                doAppearing();
            } else {
                progressBar.setVisibility(View.VISIBLE);
                title.setVisibility(View.GONE);
                progressBar.setScheme(scheme);
                progressBar.initState();
                doAppearing();
                progressBar.setCurrent(currentVideoNumber, true);
                int duration = videoDuration - currentPosition;
                float offset = (videoDuration >= 0) ? currentPosition / (float) videoDuration : 0f;
                progressBar.animateProgress(currentVideoNumber - 1, offset, duration);
            }

        }

        private void doAppearing() {
            if (appearingAnimation != null) {
                appearingAnimation.cancel();
            }
            appearingAnimation = VideoProgressBarAnimation.getTerminalAnimation(rootView, true);
            appearingAnimation.start();
            rootView.setVisibility(View.VISIBLE);
        }

        public void hide() {
            if (appearingAnimation != null) {
                appearingAnimation.cancel();
            }
            appearingAnimation = VideoProgressBarAnimation.getTerminalAnimation(rootView, false);
            appearingAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    rootView.setVisibility(View.INVISIBLE);
                }
            });
            appearingAnimation.start();
            progressBar.dropState();
            mute.setSelected(false);
        }

        public boolean handleProgressBarTouchEvent(MotionEvent event) {
            // TODO Need refactor
            int action = event.getAction();
            float x = event.getX();
            float progress;
            if (x <= progressBar.getLayoutPadding()) {
                progress = 0f;
            } else if (x >= progressBar.getWidth() - progressBar.getLayoutPadding()) {
                progress = 1f;
            } else {
                progress = (x - progressBar.getLayoutPadding()) / (progressBar.getWidth() - progressBar.getLayoutPadding() * 2);
            }
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    presenterHelper.setVolume(0f);
                    presenterHelper.pause();
                    progressBar.pause();
                    break;
                case MotionEvent.ACTION_UP:
                    presenterHelper.setVolume(mediaVolume);
                    float currentVideoProgress = setVideoProgress(progress, false, true);
                    play(currentVideoProgress);
                    break;
                case MotionEvent.ACTION_MOVE:
                    setVideoProgress(progress, true, isSeekAllowed);
                    break;
                case MotionEvent.ACTION_CANCEL:
                    presenterHelper.setVolume(mediaVolume);
                    break;
            }
            return true;
        }

        public void setPreferences(VideoContextBarPreferences prefs) {
            menuView.setVisibility(prefs.showCloseButton ? View.VISIBLE : View.GONE);
            mute.setVisibility(prefs.showMuteButton ? View.VISIBLE : View.GONE);
        }
    }
}
