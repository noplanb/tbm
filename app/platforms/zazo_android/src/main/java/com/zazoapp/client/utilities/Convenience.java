package com.zazoapp.client.utilities;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import com.zazoapp.client.Config;
import com.zazoapp.client.R;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.notification.NotificationAlertManager;
import com.zazoapp.client.ui.MainActivity;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Locale;

public class Convenience {
    public static final String TAG = Convenience.class.getSimpleName();
    public static final String ON_NOT_ENOUGH_SPACE_ACTION = "on_not_enough_space_action";

    public static float dpToPx(Context context, float dp){
		Resources r = context.getResources();
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
	}
	
	public static int dpToPx(Context context, int dp){
		Resources r = context.getResources();
		return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
	}
	
	public static float pxToDp(Context context, int px){
		Resources resources = context.getResources();
	    DisplayMetrics metrics = resources.getDisplayMetrics();
	    float dp = px / (metrics.densityDpi / 160f);
	    return dp;	}
	
	public static Bitmap bitmapWithPath(String path){
		return bitmapWithFile(new File(path));
	}
	
	public static Bitmap bitmapWithFile(File file){
        Bitmap bmp = null;
		try {
	        File thumbFile = file;
	        FileInputStream fis;
			fis = FileUtils.openInputStream(thumbFile);
	        bmp = BitmapFactory.decodeStream(fis);
		} catch (IOException e) {
			String msg = "bitmapWithFile: IOException: " + e.getMessage();
			Log.i(TAG, msg);
			Dispatch.dispatch(msg);
		}
		return bmp;
	}
	
	
	// ---------------------------
	// Task and activities helpers
	// ---------------------------
	public static List <ActivityManager.RunningTaskInfo> runningTasks(Context context){
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		return am.getRunningTasks(10000);
	}
	
	public static ActivityManager.RunningTaskInfo ourTaskInfo(Context context){
		ActivityManager.RunningTaskInfo result = null;
		for (ActivityManager.RunningTaskInfo ti : runningTasks(context)){
			if (ti.baseActivity.getPackageName().equalsIgnoreCase(Convenience.class.getPackage().getName()))
				return ti;
		}
		return result;
	}
	
	public static void printRunningTaskInfo(Context context){
		for (ActivityManager.RunningTaskInfo ti : runningTasks(context)){
			printTaskInfo(ti);
		}
	}
	
	public static void printOurTaskInfo(Context context){
		Log.i(TAG, "printOurTaskInfo");
		ActivityManager.RunningTaskInfo ti = ourTaskInfo(context);
		if (ti == null){
			Log.i(TAG, "printOurTaskInfo: NULL");
			return;
		}
		printTaskInfo(ti);
	}
	
	public static Integer numActivitiesInOurTask(Context context){
		Integer result = null;
		ActivityManager.RunningTaskInfo ti = ourTaskInfo(context);
		if (ti == null)
			return result;
		return ti.numActivities;
	}
	
	public static void printTaskInfo(ActivityManager.RunningTaskInfo ti){
		Log.i(TAG, "--------");
		Log.i(TAG, "baseActivity: " + ti.baseActivity.toShortString());
		Log.i(TAG,"baseActivity package name: " + ti.baseActivity.getPackageName());
		Log.i(TAG, "our package: " + Convenience.class.getPackage());
		Log.i(TAG, "numActivities: " + ti.numActivities);
		Log.i(TAG, "numRunning: " + ti.numRunning);
		Log.i(TAG, "topActivity: " + ti.topActivity.toString());
	}

	public static void printBundle(Bundle bundle) {
		if (bundle == null){
			Log.d(TAG, "null bundle");
			return;
		}
		for(String key : bundle.keySet()){
			if (bundle.get(key) == null){
				Log.d(TAG, key + "=null");
			} else {
				Log.d(TAG, key + "=" + bundle.get(key).toString());
			}
		}
	}
	
	public static void printCursor(Cursor c){
		c.moveToFirst();
		do {
			String msgData = "";
			for(int i=0; i<c.getColumnCount(); i++){
				msgData += " " + c.getColumnName(i) + ":" + c.getString(i) + "\n";
			}
			msgData += "=============\n";
			Log.i(TAG, msgData);
		} while (c.moveToNext());
	}

	


