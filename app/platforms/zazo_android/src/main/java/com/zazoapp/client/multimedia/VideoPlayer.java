package com.zazoapp.client.multimedia;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.zazoapp.client.R;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.IncomingVideo;
import com.zazoapp.client.network.FileDownloadService;
import com.zazoapp.client.network.FileTransferService;
import com.zazoapp.client.ui.ViewGroupGestureRecognizer;
import com.zazoapp.client.ui.ZazoManagerProvider;
import com.zazoapp.client.ui.view.GestureControlledLayout;
import com.zazoapp.client.ui.view.TouchBlockScreen;
import com.zazoapp.client.ui.view.VideoProgressBar;
import com.zazoapp.client.ui.view.VideoView;
import com.zazoapp.client.utilities.DialogShower;

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
    private TouchBlockScreen blockScreen;
    private VideoProgressBar progressBar;
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
        LayoutParams params = new FrameLayout.LayoutParams(view.getWidth(), view.getHeight());
        videoBody.setLayoutParams(params);
        videoBody.setX(view.getX());
        videoBody.setY(view.getY());
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
    }

    public boolean isPlaying(){
        return videoView != null && videoView.isPlaying();
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
            zoomRatio = 0f;
            zoomed = false;
        }

        private class ZoomGestureRecognizer extends ViewGroupGestureRecognizer.Stub {
            public ZoomGestureRecognizer(Activity a, ViewGroup vg, ArrayList<View> tvs) {
                super(a, tvs);
            }

            @Override
            public boolean click(View v) {
                if (videoRootLayout.equals(v)) {
                    if (isPlaying()) {
                        zoomOut();
                        stop();
                        return true;
                    }
                }
                if (videoBody.equals(v)) {
                    if (videoView.isPlaying()) {
                        videoView.pause();
                    } else {
                        videoView.start();
                    }
                }
                return false;
            }

            @Override
            public boolean abort(View v, int reason) {
                return false;
            }

            @Override
            public void notifyMove(View target, double startX, double startY, double offsetX, double offsetY) {

            }

            @Override
            public void startMove(View target, double startX, double startY, double offsetX, double offsetY) {
                if (videoBody.equals(target) && !(zoomAnimator != null && zoomAnimator.isStarted())) {
                    if (zoomed) {
                        zoomOut();
                    } else {
                        zoomIn();
                    }
                }
            }

            @Override
            public void endMove(double startX, double startY, double offsetX, double offsetY) {

            }

            @Override
            public boolean isSliding() {
                return false;
            }

            @Override
            public boolean isSlidingSupported() {
                return true;
            }
        }

        ;

        private boolean handleZoom(MotionEvent event) {
            int action = event.getAction();

            if (zoomed) {
                zoomOut();
            } else {
                if (zoomRollback == null) {
                    zoomRollback = new Runnable() {
                        float x = videoBody.getTranslationX();
                        float y = videoBody.getTranslationY();
                        int w = videoBody.getWidth();
                        int h = videoBody.getHeight();

                        @Override
                        public void run() {
                            videoBody.setTranslationX(x);
                            videoBody.setTranslationY(y);
                            videoView.setCropFraction(1);
                            ViewGroup.LayoutParams p = videoBody.getLayoutParams();
                            p.width = w;
                            p.height = h;
                            videoBody.setLayoutParams(p);
                        }
                    };
                }
            }
            return true;
        }

        private void zoomIn() {
            initialWidth = videoBody.getWidth();
            initialHeight = videoBody.getHeight();
            initialX = videoBody.getTranslationX();
            initialY = videoBody.getTranslationY();
            zoomAnimator = ValueAnimator.ofFloat(zoomRatio, 1);
            zoomAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                int w = initialWidth;
                int h = initialHeight;
                float trX = initialX;
                float trY = initialY;

                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float value = (float) animation.getAnimatedValue();
                    videoView.setCropFraction(1 - value);
                    videoBody.setTranslationX(trX * (1 - value));
                    videoBody.setTranslationY(trY * (1 - value));
                    ViewGroup.LayoutParams p = videoBody.getLayoutParams();
                    p.width = (int) (w + (videoRootLayout.getWidth() - w) * value);
                    p.height = (int) (h + (videoRootLayout.getHeight() - h) * value);
                    Log.i(TAG, "" + w + " " + h + " " + p.width + " " + p.height);
                    videoBody.setLayoutParams(p);
                    zoomRatio = value;
                    Log.i(TAG, String.valueOf(zoomRatio));
                }
            });
            zoomAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    zoomed = true;
                }
            });
            zoomAnimator.setDuration(1000);
            zoomAnimator.start();
        }

        private void zoomOut() {
            zoomAnimator = ValueAnimator.ofFloat(zoomRatio, 0);
            zoomAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                int w = initialWidth;
                int h = initialHeight;
                float trX = initialX;
                float trY = initialY;

                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float value = (float) animation.getAnimatedValue();
                    videoView.setCropFraction(1 - value);
                    videoBody.setTranslationX(trX * (1 - value));
                    videoBody.setTranslationY(trY * (1 - value));
                    ViewGroup.LayoutParams p = videoBody.getLayoutParams();
                    p.width = (int) (w + (videoRootLayout.getWidth() - w) * value);
                    p.height = (int) (h + (videoRootLayout.getHeight() - h) * value);
                    videoBody.setLayoutParams(p);
                    zoomRatio = value;
                    Log.i(TAG, String.valueOf(zoomRatio));
                }
            });
            zoomAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    zoomed = false;
                }
            });
            zoomAnimator.setDuration(1000);
            zoomAnimator.start();
        }
    }
}
