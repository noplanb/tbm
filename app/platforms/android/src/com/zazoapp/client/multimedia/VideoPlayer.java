package com.zazoapp.client.multimedia;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.VideoView;

import com.zazoapp.client.R;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.Video;
import com.zazoapp.client.network.FileDownloadService;
import com.zazoapp.client.utilities.DialogShower;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class VideoPlayer implements OnCompletionListener, OnPreparedListener{

	private static final String TAG = VideoPlayer.class.getSimpleName();

	public interface StatusCallbacks{
		void onVideoPlaying(String friendId, String videoId);
		void onVideoStopPlaying(String friendId);
        void onVideoPlaybackError(String friendId, String videoId);
	}

	private static VideoPlayer videoPlayer;

	private Activity activity;
	private String videoId;
	private String friendId;
	private Friend friend;
	private VideoView videoView;
	private ViewGroup videoBody;
	private boolean videosAreDownloading;

	private Set<StatusCallbacks> statusCallbacks = new HashSet<StatusCallbacks>();

    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;

    private VideoPlayer() {
    }

    /**
     * You must call init method before using VideoPlayerInstance
     * @return instance of VideoPlayer
     * @see com.zazoapp.client.multimedia.VideoPlayer#init(android.app.Activity, android.view.ViewGroup, android.widget.VideoView)
     */
    public static VideoPlayer getInstance() {
        if (videoPlayer == null)
            videoPlayer = new VideoPlayer();
        return videoPlayer;
    }

    /**
     * Init VideoPlayer instance
     * @param activity reference activity
     * @param videoBody parent layout of VideoView
     * @param videoView VideoView
     */
    public void init(Activity activity, ViewGroup videoBody, VideoView videoView) {
        this.activity = activity;
        this.videoBody = videoBody;
        this.videoView = videoView;
        this.videoView.setOnCompletionListener(this);
        this.videoView.setOnPreparedListener(this);

        audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);

        // TODO: GARF: Andrey why are these not used?
        audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            public void onAudioFocusChange(int focusChange) {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    // Pause playback
                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    // Resume playback
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                    //audioManager.unregisterMediaButtonEventReceiver(RemoteControlReceiver);
                    audioManager.abandonAudioFocus(audioFocusChangeListener);
                    // Stop playback
                }
            }
        };
    }

	public void registerStatusCallbacks(StatusCallbacks statusCallback){
		this.statusCallbacks.add(statusCallback);
	}

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

    //---------------
    // Public actions
    //---------------
	public void togglePlayOverView(View view, String friendId){
		boolean needToPlay = !(isPlaying() && friendId.equals(this.friendId));

	    // Always stop first so that the notification goes out to reset the view we were on in case it was still playing and we are switching to another view.
		stop();

		this.friendId = friendId;
		friend = (Friend) FriendFactory.getFactoryInstance().find(friendId);

		if(needToPlay) {
			setPlayerOverView(view);
			start();
		}
	}

    public void stop(){
        Log.i(TAG, "stop");
        audioManager.abandonAudioFocus(audioFocusChangeListener);
        videoView.stopPlayback();
        videoView.setVideoURI(null);
        videoView.suspend();
        videoBody.setVisibility(View.GONE);
        notifyStopPlaying();
    }

	public void release(){
	    stop();
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
		            FileDownloadService.restartTransfersPendingRetry(activity);
		            DialogShower.showBadConnection(activity);
		        } else {
		            DialogShower.showToast(activity, activity.getString(R.string.toast_downloading));
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
        // Always set it to viewed whether it is playable or not so it eventually gets deleted.
        friend.setAndNotifyIncomingVideoStatus(videoId, Video.IncomingVideoStatus.VIEWED);

        if (videoIsPlayable()){
            // TODO: GARF: Andrey what happens if it is not granted!
            if (requestAudioFocus() == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                videoBody.setVisibility(View.VISIBLE);
                final String path = friend.videoFromPath(videoId);
                videoView.setOnPreparedListener(new OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        Log.i(TAG, "video duration " + videoView.getDuration() + " " + path);
                        videoView.start();
                        notifyStartPlaying();
                    }
                });
                videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        final String brokenVideoId = videoId;
                        mp.reset();
                        onCompletion(mp);
                        friend.setAndNotifyIncomingVideoStatus(brokenVideoId, Video.IncomingVideoStatus.FAILED_PERMANENTLY);
                        notifyPlaybackError();
                        return true;
                    }
                });
                videoView.setVideoPath(path);
            }

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
        else
            stop();
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

    private boolean isPlaying(){
        return videoView.isPlaying();
    }

    private boolean videoIsPlayable(){
        File f = friend.videoFromFile(videoId);
        return f.exists() && f.length() > 100;
    }

    private int requestAudioFocus() {
        return audioManager.requestAudioFocus(audioFocusChangeListener,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
    }



}
