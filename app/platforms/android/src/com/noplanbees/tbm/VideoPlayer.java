package com.noplanbees.tbm;
import android.app.Activity;
import android.widget.VideoView;

public class VideoPlayer {
	String TAG = this.getClass().getSimpleName();
	Activity activity;

//	videoPlayers.get(v.getId()).setVideoSourcePath(videoRecorder.getRecordedFilePath(f.get("id")));

	VideoView videoView;
	Friend friend;

	public VideoPlayer(Activity a, VideoView vv) {
		activity = a;
		videoView = vv;
		friend = FriendFactory.getFriendFromVew(videoView);
		videoView.requestFocus();
	}
	
	
	public void click(){
		if (videoView.isPlaying()){
			videoView.pause();
		} else {
			videoView.setVideoPath(Config.videoPathForFriend(friend));
			videoView.start();
		}
	}
	
	public void start(){
		videoView.start();
	}
	
	public void pause(){
		videoView.pause();
	}
	
	public void stop(){
		videoView.stopPlayback();
	}
	
	public void release(){
//		videoView.r
	}
}
