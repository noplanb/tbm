package com.zazoapp.client.debug;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
* Created by skamenkovych@codeminders.com on 2/19/2015.
*/
public class ZazoGestureListener implements View.OnTouchListener {
    private static final String TAG = ZazoGestureListener.class.getSimpleName();

    private float bounce;
    private float offset;

    private float xStartPoint;
    private float yStartPoint;
    private GesturePhase phase;
    private Context context;

    private enum GesturePhase {
        IDLE,
        LONG_PRESS_CATCHING,
        LONG_PRESSED,
        SLIDE_1_DONE,
        SLIDE_2_DONE
    }

    public ZazoGestureListener(Context context) {
        this.context = context;
    }

    @Override
    public boolean onTouch(final View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                xStartPoint = event.getX();
                yStartPoint = event.getY();
                offset = 2.0f * v.getWidth();
                bounce = v.getHeight() / 1.5f;
                //Log.w(TAG, "@@@ [" + xStartPoint + ";" + yStartPoint + "]:[" + offset + ";" + bounce + "]");
                phase = GesturePhase.LONG_PRESS_CATCHING;
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (phase == GesturePhase.LONG_PRESS_CATCHING) {
                            Log.w(TAG, "@@@ intercepted");
                            phase = GesturePhase.LONG_PRESSED;
                            v.setAlpha(0.8f);
                        }
                    }
                }, 400);
                break;
            case MotionEvent.ACTION_MOVE:
                float xSlide = Math.abs(xStartPoint - event.getX());
                float ySlide = Math.abs(yStartPoint - event.getY());
                //Log.w(TAG, "@@@ bounce [" + xSlide  + ";" + ySlide + "]");
                if (ySlide > bounce) {
                    phase = GesturePhase.IDLE;
                }
                switch (phase) {
                    case LONG_PRESS_CATCHING:
                        if (xSlide > bounce) {
                            phase = GesturePhase.IDLE;
                        }
                        break;
                    case LONG_PRESSED:
                        if (xSlide > offset) {
                            phase = GesturePhase.SLIDE_1_DONE;
                        }
                        break;
                    case SLIDE_1_DONE:
                        if (xSlide < bounce) {
                            phase = GesturePhase.SLIDE_2_DONE;
                            v.post(new Runnable() {
                                @Override
                                public void run() {
                                    phase = GesturePhase.IDLE;
                                    v.setAlpha(1.0f);
                                    Intent intent = new Intent(context, DebugSettingsActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(intent);
                                }
                            });
                        }
                        break;
                }

                break;
            default:
                phase = GesturePhase.IDLE;
                v.setAlpha(1.0f);
                break;
        }
        return true;
    }
}
