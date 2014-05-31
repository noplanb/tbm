package com.noplanbees.tbm;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.VideoView;

public class VideoPlayer implements OnCompletionListener{
	String TAG = this.getClass().getSimpleName();
	Activity activity;
	Context context;	
	String friendId;
	Friend friend;
	VideoView videoView;
	ImageView thumbView;
	VideoStatusHandler videoStatusHandler;
	
	private static ArrayList <VideoView> allVideoViews;
	
	public static void setAllVideoViews(ArrayList<VideoView> videoViews){
		Log.i("VideoPlayer", "suspending all video views");
		VideoPlayer.allVideoViews = videoViews;
		for (VideoView v : VideoPlayer.allVideoViews){
			v.suspend();
		}
	}
	
	public VideoPlayer(Activity a, String friendId) {
		activity = a;
		context = activity.getApplicationContext();
		videoStatusHandler = new VideoStatusHandler(context);
		this.friendId = friendId;
		friend = (Friend) FriendFactory.getFactoryInstance().find(friendId);
		videoView = friend.videoView(activity);
		videoView.setOnCompletionListener(this);
		thumbView = friend.thumbView(activity);
		showThumb();
	}


	public void click(){
		
		if (friend.videoFromFile().length() < 100)
			return;
		
		if (videoView.isPlaying()){
			stop();
		} else {
			start();
		}
	}
	
	public void start(){
		Log.i(TAG, "start");
		videoView.setVideoPath(friend.videoFromPath());
		hideThumb();
		videoView.start();
		videoStatusHandler.setVideoViewed(friend.getId());
	}
	
	public void stop(){
		Log.i(TAG, "stop");
		videoView.stopPlayback();
		showThumb();
//		if (videoView.isPlaying())
//			videoView.stopPlayback();
	}


	//------------------------
	// Thumb stuff
	//------------------------
	public void refreshThumb(){
		showThumb();
	}
	
	public void showThumb(){
		setThumbBorder();
		loadThumb();
		thumbView.invalidate();
		thumbView.setVisibility(View.VISIBLE);
		videoView.setVisibility(View.INVISIBLE);
	}

	public void hideThumb(){
		thumbView.setVisibility(View.INVISIBLE);
		videoView.setVisibility(View.VISIBLE);
	}

	private void loadThumb(){
		if (!friend.thumbExists() ){
			Log.i(TAG, "loadThumb: Loading icon for thumb for friend=" + friend.get("firstName"));
			(thumbView).setImageResource(R.drawable.head);
		}else{
			Log.i(TAG, "loadThumb: Loading bitmap for friend=" + friend.get("firstName"));
			thumbView.setImageBitmap(friend.thumbBitmap());
		}
	}

	private void setThumbBorder(){
		if (videoStatusHandler.videoNotViewed(friend)){
			Log.i(TAG, "setThumbBorder: setting thumb background to unviewed_shape");
			thumbView.setBackgroundResource(R.drawable.blue_border_shape);

		} else {
			Log.i(TAG, "setThumbBorder: setting thumb background to null");
			thumbView.setBackgroundResource(0);
		}
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		Log.i(TAG, "play complete.");
		stop();
	}
}
