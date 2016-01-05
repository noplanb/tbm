package com.zazoapp.client.ui;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import com.zazoapp.client.ui.view.NineViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 7/8/2015.
 */
public class RectangleSpin extends NineViewGroup.SpinStrategy {

    public int left;
    public int top;
    public int right;
    public int bottom;
    public double x0;
    public double y0;
    private double aX;
    private double aY;
    private double bX;
    private double bY;
    private boolean isAnimating;
    private boolean isInited;
    private double currentAngle;
    private double previousAngle;
    private long lastTime;
    private long previousLastTime;
    private long animationLastTime;
    private double angle;
    private ValueAnimator valueAnimator;
    private double topLeftAngle;
    private double topRightAngle;
    private int width;
    private int height;

    public RectangleSpin(NineViewGroup viewGroup) {
        super(viewGroup);
    }

    @Override
    public double calculateAngle() {
        double startAngle = Math.atan2(aY, aX);
        double endAngle = Math.atan2(bY, bX);
        double diff = normalizedAngle(endAngle - startAngle);
        return diff;
    }

    @Override
    protected void spin(double startX, double startY, double offsetX, double offsetY) {
        if (getViewGroup() == null || getViewGroup().getChildCount() != 9) {
            return;
        }
        if (offsetX == 0 && offsetY == 0 || !isInited) {
            return;
        }
        aX = startX - x0;
        aY = startY - y0;
        bX = startX + offsetX - x0;
        bY = startY + offsetY - y0;
        applySpin(calculateAngle());
    }

    @Override
    protected void stopSpin(double startX, double startY) {
        initSpin(startX, startY, 0, 0);
    }

    @Override
    protected void cancelSpin() {

    }

    @Override
    protected void initSpin(double startX, double startY, double offsetX, double offsetY) {
        isInited = false;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        View centerView = getViewGroup().getCenterFrame();
        x0 = getInitialPositionX(centerView);
        y0 = getInitialPositionY(centerView);
        View topLeftView = getViewGroup().getFrame(NineViewGroup.Box.TOP_LEFT);
        View bottomRightView = getViewGroup().getFrame(NineViewGroup.Box.BOTTOM_RIGHT);
        left = topLeftView.getLeft() + topLeftView.getWidth() / 2;
        top = topLeftView.getTop() + topLeftView.getHeight() / 2;
        right = bottomRightView.getLeft() + bottomRightView.getWidth() / 2;
        bottom = bottomRightView.getTop() + bottomRightView.getHeight() / 2;
        width = right - left;
        height = bottom - top;
        angle = normalizedAngle(angle + currentAngle);
        previousAngle = currentAngle = 0;
        topLeftAngle = NineViewGroup.Box.TOP_LEFT.getInitialAngle();
        topRightAngle = NineViewGroup.Box.TOP_RIGHT.getInitialAngle();
        isInited = true;
    }

