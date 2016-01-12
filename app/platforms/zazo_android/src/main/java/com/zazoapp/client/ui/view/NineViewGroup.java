/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zazoapp.client.ui.view;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.zazoapp.client.R;
import com.zazoapp.client.ui.ViewGroupGestureRecognizer;
import com.zazoapp.client.ui.ZazoManagerProvider;

import java.util.ArrayList;

public class NineViewGroup extends ViewGroup {
    // ---------
    // Constants
    // ---------
    private static final String TAG = NineViewGroup.class.getSimpleName();

    private static final float ASPECT = 240F / 320F;

    //-----------------------------
    // Abstract callback interfaces
    //-----------------------------
    private GestureCallbacks gestureCallbacks;
    private SpinStrategy spinStrategy;
    private ViewGroupGestureRecognizer vgGestureRecognizer;
    private LayoutCompleteListener layoutCompleteListener;
    private SpinChangedListener spinChangedListener;
    private int spinOffset;
    private boolean spinEnabled;

    public interface GestureCallbacks {
        boolean onSurroundingClick(View view, int position);

        boolean onSurroundingStartLongpress(View view, int position);

        boolean onCenterClick(View view);

        boolean onCenterStartLongpress(View view);

        boolean onEndLongpress();

        boolean onCancelLongpress(int reason);

        boolean onSurroundingMovingAway(View view, int position);

        boolean onSurroundingMovingIn(View view, int position);

        void notifyUpdateDebug();
    }

    public interface SpinChangedListener {
        void onSpinChanged(int newSpinOffset);
    }

    public interface LayoutCompleteListener {
        void onLayoutComplete();
    }

    public enum Box {
        TOP_LEFT(7, 0, -Math.PI*3/4),
        TOP_CENTER(6, 1, -Math.PI/2),
        TOP_RIGHT(4, 2, -Math.PI/4),
        CENTER_LEFT(5, 7, -Math.PI),
        CENTER(8, 8, 0),
        CENTER_RIGHT(0, 3, 0),
        BOTTOM_LEFT(3, 6, Math.PI*3/4),
        BOTTOM_CENTER(1, 5, Math.PI/2),
        BOTTOM_RIGHT(2, 4, Math.PI/4);

        private int pos;
        private int spinOrder;
        private double initialAngle;
        Box(int i, int order, double angle) {
            pos = i;
            spinOrder = order;
            initialAngle = angle;
        }

        int getPos() {
            return pos;
        }

        public int getSpinOrder() {
            return spinOrder;
        }

        public static Box getBox(int position) {
            for (Box box : values()) {
                if (box.pos == position) {
                    return box;
                }
            }
            return null;
        }

        public static Box getByOrdinal(int ordinal) {
            for (Box box : values()) {
                if (box.spinOrder == ordinal) {
                    return box;
                }
            }
            return null;
        }

        public Box getNext() {
            if (this == CENTER) {
                return this;
            }
            return getByOrdinal((spinOrder + 1) % 8);
        }

        public Box getPrevious() {
            if (this == CENTER) {
                return this;
            }
            return getByOrdinal((spinOrder + 7) % 8);
        }

        public Box getWithOffset(int offset) {
            if (this == CENTER) {
                return this;
            }
            return getByOrdinal((spinOrder + offset + 8) % 8);
        }

