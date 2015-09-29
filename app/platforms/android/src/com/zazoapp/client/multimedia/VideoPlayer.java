package com.zazoapp.client.multimedia;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import com.zazoapp.client.R;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.IncomingVideo;
import com.zazoapp.client.network.FileDownloadService;
import com.zazoapp.client.network.FileTransferService;
import com.zazoapp.client.ui.ZazoManagerProvider;
import com.zazoapp.client.ui.view.VideoView;
import com.zazoapp.client.utilities.DialogShower;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class VideoPlayer implements OnCompletionListener, OnPreparedListener, Player {

	private static final String TAG = VideoPlayer.class.getSimpleName();

    private Activity activity;
	private String videoId;
	private String friendId;
	private Friend friend;
	private VideoView videoView;
	private ViewGroup videoBody;
	private boolean videosAreDownloading;
    private ZazoManagerProvider managerProvider;
    private Timer timer = new Timer();
    private TimerTask onStartTask;

	private Set<StatusCallbacks> statusCallbacks = new HashSet<StatusCallbacks>();

    public VideoPlayer(Activity activity, ZazoManagerProvider managerProvider) {
        this.activity = activity;
        this.managerProvider = managerProvider;
    }

    @Override
    public void init(ViewGroup videoBody, final VideoView videoView) {
        this.videoBody = videoBody;
        this.videoView = videoView;
        this.videoView.setOnCompletionListener(this);
        this.videoView.setOnPreparedListener(this);
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
        videoBody.setVisibility(View.INVISIBLE);
        //need to clear videoView because of last frame of already viewed video appear before new one start playing
        //TODO need to fix delay with black frame (or first video frame)
//        videoView.setVisibility(View.INVISIBLE);
//        videoView.setVisibility(View.VISIBLE);
        videoView.stopPlayback();
        videoView.setVideoURI(null);
        videoView.suspend();
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

	private void setCurrentVideoToFirst(){
	    if (videosAreDownloading)
	        setCurrentVideoId(friend.getFirstUnviewedVideoId());
	    else
	        setCurrentVideoId(friend.getFirstPlayableVideoId());
	}

	private void setCurrentVideoToNext(){
	    if (videosAreDownloading)
	        setCurrentVideoId(friend.getNextUnviewedVideoId(videoId));
	    else
	        setCurrentVideoId(friend.getNextPlayableVideoId(videoId));
	}

	private void setCurrentVideoId(String videoId){
	    this.videoId = videoId;
	}

    public boolean isPlaying(){
        return videoView.isPlaying();
    }

    @Override
    public void rewind(int msec) {
        if (videoView.isPlaying()) {
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
                                        videoView.setVisibility(View.VISIBLE);
                                        videoBody.setVisibility(View.VISIBLE);
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

}
