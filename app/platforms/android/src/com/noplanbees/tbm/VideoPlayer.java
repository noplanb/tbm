package com.noplanbees.tbm;
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
	String TAG = this.getClass().getSimpleName();
	
	private Context context;
	private String videoId;
	private String friendId;
	private VideoView videoView;
	private View videoBody;
	
	public static void bypassExistingBluetooth(Context context){
		((AudioManager) context.getSystemService(Activity.AUDIO_SERVICE)).setBluetoothScoOn(true);
	}
	
	public static void restoreExistingBluetooth(Context context){
		((AudioManager) context.getSystemService(Activity.AUDIO_SERVICE)).setBluetoothScoOn(false);
	}
	
	public Boolean isPlaying(){
		return videoView.isPlaying();
	}
	
	public VideoPlayer(Context context) {
		this.context = context;
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
		//videoBody.setVisibility(View.VISIBLE);
	}

	public void play(String friendId){
		
		if (videoView.isPlaying()){
			stop();
		} else {
			start(friendId);
		}
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
			videoBody.setVisibility(View.VISIBLE);
			videoView.setVideoPath(f.videoFromPath(videoId));
			AudioManager am = (AudioManager) this.context.getSystemService(Activity.AUDIO_SERVICE);
			am.setBluetoothScoOn(true);
			videoView.start();		
		} else {
			onCompletion(null);
		}
	}
	
	public void stop(){
		Log.i(TAG, "stop");
		VideoPlayer.restoreExistingBluetooth(context);
		videoView.stopPlayback();
		videoView.suspend();
		videoBody.setVisibility(View.INVISIBLE);
	}

	public void release(Context context){
		restoreExistingBluetooth(context);
		stop();
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		Log.i(TAG, "play complete.");
		Friend friend = (Friend) FriendFactory.instance.find(friendId);
		friend.setAndNotifyIncomingVideoViewed(videoId);
		videoId = friend.nextPlayableVideoId(videoId);
		if (videoId != null){
			play(friend);
		} else {
			stop();
		}
	}


}
