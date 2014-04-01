package com.noplanbees.tbm;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.VideoView;

public class VideoPlayer {
	String TAG = this.getClass().getSimpleName();
	Activity activity;
	Context context;	
	String friendId;
	VideoStatusHandler videoStatusHandler;

	public VideoPlayer(Activity a, String friendId) {
		activity = a;
		context = activity.getApplicationContext();
		videoStatusHandler = new VideoStatusHandler(context);
		this.friendId = friendId;
		showThumb();
	}

	private Friend getFriend() {
		return (Friend) FriendFactory.getFactoryInstance().find(friendId);
	}
	
	private VideoView getVideoView(){
		return getFriend().videoView(activity);
	}
	
	private ImageView getThumbView(){
		return getFriend().thumbView(activity);
	}

	public void click(){
		Friend friend = getFriend();
		VideoView videoView = getVideoView();
		
		if (friend.videoFromFile().length() < 100)
			return;
		
		if (videoView.isPlaying()){
			videoView.pause();
		} else {
			videoView.setVideoPath(friend.videoFromPath());
			hideThumb();
			videoView.start();
			videoStatusHandler.setVideoViewed(friend);
		}
	}
	
	public void stop(){
		VideoView videoView = getVideoView();
		if (videoView.isPlaying())
			videoView.stopPlayback();
	}


	//------------------------
	// Thumb stuff
	//------------------------
	public void refreshThumb(){
		showThumb();
	}
	
	public void showThumb(){
		ImageView thumbView = getThumbView();
		VideoView videoView = getVideoView();
		setThumbBorder();
		loadThumb();
		thumbView.invalidate();
		thumbView.setVisibility(View.VISIBLE);
		videoView.setVisibility(View.INVISIBLE);
	}

	public void hideThumb(){
		ImageView thumbView = getThumbView();
		VideoView videoView = getVideoView();
		thumbView.setVisibility(View.INVISIBLE);
		videoView.setVisibility(View.VISIBLE);
	}

	private void loadThumb(){
		ImageView thumbView = getThumbView();
		Friend friend = getFriend();
		if (!friend.thumbExists() ){
			Log.i(TAG, "loadThumb: Loading icon for thumb for friend=" + friend.get("firstName"));
			(thumbView).setImageResource(R.drawable.head);
		}else{
			Log.i(TAG, "loadThumb: Loading bitmap for friend=" + friend.get("firstName"));
			thumbView.setImageBitmap(friend.thumbBitmap());
		}
	}

	private void setThumbBorder(){
		ImageView thumbView = getThumbView();
		Friend friend = getFriend();
		if (videoStatusHandler.videoNotViewed(friend)){
			Log.i(TAG, "setThumbBorder: setting thumb background to unviewed_shape");
			thumbView.setBackgroundResource(R.drawable.blue_border_shape);

		} else {
			Log.i(TAG, "setThumbBorder: setting thumb background to null");
			thumbView.setBackgroundResource(0);
		}
	}
}
