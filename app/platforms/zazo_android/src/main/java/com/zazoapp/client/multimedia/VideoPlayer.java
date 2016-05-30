package com.zazoapp.client.multimedia;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.support.annotation.FloatRange;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.zazoapp.client.Config;
import com.zazoapp.client.R;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.IncomingVideo;
import com.zazoapp.client.network.FileDownloadService;
import com.zazoapp.client.network.FileTransferService;
import com.zazoapp.client.ui.BaseManagerProvider;
import com.zazoapp.client.ui.ViewGroupGestureRecognizer;
import com.zazoapp.client.ui.animations.TextAnimations;
import com.zazoapp.client.ui.view.GestureControlledLayout;
import com.zazoapp.client.ui.view.NineViewGroup;
import com.zazoapp.client.ui.view.TouchBlockScreen;
import com.zazoapp.client.ui.view.VideoProgressBar;
import com.zazoapp.client.ui.view.VideoView;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.DialogShower;
import com.zazoapp.client.utilities.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class VideoPlayer implements OnCompletionListener, OnPreparedListener, Player, View.OnTouchListener {

    private static final String TAG = VideoPlayer.class.getSimpleName();

    private FragmentActivity activity;
    private String videoId;
    private String friendId;
    private Friend friend;
    private int currentVideoNumber = 0;
    private int numberOfVideos = 0;
    @InjectView(R.id.video_view) VideoView videoView;
    @InjectView(R.id.video_body) ViewGroup videoBody;
    @InjectView(R.id.video_root_layout) GestureControlledLayout videoRootLayout;
    @InjectView(R.id.tw_date) TextView twDate;
    private TouchBlockScreen blockScreen;
    private VideoProgressBar progressBar;
    @InjectView(R.id.grid_view) NineViewGroup nineViewGroup;
	private boolean videosAreDownloading;
    private BaseManagerProvider managerProvider;
    private Timer timer = new Timer();
    private TimerTask onStartTask;
    private ZoomController zoomController;
    private float mediaVolume = 1.0f;

    private Set<StatusCallbacks> statusCallbacks = new HashSet<StatusCallbacks>();
    private List<IncomingVideo> playingVideos;
    private volatile boolean isSeekAllowed = true;

    public VideoPlayer(FragmentActivity activity, BaseManagerProvider managerProvider) {
        this.activity = activity;
        this.managerProvider = managerProvider;
        blockScreen = ButterKnife.findById(activity, R.id.block_screen);
        progressBar = ButterKnife.findById(activity, R.id.video_progress_bar);
        progressBar.setOnTouchListener(this);
    }

    @Override
    public void init(View rootView) {
        ButterKnife.inject(this, rootView);
        this.videoView.setOnCompletionListener(this);
        this.videoView.setOnPreparedListener(this);
        twDate.setTypeface(Convenience.getTypeface(activity));
        zoomController = new ZoomController();
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
    }

    @Override
    public boolean togglePlayOverView(View view, String friendId) {
        boolean needToPlay = !(isPlaying() && friendId.equals(this.friendId));

        // Always stop first so that the notification goes out to reset the view we were on in case it was still playing and we are switching to another view.
        stop();

        this.friendId = friendId;
        friend = FriendFactory.getFactoryInstance().find(friendId);

        if (needToPlay) {
            setPlayerOverView(view);
            return start();
        }
        return false;
    }

    @Override
    public void stop(){
        Log.i(TAG, "stop");
        if (videoView != null && videoBody != null) {
            videoRootLayout.animate().alpha(0f).start();
            zoomController.setEnabled(false);
            progressBar.doDisappearing();
            //need to clear videoView because of last frame of already viewed video appear before new one start playing
            //TODO need to fix delay with black frame (or first video frame)
            //videoView.setVisibility(View.INVISIBLE);
            //videoView.setVisibility(View.VISIBLE);
            videoView.stopPlayback();
            videoView.setVideoURI(null);
            videoView.suspend();
        }
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
        videoView.setVolume(volume);
        mediaVolume = volume;
    }

    //----------------------
	// Private state machine
	//----------------------
	private boolean start(){
		Log.i(TAG, "start");

		determineIfDownloading();
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

    private void play(@FloatRange(from = 0.0f, to = 1.0f, toInclusive = false) final float progress) {
        Log.i(TAG, "play");
        if (!managerProvider.getAudioController().gainFocus()) {
            DialogShower.showToast(activity, R.string.toast_could_not_get_audio_focus);
            return;
        }
        // Always set it to viewed whether it is playable or not so it eventually gets deleted.
        friend.setAndNotifyIncomingVideoStatus(videoId, IncomingVideo.Status.VIEWED);

        if (videoIsPlayable()) {
            managerProvider.getAudioController().setSpeakerPhoneOn(true);
            final String path = friend.videoFromPath(videoId);
            videoView.setOnPreparedListener(new OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Log.i(TAG, "video duration " + videoView.getDuration() + " " + path);
                    videoView.seekTo((int) (videoView.getDuration() * progress));
                    videoView.start();
                    waitAndNotifyWhenStart();
                }
            });
            videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    final String brokenVideoId = videoId;
                    cancelWaitingForStart();
                    mp.reset();
                    onCompletion(mp);
                    //friend.setAndNotifyIncomingVideoStatus(brokenVideoId, IncomingVideo.Status.FAILED_PERMANENTLY);
                    notifyPlaybackError();
                    Dispatch.dispatch(String.format("Error while playing video %s %d %d", brokenVideoId, what, extra));
                    return true;
                }
            });
            videoView.setVideoPath(path);

        } else {
            onCompletion(null);
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
            stop();
            zoomController.clearState();
            notifyCompletion();
        }
    }

    //---------------
    // Helper methods
    //---------------
    private void setPlayerOverView(View view) {
        zoomController.clearState();
        LayoutParams params = new FrameLayout.LayoutParams(view.getWidth(), view.getHeight());
        videoBody.setLayoutParams(params);
        videoBody.setX(view.getX());
        videoBody.setY(view.getY());
        videoBody.setTag(R.id.box_id, view.getId());
        if (view.getY() < videoRootLayout.getHeight() / 4) {
            twDate.setY(view.getY() + view.getHeight()); // for top row place it below
        } else {
            twDate.setY(view.getY() - twDate.getHeight()); // for other place it above
        }
        twDate.setX(view.getX());
        twDate.setText("");
    }

    private void determineIfDownloading() {
        videosAreDownloading = friend.hasDownloadingVideo();
    }

    private void setCurrentVideoToFirst() {
        playingVideos = (videosAreDownloading) ? friend.getSortedIncomingNotViewedVideos() : friend.getSortedIncomingPlayableVideos();
        setCurrentVideoId(Friend.getFirstVideoIdInList(playingVideos));
        currentVideoNumber = (videoId != null) ? 1 : 0;
        numberOfVideos = playingVideos.size();
    }

    private void setCurrentVideoToNext() {
        playingVideos = (videosAreDownloading) ? friend.getSortedIncomingNotViewedVideos() : friend.getSortedIncomingPlayableVideos();
        int posId = Friend.getNextVideoPositionInList(videoId, playingVideos);
        currentVideoNumber = posId + 1;
        numberOfVideos = playingVideos.size();
        setCurrentVideoId((posId >= 0) ? playingVideos.get(posId).getId() : null);
    }

    private void setCurrentVideoToPrevious() {
        playingVideos = (videosAreDownloading) ? friend.getSortedIncomingNotViewedVideos() : friend.getSortedIncomingPlayableVideos();
        int posId = Friend.getCurrentVideoPositionInList(videoId, playingVideos) - 1;
        if (posId < 0) {
            return;
        }
        currentVideoNumber = posId + 1;
        numberOfVideos = playingVideos.size();
        setCurrentVideoId((posId >= 0) ? playingVideos.get(posId).getId() : null);
    }

    private void jumpToVideo(int pos) {
        if (playingVideos == null || playingVideos.isEmpty() || pos < 0 || playingVideos.size() < pos || pos + 1 == currentVideoNumber) {
            return;
        }
        currentVideoNumber = pos + 1;
        setCurrentVideoId(playingVideos.get(pos).getId());
    }

    private void setCurrentVideoId(String videoId) {
        this.videoId = videoId;
        TextAnimations.animateAlpha(twDate, StringUtils.getEventTime(videoId));
    }

    public boolean isPlaying(){
        return videoView != null && (videoView.isPlaying() || videoView.isPaused());
    }

    @Override
    public void rewind(int msec) {
        if (isPlaying()) {
            int current = videoView.getCurrentPosition();
            int length = videoView.getDuration();
            if (length > 0) {
                int next = current + msec;
                if (next < 0) {
                    next = 0;
                } else if (next > length) {
                    next = length - 1;
                }
                videoView.seekTo(next);
            }
        }
    }

    @Override
    public void restartAfter(int delay) {
        videoView.pause();
        videoView.postDelayed(new Runnable() {
            @Override
            public void run() {
                videoView.seekTo(0);
                videoView.start();
            }
        }, delay);
    }

    @Override
    public void changeAudioStream() {
        if (isPlaying() && !managerProvider.getAudioController().isSpeakerPhoneOn()) {
            blockScreen.lock();
        } else if (blockScreen.isLocked()) {
            blockScreen.unlock(true);
        }
        videoView.changeAudioStream(managerProvider.getAudioController().isSpeakerPhoneOn());
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
                videoView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (onStartTask != null && videoView.getCurrentPosition() > 0) {
                            videoView.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    // checks if it is playing to eliminate the case of released player
                                    if (videoView.isPlaying()) {
                                        isSeekAllowed = true;
                                        videoRootLayout.animate().alpha(1).start();
                                        zoomController.setEnabled(true);
                                        VideoProgressBar.Scheme.SchemeBuilder schemeBuilder = new VideoProgressBar.Scheme.SchemeBuilder();
                                        for (int i = 0; i < numberOfVideos; i++) {
                                            schemeBuilder.addBar();
                                        }
                                        progressBar.setScheme(schemeBuilder.build());
                                        progressBar.doAppearing();
                                        progressBar.setCurrent(currentVideoNumber, true);
                                        int duration = videoView.getDuration() - videoView.getCurrentPosition();
                                        float offset = (videoView.getDuration() >= 0) ?
                                                videoView.getCurrentPosition() / (float) videoView.getDuration() : 0f;
                                        progressBar.animateProgress((currentVideoNumber - 1 + offset) / (float) numberOfVideos,
                                                currentVideoNumber / (float) numberOfVideos, duration);
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

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.video_progress_bar) {
            if (managerProvider.getFeatures().isUnlocked(Features.Feature.PAUSE_PLAYBACK)) {
                return handleProgressBarTouchEvent(event);
            }
        }
        return false;
    }

    private boolean handleProgressBarTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float x = event.getX();
        float progress = x / progressBar.getWidth();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                videoView.setVolume(0f);
                videoView.pause();
                progressBar.pause();
                break;
            case MotionEvent.ACTION_UP:
                videoView.setVolume(mediaVolume);
                float currentVideoProgress = setVideoProgress(progress, false, true);
                play(currentVideoProgress);
                break;
            case MotionEvent.ACTION_MOVE:
                setVideoProgress(progress, true, isSeekAllowed);
                break;
            case MotionEvent.ACTION_CANCEL:
                videoView.setVolume(mediaVolume);
                break;
        }
        return true;
    }

    /**
     *
     * @param fullProgress
     * @param allowLoad
     * @param seek
     * @return currentVideoProgress
     */
    private float setVideoProgress(float fullProgress, boolean allowLoad, boolean seek) {
        progressBar.setProgress(fullProgress);
        int pos = (int) Math.floor(fullProgress * numberOfVideos);
        final float curProgress = fullProgress * numberOfVideos - pos;
        if ((currentVideoNumber != pos + 1)) {
            jumpToVideo(pos);
            if (allowLoad) {
                progressBar.setCurrent(currentVideoNumber, false);
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
                        isSeekAllowed = false;
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

        ZoomController() {
            ArrayList<View> views = new ArrayList<>();
            views.add(videoBody);
            views.addAll(nineViewGroup.getNineViews());
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
                if (videoBody.equals(v)) {
                    if (videoView.isPlaying()) {
                        videoView.pause();
                        progressBar.pause();
                    } else {
                        videoView.start();
                        int duration = videoView.getDuration() - videoView.getCurrentPosition();
                        progressBar.animateProgress(progressBar.getProgress(),
                                currentVideoNumber / (float) numberOfVideos, duration);
                    }
                } else {
                    if (isPlaying()) {
                        animateZoom(false);
                        stop();
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean startLongpress(View v) {
                if (videoRootLayout.equals(v)) {
                    stop();
                    return true;
                } else if (videoBody.equals(v)) {
                    v = nineViewGroup.getFrame(NineViewGroup.Box.values()[(Integer) videoBody.getTag(R.id.box_id)]);
                }
                return nineViewGroup.getGestureRecognizer().startLongpress(v);
            }

            @Override
            public boolean endLongpress(View v) {
                if (videoRootLayout.equals(v)) {
                    return true;
                }
                if (videoBody.equals(v)) {
                    v = nineViewGroup.getFrame(NineViewGroup.Box.values()[(Integer) videoBody.getTag(R.id.box_id)]);
                }
                return nineViewGroup.getGestureRecognizer().endLongpress(v);
            }

            @Override
            public boolean bigMove(View v) {
                if (videoRootLayout.equals(v)) {
                    return false;
                }
                if (videoBody.equals(v)) {
                    v = nineViewGroup.getFrame(NineViewGroup.Box.values()[(Integer) videoBody.getTag(R.id.box_id)]);
                }
                return nineViewGroup.getGestureRecognizer().bigMove(v);
            }

            @Override
            public boolean abort(View v, int reason) {
                if (videoRootLayout.equals(v)) {
                    return false;
                }
                if (videoBody.equals(v)) {
                    v = nineViewGroup.getFrame(NineViewGroup.Box.values()[(Integer) videoBody.getTag(R.id.box_id)]);
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
                        handleProgressBarTouchEvent(event);
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
                        if (videoBody.equals(target) && isSlidingSupported(DIRECTION_VERTICAL)) {
                            gestureSign = Math.signum(offsetY);
                            if (!zoomed) {
                                initialWidth = videoBody.getWidth();
                                initialHeight = videoBody.getHeight();
                                initialX = videoBody.getTranslationX();
                                initialY = videoBody.getTranslationY();
                            }
                            animateZoom(!zoomed);
                        }
                    } else if (isSlidingSupported(DIRECTION_HORIZONTAL)) {
                        gestureDirection = DIRECTION_HORIZONTAL;
                        isNextPreviousAdvancePossible = true;
                        gestureSign = Math.signum(offsetX);
                        startTime = System.nanoTime();
                        lastTime = previousLastTime;
                        MotionEvent event = MotionEvent.obtain(startTime, lastTime, MotionEvent.ACTION_DOWN, (float) (startX + offsetX), 0f, 0);
                        handleProgressBarTouchEvent(event);
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
                            if (currentVideoNumber != numberOfVideos) {
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
                        handleProgressBarTouchEvent(event);
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
                        return managerProvider.getFeatures().isUnlocked(Features.Feature.PLAY_FULLSCREEN);
                }
                return false;
            }

            @Override
            public boolean isAbortGestureAllowed() {
                return nineViewGroup.getGestureRecognizer().isAbortGestureAllowed();
            }
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
                        videoRootLayout.getWidth() - videoView.getLeft() - videoBody.getWidth() + videoView.getRight(),
                        videoRootLayout.getHeight() - videoView.getTop() - videoBody.getHeight() + videoView.getBottom());
                int maxVideoBodyWidth = maxVideoSize.x + videoView.getLeft() + videoBody.getWidth() - videoView.getRight();
                int maxVideoBodyHeight = maxVideoSize.y + videoView.getTop() + videoBody.getHeight() - videoView.getBottom();
                int zoomInHorizontalPadding = (videoRootLayout.getWidth() - maxVideoBodyWidth) / 2;
                int zoomInVerticalPadding = (videoRootLayout.getHeight() - maxVideoBodyHeight) / 2;

                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float value = (float) animation.getAnimatedValue();
                    videoView.setCropFraction(1 - value);
                    videoBody.setTranslationX(trX * (1 - value) + zoomInHorizontalPadding * value);
                    videoBody.setTranslationY(trY * (1 - value) + zoomInVerticalPadding * value);
                    ViewGroup.LayoutParams p = videoBody.getLayoutParams();
                    p.width = (int) (w + (maxVideoBodyWidth - w) * value);
                    p.height = (int) (h + (maxVideoBodyHeight - h) * value);
                    videoBody.setLayoutParams(p);
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
}
