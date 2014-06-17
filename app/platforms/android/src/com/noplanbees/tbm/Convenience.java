package com.noplanbees.tbm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.TypedValue;

public class Convenience {
	public final static String STAG = Convenience.class.toString();
	
	public static int dpToPx(Context context, int dp){
		Resources r = context.getResources();
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
	}
	
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
}
