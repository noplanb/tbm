package com.zazoapp.client.multimedia;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.zazoapp.client.R;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.IncomingVideo;
import com.zazoapp.client.network.FileDownloadService;
import com.zazoapp.client.network.FileTransferService;
import com.zazoapp.client.ui.ViewGroupGestureRecognizer;
import com.zazoapp.client.ui.ZazoManagerProvider;
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

public class VideoPlayer implements OnCompletionListener, OnPreparedListener, Player {

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
    private ZazoManagerProvider managerProvider;
    private Timer timer = new Timer();
    private TimerTask onStartTask;
    private ZoomController zoomController;

    private Set<StatusCallbacks> statusCallbacks = new HashSet<StatusCallbacks>();


    public VideoPlayer(FragmentActivity activity, ZazoManagerProvider managerProvider) {
        this.activity = activity;
        this.managerProvider = managerProvider;
        blockScreen = ButterKnife.findById(activity, R.id.block_screen);
        progressBar = ButterKnife.findById(activity, R.id.video_progress_bar);
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
    public void togglePlayOverView(View view, String friendId) {
        boolean needToPlay = !(isPlaying() && friendId.equals(this.friendId));

        // Always stop first so that the notification goes out to reset the view we were on in case it was still playing and we are switching to another view.
        stop();

        this.friendId = friendId;
        friend = FriendFactory.getFactoryInstance().find(friendId);

        if (needToPlay) {
            setPlayerOverView(view);
            start();
        }
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
    }

    //----------------------
	// Private state machine
	//----------------------
	private void start(){
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
		    return;
		}
		play();
        managerProvider.getTutorial().onVideoStartPlaying();
	}

    private void play(){
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
                    friend.setAndNotifyIncomingVideoStatus(brokenVideoId, IncomingVideo.Status.FAILED_PERMANENTLY);
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
        List<IncomingVideo> videos = (videosAreDownloading) ? friend.getSortedIncomingNotViewedVideos() : friend.getSortedIncomingPlayableVideos();
        setCurrentVideoId(Friend.getFirstVideoIdInList(videos));
        currentVideoNumber = (videoId != null) ? 1 : 0;
        numberOfVideos = videos.size();
    }

    private void setCurrentVideoToNext() {
        List<IncomingVideo> videos = (videosAreDownloading) ? friend.getSortedIncomingNotViewedVideos() : friend.getSortedIncomingPlayableVideos();
        int posId = Friend.getNextVideoPositionInList(videoId, videos);
        currentVideoNumber = posId + 1;
        numberOfVideos = videos.size();
        setCurrentVideoId((posId >= 0) ? videos.get(posId).getId() : null);
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
        return f.exists() && f.length() > 100;
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
                                        videoRootLayout.animate().alpha(1).start();
                                        zoomController.setEnabled(true);
                                        progressBar.doAppearing();
                                        progressBar.setCurrent(currentVideoNumber, true);
                                        int duration = videoView.getDuration() - videoView.getCurrentPosition();
                                        progressBar.animateProgress((currentVideoNumber - 1) / (float) numberOfVideos,
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

    private class ZoomController {
        private boolean zoomed;
        private Runnable zoomRollback;
        private float zoomRatio = 0f;
        private ZoomGestureRecognizer gestureRecognizer;
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
            gestureRecognizer = new ZoomGestureRecognizer(activity, videoRootLayout, views);
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

        private class ZoomGestureRecognizer extends ViewGroupGestureRecognizer.Stub {
            private long lastTime;
            private long previousLastTime;
            private boolean isInited;
            private double startOffsetX;
            private double startOffsetY;

            public ZoomGestureRecognizer(Activity a, ViewGroup vg, ArrayList<View> tvs) {
                super(a, tvs);
            }

            @Override
            public boolean click(View v) {
                if (videoBody.equals(v)) {
                    if (managerProvider.getFeatures().isUnlocked(Features.Feature.PAUSE_PLAYBACK)) {
                        if (videoView.isPlaying()) {
                            videoView.pause();
                            progressBar.pause();
                        } else {
                            videoView.start();
                            int duration = videoView.getDuration() - videoView.getCurrentPosition();
                            progressBar.animateProgress(progressBar.getProgress(),
                                    currentVideoNumber / (float) numberOfVideos, duration);
                        }
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
                if (!videoBody.equals(target)) {
                    return;
                }
                previousLastTime = lastTime;
                lastTime = System.nanoTime();
            }

            @Override
            public void startMove(View target, double startX, double startY, double offsetX, double offsetY) {
                if (videoBody.equals(target) && !(zoomAnimator != null && zoomAnimator.isStarted())) {
                    startOffsetX = offsetX;
                    startOffsetY = offsetY;
                    isInited = true;
                    if (!zoomed) {
                        initialWidth = videoBody.getWidth();
                        initialHeight = videoBody.getHeight();
                        initialX = videoBody.getTranslationX();
                        initialY = videoBody.getTranslationY();
                    }
                    animateZoom(!zoomed);
                }
            }

            @Override
            public void endMove(double startX, double startY, double offsetX, double offsetY) {
                if (!isInited) {
                    return;
                }
                isInited = false; // TODO after parking animation
            }

            @Override
            public boolean isSliding() {
                return false;
            }

            @Override
            public boolean isSlidingSupported() {
                return managerProvider.getFeatures().isUnlocked(Features.Feature.PLAY_FULLSCREEN);
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
