package com.zazoapp.s3networktest;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;
import com.zazoapp.s3networktest.dispatch.Dispatch;
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

public class Convenience {
    public static final String TAG = Convenience.class.getSimpleName();

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
        return (Boolean) km.inKeyguardRestrictedInputMode();
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
}
