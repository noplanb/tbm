package com.noplanbees.tbm.utilities;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class Convenience {
	public final static String STAG = Convenience.class.getSimpleName();
	
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
			Log.i(STAG, "bitmapWithFile: IOException: " + e.getMessage());
		}
		return bmp;
	}
	
	public static int getFriendPosByUiPos(int friendPos){
		switch(friendPos){
		case 0:
			return 7;
		case 1:
			return 6;
		case 2:
			return 4;
		case 3:
			return 5;
		case 4:
			return 0;
		case 5:
			return 3;
		case 6:
			return 1;
		case 7:
			return 2;
		default:
			return 0;
		}
	}
	
	public static int getUiPosForFriendPos(int friendPos){
		switch(friendPos){
		case 0:
			return 4;
		case 1:
			return 6;
		case 2:
			return 7;
		case 3:
			return 5;
		case 4:
			return 2;
		case 5:
			return 3;
		case 6:
			return 1;
		case 7:
			return 0;
		default:
			return 0;
		}
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
		Log.i(STAG, "printOurTaskInfo");
		ActivityManager.RunningTaskInfo ti = ourTaskInfo(context);
		if (ti == null){
			Log.i(STAG, "printOurTaskInfo: NULL");
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
		Log.i(STAG, "--------");
		Log.i(STAG, "baseActivity: " + ti.baseActivity.toShortString());
		Log.i(STAG,"baseActivity package name: " + ti.baseActivity.getPackageName());
		Log.i(STAG, "our package: " + Convenience.class.getPackage());
		Log.i(STAG, "numActivities: " + ti.numActivities);
		Log.i(STAG, "numRunning: " + ti.numRunning);
		Log.i(STAG, "topActivity: " + ti.topActivity.toString());
	}

	public static void printBundle(Bundle bundle) {
		if (bundle == null){
			Log.d(STAG, "null bundle");
			return;
		}
		for(String key : bundle.keySet()){
			if (bundle.get(key) == null){
				Log.d(STAG, key + "=null");
			} else {
				Log.d(STAG, key + "=" + bundle.get(key).toString());
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
			Log.i(STAG, msgData);
		} while (c.moveToNext());
	}

	

	
	//------------
	// URI helpers
	//------------
	
}
