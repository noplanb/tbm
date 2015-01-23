package com.noplanbees.tbm.multimedia;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.VideoView;

import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.FriendFactory;

import java.util.HashSet;
import java.util.Set;


public class VideoPlayer implements OnCompletionListener{
	
	private String TAG = this.getClass().getSimpleName();

	public interface StatusCallbacks{
		void onVideoPlaying(String friendId, String videoId);
		void onVideoStopPlaying();
	}
	
	private static VideoPlayer videoPlayer;
	
	private Context context;
	private String videoId;
	private String friendId;
	private VideoView videoView;
	private View videoBody;
	
	private Set<StatusCallbacks> statusCallbacks;

    private AudioManager am;
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;

    private VideoPlayer(Context context) {
		this.context = context;
		statusCallbacks = new HashSet<StatusCallbacks>();

        am = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            public void onAudioFocusChange(int focusChange) {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    // Pause playback
                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    // Resume playback
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                    //am.unregisterMediaButtonEventReceiver(RemoteControlReceiver);
                    am.abandonAudioFocus(audioFocusChangeListener);
                    // Stop playback
                }
            }
        };


    }
	
	public static VideoPlayer getInstance(Context context){
		if(videoPlayer == null)
			videoPlayer = new VideoPlayer(context);
		return videoPlayer;
	}
	
	public void registerStatusCallbacks(StatusCallbacks statusCallback){
		this.statusCallbacks.add(statusCallback);
	}
	
	public void unregisterStatusCallbacks(StatusCallbacks statusCallback){
		this.statusCallbacks.remove(statusCallback);
	}
	
	private void notifyStartPlaying(){
		for (StatusCallbacks callbacks : statusCallbacks) {
			callbacks.onVideoPlaying(friendId, videoId);
		}
	}
	
	private void notifyStopPlaying(){
		for (StatusCallbacks callbacks : statusCallbacks) {
			callbacks.onVideoStopPlaying();
		}
	}

	public static void bypassExistingBluetooth(Context context){
		//((AudioManager) context.getSystemService(Activity.AUDIO_SERVICE)).setBluetoothScoOn(true);
	}
	
	public static void restoreExistingBluetooth(Context context){
		//((AudioManager) context.getSystemService(Activity.AUDIO_SERVICE)).setBluetoothScoOn(false);
	}
	
	public boolean isPlaying(){
		return videoView.isPlaying();
	}
	
	public void setVideoView(VideoView videoView){
		this.videoView = videoView;
		this.videoView.setOnCompletionListener(this);
	}
	
	public void setVideoViewBody(View videoBody){
		this.videoBody = videoBody;
	}
	
	public void setVideoViewSize(float x, float y, int width, int height) {
		LayoutParams params = new FrameLayout.LayoutParams(width, height);
		videoBody.setLayoutParams(params);
		videoBody.setX(x);
		videoBody.setY(y);
	}

	public String getFriendId() {
		return friendId;
	}

	public void playAtPos(float x, float y, int width, int height, String friendId){
		
		boolean needToPlay = !(videoView.isPlaying() && friendId.equals(this.friendId));
		
		if (videoView.isPlaying())
			stop();
		
		if(needToPlay) {
			setVideoViewSize(x, y, width, height);
			start(friendId);
		}
	}
	
	public void play(String friendId){
		
		boolean needToPlay = !(videoView.isPlaying() && friendId.equals(this.friendId));
		
		if (videoView.isPlaying())
			stop();
		
		if(needToPlay) {
			start(friendId);
		}
	}
	
	public void stop(){
		Log.i(TAG, "stop");
		VideoPlayer.restoreExistingBluetooth(context);
		videoView.stopPlayback();
		videoView.suspend();
		videoBody.setVisibility(View.GONE);
	}

	private void start(String friendId){
		Log.i(TAG, "start");
		this.friendId = friendId;
		
	    Friend friend = (Friend) FriendFactory.getFactoryInstance().find(friendId);
		videoId = friend.firstPlayableVideoId();
		
		if (videoId == null)
			return;
		
		play(friend);
	}

	private void play(Friend f){
		if (f.videoFromFile(videoId).length() > 100){
			f.setAndNotifyIncomingVideoViewed(videoId);
			videoBody.setVisibility(View.VISIBLE);
			videoView.setVideoPath(f.videoFromPath(videoId));
			//AudioManager am = (AudioManager) this.context.getSystemService(Activity.AUDIO_SERVICE);
			//am.setBluetoothScoOn(true);

            // Request audio focus for playback

            int result = am.requestAudioFocus(audioFocusChangeListener,
                    // Use the music stream.
                    AudioManager.STREAM_MUSIC,
                    // Request permanent focus.
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                videoView.start();
                notifyStartPlaying();
            }

		} else {
			onCompletion(null);
		}
	}
	
	public void release(Context context){
		restoreExistingBluetooth(context);
		stop();
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		Log.i(TAG, "play complete.");
        am.abandonAudioFocus(audioFocusChangeListener);
		notifyStopPlaying();
		Friend friend = (Friend) FriendFactory.getFactoryInstance().find(friendId);
		videoId = friend.nextPlayableVideoId(videoId);
		if (videoId != null){
			play(friend);
		} else {
			stop();
		}
	}


}
