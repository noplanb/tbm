package com.zazoapp.client.notification;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.zazoapp.client.Config;
import com.zazoapp.client.core.IntentHandler;
import com.zazoapp.client.R;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.ui.LockScreenAlertActivity;
import com.zazoapp.client.ui.MainActivity;


public class NotificationAlertManager {

    private static final String TAG = NotificationAlertManager.class.getSimpleName();

    // ----------------------------
	// Attributes for notifications
	// ----------------------------
	public static final String TITLE_KEY = "titleKey";
	public static final String SUB_TITLE_KEY = "subTitleKey";
	public static final String LARGE_IMAGE_PATH_KEY = "largeImagePathKey";
	public static final String SMALL_ICON_KEY = "smallIconKey";
	
	private static final String subTitle = Config.appName;

    private static SoundPool soundPool;
    private static int beepTone;

    private static Bitmap largeImage(Friend friend){
		return friend.sqThumbBitmap();
	}

	private static String largeImagePath(Friend friend){
		return friend.thumbPath();
	}

	private static String title(Friend friend){
		return "From " + friend.get(Friend.Attributes.FIRST_NAME) + "!";
	}

    private static int getNotificationIcon() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) ? R.drawable.ic_notification_white : R.drawable.ic_launcher;
    }
	// -------------------
	// Notification Alerts
	// -------------------
	
	// Public methods
	public static void alert(Context context, Friend friend, String videoId){
		Log.i(TAG, "alert");
		
		if ( screenIsLocked(context) || screenIsOff(context))
			postLockScreenAlert(context, friend, videoId);
		
		postNativeAlert(context, friend, videoId);
	}
	
	private static void cancelNativeAlerts(Context context){
		Log.i(TAG, "cancelNativeAlerts");
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancelAll();
	}

    @SuppressWarnings("deprecation")
    public static void init(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes.Builder attrsBuilder = new AudioAttributes.Builder();
            attrsBuilder.setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT);
            attrsBuilder.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION);
            soundPool = new SoundPool.Builder().setAudioAttributes(attrsBuilder.build()).build();
        } else {
            soundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
        }
        beepTone = soundPool.load(context, R.raw.beep, 1);

        cancelNativeAlerts(context);
    }

    public static void cleanUp() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    public static void playTone() {
        if (soundPool != null)
            soundPool.play(beepTone, 0.3f, 0.3f, 0, 0, 1);
    }

	// Private 
	private static void postNativeAlert(Context context, Friend friend, String videoId) {
		Log.i(TAG, "postNativeAlert");
		final int NOTIFICATION_ID = 1;
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Intent intent = new Intent(context.getApplicationContext(), MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, makePlayVideoIntent(intent, context, friend), 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSound(getNotificationToneUri(context))
                .setSmallIcon(getNotificationIcon())
                .setContentTitle(title(friend))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(title(friend)))
                .setContentText(subTitle)
                .setContentIntent(contentIntent)
                .setColor(context.getResources().getColor(R.color.green))
                .setAutoCancel(true);

        if (friend.thumbExists()) {
            mBuilder.setLargeIcon(largeImage(friend));
        } else {
            mBuilder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_no_pic_z));
        }

		notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}

	private static void postLockScreenAlert(Context context, Friend friend, String videoId) {
		Log.i(TAG, "postLockScreenAlert");
		Intent ri = new Intent(context, LockScreenAlertActivity.class);
		Intent i = makePlayVideoIntent(ri, context, friend);
		i.putExtra(IntentHandler.IntentParamKeys.FRIEND_ID, friend.getId());
		i.putExtra(LARGE_IMAGE_PATH_KEY, largeImagePath(friend));
		i.putExtra(SMALL_ICON_KEY, R.drawable.ic_launcher);
		i.putExtra(TITLE_KEY, title(friend));
		i.putExtra(SUB_TITLE_KEY, subTitle);
		i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		i.addFlags(Intent.FLAG_FROM_BACKGROUND);
		//i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // This is probably not necessary since the activity has launch mode singleInstance.
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(i);
	}
	
	public static Intent makePlayVideoIntent(Intent intent, Context context, Friend friend){
		intent.setAction(IntentHandler.IntentActions.PLAY_VIDEO);
		Uri uri = new Uri.Builder().appendPath(IntentHandler.IntentActions.PLAY_VIDEO).appendQueryParameter(IntentHandler.IntentParamKeys.FRIEND_ID, friend.getId()).build();
		intent.setData(uri);
		return intent;
	}
	
	public static boolean screenIsLocked(Context context){
		KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
		return km.inKeyguardRestrictedInputMode();
	}
	
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public static boolean screenIsOff(Context context){
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		if (android.os.Build.VERSION.SDK_INT < 20)
			return !pm.isScreenOn();
		else
			return !pm.isInteractive();
	}

    private static Uri getNotificationToneUri(Context context) {
        return Uri.parse("android.resource://"+ context.getPackageName() + "/" + R.raw.notification_tone);
    }
}
