package com.noplanbees.tbm;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.VideoView;

public class VideoPlayer implements OnCompletionListener{
	String TAG = this.getClass().getSimpleName();
	
	GridElement gridElement;
	Activity activity;
	VideoView videoView;
	ImageView thumbView;
	String videoId;
	
	private static ArrayList <VideoView> allVideoViews = new ArrayList <VideoView>();
	private static ArrayList <VideoPlayer> allVideoPlayers = new ArrayList <VideoPlayer>();
	
	public static void stopAll(){
		VideoPlayer.showAllThumbs();
		VideoPlayer.suspendAll();
	}
	
	public static void release(Context context){
		restoreExistingBluetooth(context);
		stopAll();
	}
	
	public static void bypassExistingBluetooth(Context context){
		((AudioManager) context.getSystemService(Activity.AUDIO_SERVICE)).setBluetoothScoOn(true);
	}
	
	public static void restoreExistingBluetooth(Context context){
		((AudioManager) context.getSystemService(Activity.AUDIO_SERVICE)).setBluetoothScoOn(false);
	}
	
	private static void suspendAll(){
		Log.i("VideoPlayer", "suspending all video views");
		for (VideoView v : VideoPlayer.allVideoViews){
			Log.i("VideoPlayer", String.format("stoping and suspending %s", v.toString()));
			v.stopPlayback();
			v.suspend();
		}
	}
	
	public static void showAllThumbs(){
		for (VideoPlayer vp : VideoPlayer.allVideoPlayers){
			if (vp.gridElement.hasFriend())
				vp.showThumb();
		}
	}
	
	public static Boolean isPlaying(String friendId){
		Boolean r = false;
		GridElement ge = GridElementFactory.instance.findWithFriendId(friendId);
		if (ge != null)
			r = ge.videoView.isPlaying();
		return r;
	}
	
	public static void refreshThumbWithFriendId(String friendId) {
		GridElement ge = GridElementFactory.instance.findWithFriendId(friendId);
		if (ge != null)
			ge.videoPlayer.refreshThumb();
	}
	
	public VideoPlayer(GridElement ge) {
		gridElement = ge;
		activity = gridElement.activity;
		videoView = gridElement.videoView;
		videoView.setOnCompletionListener(this);
		
		if (!VideoPlayer.allVideoViews.contains(videoView))
			VideoPlayer.allVideoViews.add(videoView);
		
		VideoPlayer.allVideoPlayers.add(this);
		
		thumbView = gridElement.thumbView;
		showThumb();
	}
	
	private Friend friend(){
		return gridElement.friend();
	}
	
	public void click(){
		
		if (videoView.isPlaying()){
			stop();
		} else {
			start();
		}
	}
	
	public void start(){
		Log.i(TAG, "start");
		VideoPlayer.stopAll();
	    videoId = friend().firstPlayableVideoId();
		
		if (videoId == null)
			return;
		
		play();
	}
	
	public void play(){
		if (friend().videoFromFile(videoId).length() > 100){
			videoView.setVideoPath(friend().videoFromPath(videoId));
			hideThumb();
			AudioManager am = (AudioManager) activity.getSystemService(Activity.AUDIO_SERVICE);
			am.setBluetoothScoOn(true);
			videoView.start();		
		} else {
			onCompletion(null);
		}
	}
	
	public void stop(){
		Log.i(TAG, "stop");
		VideoPlayer.restoreExistingBluetooth(activity);
		VideoPlayer.stopAll();
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
		
		if (!videoView.isPlaying()){
			thumbView.invalidate();
			thumbView.setVisibility(View.VISIBLE);
			videoView.setVisibility(View.INVISIBLE);
		}
	}
	
	public void hideThumb(){
		thumbView.setVisibility(View.INVISIBLE);
		videoView.setVisibility(View.VISIBLE);
	}

	private void loadThumb(){
		if (!gridElement.hasFriend())
			return;
		
		if (!friend().thumbExists() ){
			Log.i(TAG, "loadThumb: Loading icon for thumb for friend=" + friend().get(Friend.Attributes.FIRST_NAME));
			thumbView.setImageResource(R.drawable.head);
		}else{
			Log.i(TAG, "loadThumb: Loading bitmap for friend=" + friend().get(Friend.Attributes.FIRST_NAME));
			thumbView.setImageBitmap(friend().lastThumbBitmap());
		}
	}

	private void setThumbBorder(){
		if (!gridElement.hasFriend())
			return;
		
		if (friend().incomingVideoNotViewed()){
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
		friend().setAndNotifyIncomingVideoViewed(videoId);
		videoId = friend().nextPlayableVideoId(videoId);
		if (videoId != null){
			play();
		} else {
			stop();
		}
	}


}
