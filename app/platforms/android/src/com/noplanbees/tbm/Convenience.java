package com.noplanbees.tbm;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

public class Convenience {
	
	public static int dpToPx(Context context, int dp){
		Resources r = context.getResources();
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
	}
}
