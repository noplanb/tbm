package com.noplanbees.tbm.multimedia;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.VideoView;

import com.noplanbees.tbm.R;
import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.FriendFactory;
import com.noplanbees.tbm.model.Video;
import com.noplanbees.tbm.network.FileDownloadService;
import com.noplanbees.tbm.utilities.DialogShower;


public class VideoPlayer implements OnCompletionListener{
	
	private String TAG = this.getClass().getSimpleName();

	public interface StatusCallbacks{
		void onVideoPlaying(String friendId, String videoId);
		void onVideoStopPlaying(String friendId);
	}
	
	private static VideoPlayer videoPlayer;
	
	private Activity activity;
	private String videoId;
	private Video video;
	private String friendId;
	private Friend friend;
	private VideoView videoView;
	private View videoBody;
	private boolean videosAreDownloading;
	
	private Set<StatusCallbacks> statusCallbacks;

    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;

    //---------------------
    // Instantiate and init
    //---------------------
    private VideoPlayer(Activity a) {
		activity = a;
		statusCallbacks = new HashSet<StatusCallbacks>();

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
    
    // TODO: Serhii reference should be held by the instantiating fragment
    public static VideoPlayer getInstance(Activity activity){
        if(videoPlayer == null)
            videoPlayer = new VideoPlayer(activity);
        return videoPlayer;
    }
    
    
    // TODO: Serhii we should probably pass the view and the body in in the constructor no?
    public void setVideoView(VideoView videoView){
        this.videoView = videoView;
        this.videoView.setOnCompletionListener(this);
    }
    
    // TODO: Serhii we should probably pass the view and the body in in the constructor no?
    public void setVideoViewBody(View videoBody){
        this.videoBody = videoBody;
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
	

    //---------------
    // Public actions
    //---------------
	public void togglePlayOverView(View view, String friendId){
		boolean needToPlay = !(isPlaying() && friendId.equals(this.friendId));
		
	    // Always stop first so that the notification goes out to reset the view we were on in case it was still playing and we are switching to another view.
		stop();
		
		this.friendId = friendId;
		friend = (Friend) FriendFactory.getFactoryInstance().find(friendId);
		
		if (isPlaying())
			stop();
		
		if(needToPlay) {
			setPlayerOverView(view);
			start();
		}
	}
	
    public void stop(){
        Log.i(TAG, "stop");
        audioManager.abandonAudioFocus(audioFocusChangeListener);
        videoView.stopPlayback();
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
        friend.setAndNotifyIncomingVideoViewed(videoId);

        if (videoIsPlayable()){
			videoBody.setVisibility(View.VISIBLE);
			videoView.setVideoPath(friend.videoFromPath(videoId));

            // TODO: GARF: Andrey what happens if it is not granted!
            if (requestAudioFocus() == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                videoView.start();
                notifyStartPlaying();
            }

		} else {
			onCompletion(null);
		}
	}
    
    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.i(TAG, "play complete.");
        // TODO check notification, if it is the last video it will notify two times (also from stop()) --Serhii
        // This should be fixed -- Sani
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
	
	private void determineIfDownloading(){
	    if (friend.hasDownloadingVideo())
	        videosAreDownloading = true;
	    else
	        videosAreDownloading = false;	    
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
