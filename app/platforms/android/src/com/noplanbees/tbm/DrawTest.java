package com.noplanbees.tbm;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class DrawTest {
	private final String TAG = this.getClass().getSimpleName();
	private Activity activity;
	private Context context;
	private SurfaceHolder surfaceHolder;
	private DrawView drawView;
	private boolean drawable = false;

	public DrawTest(Activity a){
		Log.i(TAG, "constructor");
		activity = a;
		context = activity.getApplicationContext();
		drawView = new DrawView(context);
		Log.i(TAG, "Got drawview = " + drawView.toString());
		FrameLayout fl = (FrameLayout) activity.findViewById(R.id.drawTest);
		Log.i(TAG, "Got frameLayout = " + fl.toString());
		((ViewGroup) fl).addView(drawView);
	}

	private void drawShape(){
		Log.i(TAG, "drawShape");
		Canvas c = surfaceHolder.lockCanvas();
		ShapeDrawable mDrawable = new ShapeDrawable(new RectShape());
		mDrawable.getPaint().setColor(0xff74AC23);
		mDrawable.setBounds(0, 0, 10, 10);
		mDrawable.draw(c);
		
		Path borderPath = new Path();
		borderPath.lineTo(100,100);

		Paint paint = new Paint();
		paint.setColor(Color.RED);
		paint.setStrokeWidth(5);
		paint.setStyle(Paint.Style.STROKE);
		
		c.drawPath(borderPath, paint);
		surfaceHolder.unlockCanvasAndPost(c);
	}


	private class DrawView extends SurfaceView implements SurfaceHolder.Callback{

		public DrawView(Context context) {
			super(context);
			surfaceHolder = getHolder();
			surfaceHolder.addCallback(this);
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			drawable = true;
			Log.i(TAG, "surfaceCreated");
			drawShape();
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {			
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {	
			drawable = false;
		}		
	}



}
