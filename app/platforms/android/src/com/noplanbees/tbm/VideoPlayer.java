package com.noplanbees.tbm;
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
	
	private VideoPlayer(Context context) {
		this.context = context;
		statusCallbacks = new HashSet<StatusCallbacks>();
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
	
	public void play(String friendId){
		
		if (videoView.isPlaying() && friendId.equals(this.friendId)){
			stop();
		} else {
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
		
	    Friend friend = (Friend) FriendFactory.instance.find(friendId);
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
			videoView.start();
			notifyStartPlaying();
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
		notifyStopPlaying();
		Friend friend = (Friend) FriendFactory.instance.find(friendId);
		videoId = friend.nextPlayableVideoId(videoId);
		if (videoId != null){
			play(friend);
		} else {
			stop();
		}
	}


}
