package com.noplanbees.tbm;

import java.net.URI;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class NotificationAlertManager {
	
	private final String TAG = this.getClass().getSimpleName();
	private final static String STAG = NotificationAlertManager.class.getSimpleName();
	
	// ----------------------------
	// Attributes for notifications
	// ----------------------------
	public static final String TITLE_KEY = "titleKey";
	public static final String SUB_TITLE_KEY = "subTitleKey";
	public static final String LARGE_IMAGE_PATH_KEY = "largeImagePathKey";
	public static final String SMALL_ICON_KEY = "smallIconKey";
	
	private static String subTitle = "Three By Me";
	private static int smallIcon = R.drawable.ic_stat_gcm;
	private static Uri alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

	private static Bitmap largeImage(Friend friend, String videoId){
		return friend.sqThumbBitmap(videoId);
	}

	private static String largeImagePath(Friend friend, String videoId){
		return friend.thumbPath(videoId);
	}

	private static String title(Friend friend){
		return "From " + friend.get(Friend.Attributes.FIRST_NAME) + "!";
	}

	// -------------------
	// Notification Alerts
	// -------------------
	
	// Public methods
	public static void alert(HomeActivity homeActivity, Friend friend, String videoId){
		Log.i(STAG, "alert");
		
		if ( screenIsLocked(homeActivity) || screenIsOff(homeActivity))
			postLockScreenAlert(homeActivity, friend, videoId);
		
		postNativeAlert(homeActivity, friend, videoId);
	}
	
	public static void cancelNativeAlerts(HomeActivity homeActivity){
		Log.i(STAG, "cancelNativeAlerts");
		NotificationManager nm = (NotificationManager) homeActivity.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancelAll();
	}
	
	// Private 
	private static void postNativeAlert(HomeActivity homeActivity, Friend friend, String videoId) {
		Log.i(STAG, "postNativeAlert");
		final int NOTIFICATION_ID = 1;
		NotificationManager notificationManager = (NotificationManager) homeActivity.getSystemService(Context.NOTIFICATION_SERVICE);
		Intent intent = new Intent(homeActivity.getApplicationContext(), homeActivity.getClass());
		PendingIntent contentIntent = PendingIntent.getActivity(homeActivity, 0, makePlayVideoIntent(intent, homeActivity, friend), 0);		

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(homeActivity)
		.setSound(alertSound)
		.setLargeIcon(largeImage(friend, videoId))
		.setSmallIcon(smallIcon)
		.setContentTitle(title(friend))
		.setStyle(new NotificationCompat.BigTextStyle().bigText(title(friend)))
		.setContentText(subTitle)
		.setContentIntent(contentIntent);
				
		notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}

	private static void postLockScreenAlert(HomeActivity homeActivity, Friend friend, String videoId) {
		Log.i(STAG, "postLockScreenAlert");
		Intent ri = new Intent(homeActivity, LockScreenAlertActivity.class);
		Intent i = makePlayVideoIntent(ri, homeActivity, friend);
		i.putExtra(IntentHandler.IntentParamKeys.FRIEND_ID, friend.getId());
		i.putExtra(LARGE_IMAGE_PATH_KEY, largeImagePath(friend, videoId));
		i.putExtra(SMALL_ICON_KEY, smallIcon);
		i.putExtra(TITLE_KEY, title(friend));
		i.putExtra(SUB_TITLE_KEY, subTitle);
		i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		i.addFlags(Intent.FLAG_FROM_BACKGROUND);
		//i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // This is probably not necessary since the activity has launch mode singleInstance.
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		homeActivity.startActivity(i);
	}
	
	public static Intent makePlayVideoIntent(Intent intent, HomeActivity homeActivity, Friend friend){
		intent.setAction(IntentHandler.IntentActions.PLAY_VIDEO);
		Uri uri = new Uri.Builder().appendPath(IntentHandler.IntentActions.PLAY_VIDEO).appendQueryParameter(IntentHandler.IntentParamKeys.FRIEND_ID, friend.getId()).build();
		intent.setData(uri);
		return intent;
	}
	
	public static Boolean screenIsLocked(Context context){
		KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
		return (Boolean) km.inKeyguardRestrictedInputMode();
	}
	
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	private static Boolean screenIsOff(Context context){
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		if (android.os.Build.VERSION.SDK_INT < 20)
			return (Boolean) !pm.isScreenOn();
		else
			return (Boolean) !pm.isInteractive();
	}

}
