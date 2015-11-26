package com.zazoapp.client.notification;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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
import com.zazoapp.client.R;
import com.zazoapp.client.core.IntentHandlerService;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.IncomingVideo;
import com.zazoapp.client.model.IncomingVideoFactory;
import com.zazoapp.client.ui.LockScreenAlertActivity;
import com.zazoapp.client.ui.MainActivity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public class NotificationAlertManager {

    public enum Tone {
        BEEP,
        FEATURE_UNLOCK,
        ;

        int get() {
            switch (this) {
                case BEEP:
                    return beepTone;
                case FEATURE_UNLOCK:
                    return featureUnlockTone;
                default:
                    return 0;
            }
        }
    }

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
    private static int featureUnlockTone;

    private static Bitmap largeImage(Friend friend){
		return friend.sqThumbBitmap();
	}

	private static String largeImagePath(Friend friend){
		return friend.thumbPath();
	}

    private static String title(Context context, int count) {
        Resources r = context.getResources();
        return r.getQuantityString(R.plurals.notification_title, count, count);
    }

    private static String title(Context context, Friend friend){
        return context.getString(R.string.notification_from, friend.get(Friend.Attributes.FIRST_NAME));
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

    public static void alert(Context context, String title, String subTitle, long[] vibratePattern, int id) {
        if (id <= 0) {
            id = 2;
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(context.getApplicationContext(), MainActivity.class);
        PendingIntent openAppIntent = PendingIntent.getActivity(context, 0, intent, 0);

        NotificationCompat.BigTextStyle notiStyle = new NotificationCompat.BigTextStyle();
        notiStyle.setBigContentTitle(title);
        notiStyle.bigText(subTitle);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .setVibrate(vibratePattern)
                //.setSound(getNotificationToneUri(context))
                .setSmallIcon(getNotificationIcon())
                .setContentTitle(title)
                .setContentText(subTitle)
                .setStyle(notiStyle)
                .setColor(context.getResources().getColor(R.color.green))
                .setContentIntent(openAppIntent)
                .setAutoCancel(true);

        notificationManager.cancel(id);
        notificationManager.notify(id, mBuilder.build());
    }

	private static void cancelNativeAlerts(Context context){
		Log.i(TAG, "cancelNativeAlerts");
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancelAll();
	}

    public static void cancelNativeAlert(Context context, int id) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(id);
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
        featureUnlockTone = soundPool.load(context, R.raw.feature_unlock, 1);

        cancelNativeAlerts(context);
    }

    public static void cleanUp() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    public static void playTone(Tone tone, float velocity) {
        if (soundPool != null)
            soundPool.play(tone.get(), velocity, velocity, 0, 0, 1);
    }

    public static float getVelocity(Context context, int stream) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamVolume(stream);
    }

	// Private 
	private static void postNativeAlert(Context context, Friend friend, String videoId) {
		Log.i(TAG, "postNativeAlert");
		final int NOTIFICATION_ID = 1;
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Intent intent = new Intent(context.getApplicationContext(), MainActivity.class);
		PendingIntent playVideoIntent = PendingIntent.getActivity(context, 0,
                makePlayVideoIntent(intent, context, friend), 0);

        int unviewedCount = IncomingVideoFactory.getFactoryInstance().allNotViewedCount() + 1;
        String title = title(context, unviewedCount);
        NotificationCompat.BigTextStyle notiStyle = new NotificationCompat.BigTextStyle();
        notiStyle.bigText(formatFriendsList(context, friend, true));
        notiStyle.setBigContentTitle(title);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setSound(getNotificationToneUri(context))
                .setSmallIcon(getNotificationIcon())
                .setContentTitle(title)
                .setStyle(notiStyle)
                .setNumber(unviewedCount)
                .setColor(context.getResources().getColor(R.color.green))
                .setAutoCancel(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            PendingIntent openAppIntent = PendingIntent.getActivity(context, 0, intent, 0);
            mBuilder.setContentIntent(openAppIntent);
            mBuilder.setContentText(formatFriendsList(context, friend, false));
            if (unviewedCount == friend.incomingVideoNotViewedCount() + 1) {
                mBuilder.addAction(R.drawable.ic_action_view, context.getString(R.string.action_view), playVideoIntent);
            }
        } else {
            mBuilder.setContentIntent(playVideoIntent);
            mBuilder.setContentText(friend.getFullName());
        }
        if (unviewedCount == friend.incomingVideoNotViewedCount() + 1) {
            if (friend.thumbExists()) {
                mBuilder.setLargeIcon(largeImage(friend));
            } else {
                mBuilder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_no_pic_z));
            }
        }

        notificationManager.cancel(NOTIFICATION_ID);
		notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}

    private static String formatFriendsList(Context context, Friend friend, boolean longList) {
        ArrayList<IncomingVideo> notViewedVideos = IncomingVideoFactory.getFactoryInstance().allNotViewed();
        Set<String> friendIds = new LinkedHashSet<>();
        friendIds.add(friend.getId());
        for (IncomingVideo video : notViewedVideos) {
            friendIds.add(video.getFriendId());
        }
        FriendFactory friends = FriendFactory.getFactoryInstance();
        StringBuilder friendsList = new StringBuilder();
        int friendCount = 0;
        for (String id : friendIds) {
            if (friendCount == 5 && longList) {
                friendsList.append("'\n");
                friendsList.append(context.getString(R.string.notification_list_more, friendIds.size() - 5));
                break;
            }
            Friend f = friends.find(id);
            if (f != null) {
                if (friendCount > 0) {
                    friendsList.append(longList ? "\n" : ", ");
                    if (friendCount == 3 && !longList) {
                        friendsList.append(context.getString(R.string.notification_list_more, friendIds.size() - 3));
                        break;
                    }
                }
                if (longList) {
                    friendsList.append(f.getFullName());
                } else {
                    friendsList.append((friendIds.size() == 1) ? f.getFullName() :f.getUniqueName());
                }
                friendCount++;
            }
        }
        return friendsList.toString();
    }

    private static void postLockScreenAlert(Context context, Friend friend, String videoId) {
		Log.i(TAG, "postLockScreenAlert");
		Intent ri = new Intent(context, LockScreenAlertActivity.class);
		Intent i = makePlayVideoIntent(ri, context, friend);
		i.putExtra(IntentHandlerService.IntentParamKeys.FRIEND_ID, friend.getId());
		i.putExtra(LARGE_IMAGE_PATH_KEY, largeImagePath(friend));
		i.putExtra(SMALL_ICON_KEY, R.drawable.ic_launcher);
		i.putExtra(TITLE_KEY, title(context, friend));
		i.putExtra(SUB_TITLE_KEY, subTitle);
		i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		i.addFlags(Intent.FLAG_FROM_BACKGROUND);
		//i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // This is probably not necessary since the activity has launch mode singleInstance.
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(i);
	}
	
	public static Intent makePlayVideoIntent(Intent intent, Context context, Friend friend){
        Intent i = new Intent(intent);
        i.setAction(IntentHandlerService.IntentActions.PLAY_VIDEO);
        Uri uri = new Uri.Builder().appendPath(IntentHandlerService.IntentActions.PLAY_VIDEO).appendQueryParameter(
                IntentHandlerService.IntentParamKeys.FRIEND_ID, friend.getId()).build();
        i.setData(uri);
        return i;
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