        public double getInitialAngle() {
            return initialAngle;
        }
    }
    //-------------
    // Constructors
    //-------------
    public NineViewGroup(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public NineViewGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NineViewGroup(Context context) {
        this(context, null);
    }

    private void init() {
        addElementViews();
        if (isInEditMode())
            return;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        layoutElementViews();
        /*Log.i(TAG, "onLayout: " + changed + " " + l + " " + t + " " + r + " " + b);*/
        if (changed) {
            if (layoutCompleteListener != null)
                layoutCompleteListener.onLayoutComplete();
        }
    }

    //----------------------
    // Callback registration
    //----------------------
    public GestureCallbacks getGestureListener() {
        return gestureCallbacks;
    }

    public void setGestureListener(GestureCallbacks gl) {
        this.gestureCallbacks = gl;
    }

    public SpinStrategy getSpinStrategy() {
        return spinStrategy;
    }

    public void setSpinStrategy(SpinStrategy spinStrategy) {
        this.spinStrategy = spinStrategy;
    }

    public void setChildLayoutCompleteListener(LayoutCompleteListener childLayoutCompleteListener) {
        this.layoutCompleteListener = childLayoutCompleteListener;
    }

    public void setSpinChangedListener(SpinChangedListener listener) {
        this.spinChangedListener = listener;
    }

    //-------
    // Layout
    //-------
    private void addElementViews() {
        for (int i = 0; i < 9; i++) {
            FrameLayout fl = new FrameLayout(getContext());
            fl.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            fl.setClipChildren(false);
            fl.setClipToPadding(false);
            fl.setId(i);
            fl.setTag(R.id.box_id, i);
            addView(fl, i);
        }
    }

    private void layoutElementViews() {
        int x;
        int y;
        int i = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                x = (int) (gutterLeft() + col * (elementWidth() + paddingPx()));
                y = (int) (gutterTop() + row * (elementHeight() + paddingPx()));
                FrameLayout fl = (FrameLayout) getChildAt(i);
                fl.measure(MeasureSpec.EXACTLY | elementWidth(), MeasureSpec.EXACTLY | elementHeight());
                fl.layout(x, y, x + elementWidth(), y + elementHeight());
                i++;
            }
        }
    }

    // Layout helpers
    private Pair<Float, Float> elementSize() {
        float width;
        float height;
        if (isHeightConstrained()) {
            height = (getHeight() - 2 * (marginPx() + paddingPx())) / 3;
            width = ASPECT * height;
        } else {
            width = (getWidth() - 2 * (marginPx() + paddingPx())) / 3;
            height = (getHeight() - 2 * (marginPx() + paddingPx())) / 3;
        }
        return new Pair<Float, Float>(width, height);
    }

    private int elementWidth() {
        return Math.round(elementSize().first);
    }

    private int elementHeight() {
        return Math.round(elementSize().second);
    }

    private boolean isHeightConstrained() {
        return !isWidthConstrained();
    }

    private boolean isWidthConstrained() {
        return (float) getWidth() / (float) getHeight() < ASPECT;
    }

    private Integer gutterTop() {
        return (int) marginPx();
    }

    private Integer gutterLeft() {
        if (isWidthConstrained())
            return (int) marginPx();
        else
            return (int) ((getWidth() - 3 * elementWidth() - 2 * paddingPx()) / 2);
    }

    private float paddingPx() {
        return getContext().getResources().getDimension(R.dimen.nine_view_padding);
    }

    private float marginPx() {
        return getContext().getResources().getDimension(R.dimen.nine_view_marging);
    }

    //---------------
    // Public getters
    //---------------
    public FrameLayout getCenterFrame() {
        return (FrameLayout) getChildAt(4);
    }

    /**
     * Returns view by desired populating order:
     * <pre>
     * 7 6 4
     * 5 8 0
     * 3 1 2
     * </pre>
     * <p/>
     * By converting from this internal index order
     * <pre>
     * 0 1 2
     * 3 4 5
     * 6 7 8
     * </pre>
     *
     * @return corresponding view with taking spinOffset into account
     */
    public FrameLayout getSurroundingFrame(int position) {
        return (FrameLayout) getChildAt(Box.getBox(position).getWithOffset(spinOffset).ordinal());
    }

    /**
     * @param box
     * @return corresponding view (spinOffset isn't taken into account)
     */
    public FrameLayout getFrame(Box box) {
        return (FrameLayout) getChildAt(box.ordinal());
    }

    /**
     *
     * @param view
     * @return return corresponding box
     */
    public Box getBox(View view) {
        return Box.values()[view.getId()];
    }

    public void setSpinOffset(Box box, FrameLayout view) {
        int curPos = getBox(view).getSpinOrder();
        int newPos = box.getSpinOrder();
        setSpinOffset((8 - curPos + newPos) % 8);
    }

    /**
     *
     * @param offset value in between the range [0; 7]
     */
    public void setSpinOffset(int offset) {
        int tempSpin = spinOffset;
        spinOffset = (spinOffset + offset) % 8;
        if (spinOffset != tempSpin) {
            ArrayList<View> views = getNineViews();
            removeViews(5, 4);
            removeViews(0, 4);
            for (int i = 0; i < 9; i++) {
                if (i != 4) {
                    int oldId = Box.values()[i].getWithOffset(-offset).ordinal();
                    views.get(oldId).setId(i);
                    addView(views.get(oldId), i);
                }
            }
            if (spinChangedListener != null) {
                spinChangedListener.onSpinChanged(spinOffset);
            }
        }
    }

    public int getSpinOffset() {
        return spinOffset;
    }

    public void enableSpin(boolean enable) {
        spinEnabled = enable;
    }

    public boolean isSpinEnabled() {
        return spinEnabled;
    }

    public ArrayList<View> getNineViews() {
        ArrayList<View> r = new ArrayList<View>();
        for (int i = 0; i < getChildCount(); i++) {
            r.add(getChildAt(i));
        }
        return r;
    }

    @SuppressWarnings("ResourceType")
    boolean isCenterView(View v) {
        return v != null && v.getId() == 4;
    }

    int positionOfView(View v) {
        if (isCenterView(v))
            return -1;

        int pov = Box.values()[(Integer) v.getTag(R.id.box_id)].getPos();
        if (pov > 7)
            throw new RuntimeException(TAG + " positionWithIndex: got out of bounds possition for view: " + pov);

        return pov;
    }

    //-------------------------------
    // NineViewGroupGestureRecognizer
    //-------------------------------
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return vgGestureRecognizer.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        vgGestureRecognizer.dispatchTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return vgGestureRecognizer.onTouchEvent(event);
    }

    public void setGestureRecognizer(ZazoManagerProvider provider) {
        vgGestureRecognizer = new NineViewGroupGestureRecognizer((Activity) getContext(), this, getNineViews(), provider);
        vgGestureRecognizer.enable();
    }

    public void notifyUpdateDebug() {
        if (gestureCallbacks != null) {
            gestureCallbacks.notifyUpdateDebug();
        }
    }

    protected boolean handleAbort(View v, int reason) {
        if (gestureCallbacks == null)
            return false;
        return gestureCallbacks.onCancelLongpress(reason);
    }

    public static abstract class SpinStrategy {
        protected String TAG = getClass().getSimpleName();
        private NineViewGroup viewGroup;
        public SpinStrategy(NineViewGroup viewGroup) {
            this.viewGroup = viewGroup;
        }

        protected NineViewGroup getViewGroup() {
            return viewGroup;
        }

        abstract public double calculateAngle();
        abstract protected float[] calculateOffset(Box box, double distance);
        abstract protected void spin(double startX, double startY, double offsetX, double offsetY);
        abstract protected void stopSpin(double startX, double startY);
        abstract protected void cancelSpin();
        abstract protected void initSpin(double startX, double startY, double offsetX, double offsetY);
        abstract protected void finishSpin(double startX, double startY, double offsetX, double offsetY);
        abstract protected boolean isSpinning();

        public static double getInitialPositionX(View v) {
            return v.getLeft() + v.getWidth() / 2;
        }

        public static double getInitialPositionY(View v) {
            return v.getTop() + v.getHeight() / 2;
        }

        public abstract void reset();

        public static double normalizedAngle(double angle) {
            while (true) {
                if (angle >= Math.PI) {
                    angle -= 2 * Math.PI;
                } else if (angle < -Math.PI) {
                    angle += 2 * Math.PI;
                } else {
                    break;
                }
            }
            return angle;
        }
    }
}