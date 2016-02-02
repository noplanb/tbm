package com.zazoapp.client.ui.view;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import com.zazoapp.client.R;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.ui.ViewGroupGestureRecognizer;
import com.zazoapp.client.ui.ZazoManagerProvider;

import java.util.ArrayList;

/**
 * Created by skamenkovych@codeminders.com on 7/24/2015.
 */
class NineViewGroupGestureRecognizer extends ViewGroupGestureRecognizer {
    private NineViewGroup nineViewGroup;
    private ZazoManagerProvider managerProvider;

    public NineViewGroupGestureRecognizer(Activity a, NineViewGroup group, ArrayList<View> tvs, ZazoManagerProvider provider) {
        super(a, tvs);
        this.nineViewGroup = group;
        this.managerProvider = provider;
    }

    @Override
    public boolean click(View v) {
        if (nineViewGroup.getGestureListener() == null)
            return false;

        if (nineViewGroup.isCenterView(v))
            return nineViewGroup.getGestureListener().onCenterClick(v);
        return nineViewGroup.getGestureListener().onSurroundingClick(v, nineViewGroup.positionOfView(v));
    }

    @Override
    public boolean startLongpress(View v) {
        if (nineViewGroup.getGestureListener() == null)
            return false;

        if (nineViewGroup.isCenterView(v))
            return nineViewGroup.getGestureListener().onCenterStartLongpress(v);
        return nineViewGroup.getGestureListener().onSurroundingStartLongpress(v, nineViewGroup.positionOfView(v));
    }

    @Override
    public boolean endLongpress(View v) {
        if (nineViewGroup.getGestureListener() == null)
            return false;
        return nineViewGroup.getGestureListener().onEndLongpress();
    }

    @Override
    public boolean bigMove(View v) {
        return managerProvider.getFeatures().isUnlocked(Features.Feature.ABORT_RECORDING) &&
                nineViewGroup.handleAbort(v, R.string.toast_dragged_finger_away);
    }

    @Override
    public boolean abort(View v, int reason) {
        return nineViewGroup.handleAbort(v, reason);
    }

    @Override
    public void notifyMove(View target, double startX, double startY, double offsetX, double offsetY) {
        if (nineViewGroup.getSpinStrategy() != null && !nineViewGroup.isCenterView(target)) {
            if (checkTouch(startX, startY)) {
                nineViewGroup.getSpinStrategy().spin(startX, startY, offsetX, offsetY);
            }
        }
    }

    @Override
    public void startMove(View target, double startX, double startY, double offsetX, double offsetY) {
        if (!nineViewGroup.isCenterView(target)) {

            if (target != null) {
                double x0 = NineViewGroup.SpinStrategy.getInitialPositionX(nineViewGroup.getFrame(NineViewGroup.Box.CENTER));
                double y0 = NineViewGroup.SpinStrategy.getInitialPositionY(nineViewGroup.getFrame(NineViewGroup.Box.CENTER));
                double angle = Math.atan2(NineViewGroup.SpinStrategy.getInitialPositionY(target) - y0,
                        NineViewGroup.SpinStrategy.getInitialPositionX(target) - x0);
                double prevAngle = NineViewGroup.SpinStrategy.normalizedAngle(angle - Math.PI / 9);
                double nextAngle = NineViewGroup.SpinStrategy.normalizedAngle(angle + Math.PI / 9);
                if (angleInBetween(Math.atan2(offsetY, offsetX), prevAngle, nextAngle)) {
                    runSurroundingMovingAway(target);
                    return;
                }

                //if (angleInBetween(NineViewGroup.SpinStrategy.normalizedAngle(Math.atan2(offsetY, offsetX) + Math.PI),
                //        prevAngle, nextAngle)) {
                //    runSurroundingMovingIn(target);
                //    return;
                //}
            }
            if (nineViewGroup.getSpinStrategy() != null && checkTouch(startX, startY)) {
                nineViewGroup.getSpinStrategy().initSpin(startX, startY, offsetX, offsetY);
            }
        }
    }

    @Override
    public void endMove(double startX, double startY, double offsetX, double offsetY) {
        if (nineViewGroup.getSpinStrategy() != null  && checkTouch(startX, startY)) {
            nineViewGroup.getSpinStrategy().finishSpin(startX, startY, offsetX, offsetY);
        }
    }

    @Override
    public void onTouch(double startX, double startY) {
        if (nineViewGroup.getSpinStrategy() != null && checkTouch(startX, startY)) {
            nineViewGroup.getSpinStrategy().stopSpin(startX, startY);
        }
    }

    private boolean checkTouch(double startX, double startY) {
        double bounce = nineViewGroup.getContext().getResources().getDimensionPixelSize(R.dimen.nine_view_sliding_min_bounce) / 2;
        return startX > bounce && startX < nineViewGroup.getWidth() - bounce;
    }

    @Override
    public boolean isSliding() {
        return nineViewGroup.getSpinStrategy() != null && nineViewGroup.getSpinStrategy().isSpinning();
    }

    @Override
    public boolean isSlidingSupported() {
        return nineViewGroup.getSpinStrategy() != null && nineViewGroup.isSpinEnabled();
    }

    private boolean angleInBetween(double angle, double startAngle, double endAngle) {
        if (endAngle > startAngle) {
            return startAngle < angle && angle < endAngle;
        } else {
            endAngle = NineViewGroup.SpinStrategy.normalizedAngle(endAngle + Math.PI);
            startAngle = NineViewGroup.SpinStrategy.normalizedAngle(startAngle + Math.PI);
            angle = NineViewGroup.SpinStrategy.normalizedAngle(angle + Math.PI);
            return startAngle < angle && angle < endAngle;
        }
    }

    private void runSurroundingMovingAway(final View view) {
        runAction(new Runnable() {
            @Override
            public void run() {
                nineViewGroup.getGestureListener().onSurroundingMovingAway(view, nineViewGroup.positionOfView(view));
            }
        });
    }

    private void runSurroundingMovingIn(final View view) {
        runAction(new Runnable() {
            @Override
            public void run() {
                nineViewGroup.getGestureListener().onSurroundingMovingIn(view, nineViewGroup.positionOfView(view));
            }
        });
    }

    @Override
    protected boolean isAbortLongpressMove(MotionEvent event) {
        return managerProvider.getFeatures().isUnlocked(Features.Feature.ABORT_RECORDING) && super.isAbortLongpressMove(event);
    }
}
