package com.noplanbees.tbm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.TextView;

public class IntentHandler {
	private final String TAG = this.getClass().getSimpleName();

	public static final int TYPE_VIDEO_RECEIVED = 0;
	public static final int TYPE_VIDEO_STATUS_UPDATE = 1;

	public static final int STATE_SHUTDOWN = 0;
	public static final int STATE_FOREGROUND = 1;
	public static final int STATE_BACKGROUND = 2;

	public static final String INTENT_TYPE_KEY = "type";

	public static final int RESULT_FINISH = 0;
	public static final int RESULT_CONTINUE = 1;



	private HomeActivity homeActivity;
	private Intent intent;
	private Bundle extras;
	private Friend friend;

	public IntentHandler(HomeActivity a, Intent i){
		homeActivity = a;
		intent = i;
		extras = intent.getExtras();
		friend = FriendFactory.getFactoryInstance().getFriendFromIntent(intent);
	}

	public Integer handle(int state){
		Log.i(TAG, "handle: state = " + state);
		Integer r = null;
		if (typeIsVideoReceived()){
			r = handleVideoReceived(state);
		} else if (typeIsVideoStatusUpdate()){
			r = handleVideoStatusUpdate(state);
		} else {
			Log.i(TAG, "handle: no intent type ");
		}
		return r;
	}

	//-----------------------
	// VideoStausUpdate stuff
	//-----------------------
	private int handleVideoStatusUpdate(int state) {
		Log.i(TAG, "handleVideoStatusUpdate");
		if (state == STATE_FOREGROUND){
			updateHomeViewSentVideoStatus(friend);
			return RESULT_CONTINUE;
		} 
		return RESULT_FINISH;
	}

	public void updateHomeViewSentVideoStatus(Friend friend) {
		if (friend != null){
			Log.i(TAG, "updateHomeViewSentVideoStatus: friend = " + friend.get("firstName"));
			Integer nameTextId = Integer.parseInt(friend.get("nameTextId"));
			TextView nameText = (TextView) homeActivity.findViewById(nameTextId);
			nameText.setText(new VideoStatusHandler(homeActivity).getStatusStr(friend));
			nameText.invalidate();
		} else {
			Log.e(TAG, "updateHomeViewSentVideoStatus: Error friend was null");
		}
	}

	//-----------------------
	// VideoRecievedStuff stuff
	//-----------------------
	private Integer handleVideoReceived(int state) {
		Log.i(TAG, "handleVideoReceived");
		Integer r = null;
		if (state == STATE_SHUTDOWN){
			new SendNotificationAsync().execute();
			r = RESULT_FINISH;
		} else {
			updateHomeViewReceivedVideo();
			r = RESULT_CONTINUE;
		}
		return r;
	}

	private void updateHomeViewReceivedVideo() {
		Log.i(TAG, "updateHomeViewReceivedVideo");
		if (friend != null){
			homeActivity.getVideoPlayerForFriend(friend).refreshThumb();
			playNotificationTone();
			updateHomeViewSentVideoStatus(friend);
		}	
	}

	// Using async to prevent the screen from flashing by minimizing the delay to finish onCreate
	class SendNotificationAsync extends AsyncTask<Void, Void, Void>{
		@Override
		protected Void doInBackground(Void... params) {
			sendNotification();
			return null;
		}
	}

	private void sendNotification() {
		Log.i(TAG, "sendNotification");
		final int NOTIFICATION_ID = 1;
		NotificationManager notificationManager = (NotificationManager) homeActivity.getSystemService(Context.NOTIFICATION_SERVICE);

		PendingIntent contentIntent = PendingIntent.getActivity(homeActivity, 0, new Intent(homeActivity, HomeActivity.class), 0);

		String msg = "Message from " + friend.get("firstName") + "!";

		Bitmap sqThumbBmp = friend.sqThumbBitmap();

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(homeActivity)
		.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
		.setLargeIcon(sqThumbBmp)
		.setSmallIcon(R.drawable.ic_stat_gcm)
		.setContentTitle(msg)
		.setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
		.setContentText("Three By Me");

		Log.i(TAG, "sendNotification: Sending notification");
		mBuilder.setContentIntent(contentIntent);
		notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}


	private boolean typeIsVideoStatusUpdate() {
		return  type() != null && type() == TYPE_VIDEO_STATUS_UPDATE;
	}

	private Integer type(){
		Integer t = null;
		if (extras != null){
			t = extras.getInt(INTENT_TYPE_KEY);
		}
		return t; 
	}

	private boolean typeIsVideoReceived() {
		return type() != null && type() == TYPE_VIDEO_RECEIVED;
	}

	private void playNotificationTone(){
		Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		Ringtone r = RingtoneManager.getRingtone(homeActivity.getApplicationContext(), notification);
		r.play();
	}
}