    /*
    * @return: Point where x - width and y - height
    * */
    public static Point getScreenDimensions(Context context){
        Point size = new Point();
        WindowManager w = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2)
        {
            w.getDefaultDisplay().getSize(size);
        }
        else
        {
            Display d = w.getDefaultDisplay();
            size.x = d.getWidth();
            size.y = d.getHeight();
        }
        return size;

    }

    public static boolean screenIsOff(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return !pm.isScreenOn();
    }

    public static boolean screenIsLocked(Context context) {
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return km.inKeyguardRestrictedInputMode();
    }

    public static boolean screenIsLockedOrOff(Context context) {
        return screenIsLocked(context) || screenIsOff(context);
    }

    public static void copy(File src, File dst) {
        Log.d(TAG, "copying " + src.getName() + " " + String.valueOf(src.length()));
        if (dst == null) {
            String root = Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator + "ZazoVideos"+File.separator;
            dst = new File(root);
            if(!dst.exists()) {
                dst.mkdir();
            }
            dst = new File(root + src.getName());
        }

        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);

            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static File createFileFromAssets(AssetManager am, String dstPath, String assetFile) throws Exception {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        File f = new File(dstPath);
        if (f.exists()) {
            return f;
        }
        try{
            inputStream = am.open(assetFile);
            outputStream = new FileOutputStream(f);
            byte buffer[] = new byte[1024];
            int length = 0;

            while((length=inputStream.read(buffer)) > 0) {
                outputStream.write(buffer,0,length);
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {}
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {}
            }
        }

        return f;
    }

    public static boolean isLanguageSupported(Context c) {
        String language = Locale.getDefault().getLanguage();
        String[] localizations = c.getResources().getStringArray(R.array.localizations);
        for (String localization : localizations) {
            if (localization.equals(language)) {
                return true;
            }
        }
        return false;
    }

    public static void saveJsonToFile(String json, String path) {
        try {
            File f = new File(path);
            if (f.exists())
                f.delete();
            FileOutputStream fos = new FileOutputStream(f, true);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write(json);
            osw.close();
            fos.close();
        } catch (IOException e) {
            Dispatch.dispatch("ERROR: This should never happen." + e.getMessage() + e.toString());
            throw new RuntimeException();
        }
    }

    public static String getJsonFromFile(String path) {
        String json;
        try {
            File f = new File(path);
            FileInputStream fis = new FileInputStream(f);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            String s = "";
            StringBuilder sb = new StringBuilder();
            while ((s = br.readLine()) != null) {
                sb.append(s);
            }
            json = sb.toString();
            br.close();
            isr.close();
            fis.close();
        } catch (FileNotFoundException e) {
            Log.i(TAG, e.getMessage() + e.toString());
            return null;
        } catch (IOException e) {
            Dispatch.dispatch(e.getMessage() + e.toString());
            return null;
        }
        return json;
    }

    public static Typeface getTutorialTypeface(Context context) {
        boolean isLanguageSupported = isLanguageSupported(context);
        AssetManager am = context.getAssets();
        Typeface tf = null;
        if (isLanguageSupported) {
            try {
                tf = Typeface.createFromAsset(am, "fonts/tutorial-" + Locale.getDefault().getLanguage() + ".ttf");
            } catch (RuntimeException e) {
                tf = Convenience.getTypeface(context);
            }
        } else {
            tf = Typeface.createFromAsset(am, "fonts/tutorial-en.ttf");
        }
        return tf;
    }

    public static Typeface getTypeface(Context context, CharSequence name) {
        return Typeface.createFromAsset(context.getAssets(), "fonts/" + name + ".ttf");
    }

    public static Typeface getTypeface(Context context) {
        return getTypeface(context, "Roboto-Medium");
    }

    public static float availableRoomSpace(File file) {
        StatFs stat = new StatFs(file.getPath());
        long bytesAvailable;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
            bytesAvailable = getAvailableBytes(stat);
        } else {
            //noinspection deprecation
            bytesAvailable = (long) stat.getBlockSize() * (long) stat.getAvailableBlocks();
        }

        return bytesAvailable / (1024.f * 1024.f);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static long getAvailableBytes(StatFs stat) {
        return stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
    }

    /**
     *
     * @param context
     * @return true if it is enough space
     */
    public static boolean checkAndNotifyNoSpace(Context context) {
        boolean result = availableRoomSpace(new File(Config.homeDirPath(context))) > DebugConfig.Int.MIN_ROOM_SPACE_RESTRICTION.get();
        if (!result) {
            Log.d(TAG, "Not enough space");
            String title = context.getString(R.string.alert_not_enough_space_title);
            String message = context.getString(R.string.alert_not_enough_space_message);
            Intent intent = new Intent(context.getApplicationContext(), MainActivity.class);
            NotificationAlertManager.alert(context, title, message, null, NotificationAlertManager.NotificationType.NO_SPACE_LEFT.id(), intent);
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ON_NOT_ENOUGH_SPACE_ACTION));
        }
        return result;
    }

    public static int getStringDependentItem(@NonNull CharSequence base, @NonNull int[] items) {
        return items[Math.abs(base.hashCode() % items.length)];
    }

    public static <T> T getStringDependentItem(@NonNull CharSequence base, @NonNull T[] items) {
        return items[Math.abs(base.hashCode() % items.length)];
    }

    public static RectF getViewRect(View view) {
        int[] location = new int[2];
        view.getLocationInWindow(location);
        int left = location[0];
        int top = location[1];
        int right = left + view.getWidth();
        int bottom = top + view.getHeight();
        return new RectF(left, top, right, bottom);
    }

    public static int[] getViewInnerCircle(View view) {
        int[] circleData = new int[3];
        int[] location = new int[2];
        view.getLocationInWindow(location);
        circleData[0] = location[0] + view.getWidth() / 2;
        circleData[1] = location[1] + view.getHeight() / 2;
        circleData[2] = Math.min(view.getWidth(), view.getHeight()) / 2;
        return circleData;
    }

    public static int[] getViewOuterCircle(View view) {
        int[] circleData = new int[3];
        int[] location = new int[2];
        view.getLocationInWindow(location);
        circleData[0] = location[0] + view.getWidth() / 2;
        circleData[1] = location[1] + view.getHeight() / 2;
        circleData[2] = (int) (Math.max(view.getWidth(), view.getHeight()) * 0.8);
        return circleData;
    }
}