    @Override
    protected void finishSpin(double startX, double startY, double offsetX, double offsetY) {
        if (!isInited) {
            return;
        }
        final double angleDiff = normalizedAngle(currentAngle - previousAngle) / Math.abs(lastTime - previousLastTime);
        long duration = Math.min(Math.max((long) (200000000000.0 * Math.abs(angleDiff)), 200), 2000);
        double maxDistance = Math.abs(angleDiff * duration) * 333333;
        final double maxEndAngle = angle + currentAngle + Math.copySign(maxDistance, angleDiff);
        double quarter = Math.PI / 4;
        final double normEndAngle = Math.copySign(Math.round(Math.abs(maxEndAngle / quarter)), maxEndAngle) * quarter;

        final long parkingDuration = (long) (duration * Math.abs(normEndAngle - maxEndAngle) / maxDistance);
        valueAnimator = ValueAnimator.ofFloat((float) currentAngle,
                (float) (((normEndAngle != 0) ? normEndAngle : maxEndAngle) - angle));
        animationLastTime = System.nanoTime();
        valueAnimator.setDuration(duration);
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                animationLastTime = System.nanoTime();
                applySpin(normalizedAngle((Float) animation.getAnimatedValue()));
            }
        });
        valueAnimator.addListener(new Animator.AnimatorListener() {
            boolean isCancelled;
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isCancelled) {
                    if (isInited) {
                        if (normEndAngle == 0 && normEndAngle - angle != currentAngle) {
                            // small parking animation
                            valueAnimator = valueAnimator.clone();
                            valueAnimator.setFloatValues((float) currentAngle, (float) (normEndAngle - angle));
                            valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
                            valueAnimator.setDuration(Math.min(parkingDuration, 150));
                            valueAnimator.start();
                            return;
                        }
                    }
                    runAllocationAnimation();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                isCancelled = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        valueAnimator.start();
        isAnimating = true;
    }

    @Override
    protected boolean isSpinning() {
        return isInited || isAnimating;
    }

    private void runAllocationAnimation() {
        List<NineViewGroup.Box> placements = new ArrayList<>();
        List<FrameLayout> unlocated = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            placements.add(NineViewGroup.Box.getByOrdinal(i));
            unlocated.add(getViewGroup().getFrame(NineViewGroup.Box.getBox(i)));
        }
        NineViewGroup.Box startPlacementBox = NineViewGroup.Box.TOP_LEFT;
        FrameLayout placementView = getViewGroup().getFrame(startPlacementBox);
        double distance = Math.hypot(x0 * 2, y0 * 2);
        FrameLayout movingView = null;
        for (FrameLayout view : unlocated) {
            double currentDistance = getDistanceFromInitial(view, placementView);
            if (Double.compare(currentDistance, distance) < 0) {
                movingView = view;
                distance = currentDistance;
            }
        }
        reset();
        getViewGroup().setSpinOffset(startPlacementBox, movingView);
    }

    @Override
    public void reset() {
        angle = previousAngle = currentAngle = 0;
        isAnimating = false;
        isInited = false;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        for (int i = 0; i < 8; i++) {
            View v = getViewGroup().getSurroundingFrame(i);
            v.setTranslationX(0);
            v.setTranslationY(0);
        }
    }

    @Override
    protected float[] calculateOffset(NineViewGroup.Box box, double distance) {
        float[] offset = new float[2];
        View frame = getViewGroup().getFrame(box);
        double x = getInitialPositionX(frame) - x0;
        double y = getInitialPositionY(frame) - y0;
        double newAngle = normalizedAngle(box.getInitialAngle() + distance + angle);

        double newX, newY;
        if (newAngle <= -topRightAngle && newAngle > topRightAngle) {
            // right line
            newX = (right - x0);
            newY = newX*Math.tan(newAngle) * height / width;
        } else if (newAngle <= topRightAngle && newAngle > topLeftAngle) {
            // top line
            newY = (top - y0);
            newX = newY/Math.tan(newAngle) * width / height;
        } else if (newAngle <= -topLeftAngle && newAngle > -topRightAngle) {
            // bottom line
            newY = (bottom - y0);
            newX = newY/Math.tan(newAngle) * width / height;
        } else {
            // left line
            newX = (left - x0);
            newY = newX*Math.tan(newAngle) * height / width;
        }
        offset[0] = (float) (newX - x);
        offset[1] = (float) (newY - y);
        return offset;
    }

    private int getPerimeter() {
        return (right - left + bottom - top) << 1;
    }

    private void applySpin(double angle) {
        for (int i = 0; i < 8; i++) {
            View v = getViewGroup().getSurroundingFrame(i);
            float[] offset = calculateOffset(getViewGroup().getBox(v), angle);
            v.setTranslationX(offset[0]);
            v.setTranslationY(offset[1]);
        }
        previousAngle = currentAngle;
        currentAngle = angle;
        previousLastTime = lastTime;
        lastTime = System.nanoTime();
        getViewGroup().notifyUpdateDebug();
    }

    private double getDistanceFromInitial(View target, View initialView) {
        double tX = getInitialPositionX(target) + target.getTranslationX();
        double tY = getInitialPositionY(target) + target.getTranslationY();
        return Math.hypot(tX - getInitialPositionX(initialView), tY - getInitialPositionY(initialView));
    }
}
