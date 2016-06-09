package com.zazoapp.client.tutorial;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.zazoapp.client.R;
import com.zazoapp.client.utilities.Convenience;

import java.lang.ref.WeakReference;

/**
 * Created by skamenkovych@codeminders.com on 5/6/2015.
 */
public class TutorialLayout extends FrameLayout {
    private static final String TAG = TutorialLayout.class.getSimpleName();
    private static final int MAX_DIM = (int) (0.8f * 255);
    private static final int DURATION = 300;
    private boolean dimmed;
    private int dimValue;
    private Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Paint additionalBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private ValueAnimator dimAnimator;
    private Bitmap helpViewBitmap;
    private Bitmap additionalViewBitmap;

    private RectF dimExcludedRect = new RectF();
    private RectF backgroundViewRect = new RectF();
    private RectF arrowAnchorRect;
    private RectF tutorialRect;
    private RectF additionalViewRect;

    private String hintText;
    private String buttonText;
    private OnTutorialEventListener onTutorialEventListener;

    @InjectView(R.id.tutorial_arrow) ImageView arrowView;
    @InjectView(R.id.tutorial_hint) TextView tutorialHintView;
    @InjectView(R.id.tutorial_btn) Button gotItButton;

    private WeakReference<View> helpViewRef;
    private WeakReference<View> additionalViewRef;
    private WeakReference<View> backgroundViewRef;
    private boolean shouldUpdateViews;

    public TutorialLayout(Context context) {
        this(context, null);
    }

    public TutorialLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TutorialLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TutorialLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.tutorial_layout, this, true);
        ButterKnife.inject(this);
        // we set it to software because of clipPath that doesn't work on hardware accelerated canvas before API 18
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void dim() {
        dimmed = true;
        shiftRectToBackground(dimExcludedRect);
        shiftRectToBackground(additionalViewRect);
        shiftRectToBackground(arrowAnchorRect);
        shiftRectToBackground(backgroundViewRect);
        setUpHintText();
        setUpGotItButton();
        setVisibility(VISIBLE);
        dimAnimator = ValueAnimator.ofInt(0, MAX_DIM);
        dimPaint.setColor(getContext().getResources().getColor(R.color.light_grey_new));
        dimPaint.setStyle(Paint.Style.FILL);
        dimAnimator.setDuration(DURATION);
        dimAnimator.setInterpolator(new DecelerateInterpolator());
        dimAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                dimValue = (int) animation.getAnimatedValue();
                setAlpha(dimValue / (float) MAX_DIM);
                invalidate();
            }
        });
        dimAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (onTutorialEventListener != null) {
                    onTutorialEventListener.onDimmed();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        dimAnimator.start();
        if (getHeight() == 0) {
            shouldUpdateViews = true;
        }
    }

    private void setUpHintText() {
        tutorialHintView.setText(hintText);
        Typeface tf = Convenience.getTutorialTypeface(getContext());
        tutorialHintView.setTypeface(tf);
        if (arrowAnchorRect == null) {
            bitmapPaint.setColorFilter(null);
        } else {
            bitmapPaint.setColorFilter(new PorterDuffColorFilter(Color.parseColor("#A8A8A8"), PorterDuff.Mode.MULTIPLY));
        }
        if (backgroundViewRect.height() == 0) {
            setBackgroundView(this);
        }
        int bWidth = (int) backgroundViewRect.width();
        int bHeight = (int) backgroundViewRect.height();
        RectF topThird = new RectF(backgroundViewRect);
        topThird.bottom = backgroundViewRect.top + bHeight / 3;
        ArrowPosition arrowPosition;
        if (RectF.intersects(dimExcludedRect, topThird)) {
            // TOP part, place just below
            LayoutParams p = (LayoutParams) tutorialHintView.getLayoutParams();
            p.setMargins(0, (int) dimExcludedRect.bottom, 0, 0);
            tutorialHintView.setLayoutParams(p);
            tutorialHintView.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            arrowPosition = ArrowPosition.TOP_CENTER;
        } else {
            LayoutParams p = (LayoutParams) tutorialHintView.getLayoutParams();
            p.setMargins(0, 0, 0, (int) (backgroundViewRect.bottom - dimExcludedRect.top));
            tutorialHintView.setLayoutParams(p);
            tutorialHintView.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            arrowPosition = ArrowPosition.BOTTOM_CENTER;
        }
        if (dimExcludedRect.right < bWidth / 2) {
            arrowPosition = arrowPosition.left();
        } else if (dimExcludedRect.left > bWidth / 2) {
            arrowPosition = arrowPosition.right();
        }
        ArrowPosition.setUpArrowView(arrowView, tutorialHintView, getArrowAnchorRect(), arrowPosition, getMeasuredWidth(), getMeasuredHeight());
    }

    private void setUpGotItButton() {
        gotItButton.setTypeface(tutorialHintView.getTypeface());
        if (buttonText == null) {
            gotItButton.setVisibility(INVISIBLE);
        } else {
            gotItButton.setText(buttonText);
        }
    }

    private RectF getArrowAnchorRect() {
        return (arrowAnchorRect == null) ? dimExcludedRect : arrowAnchorRect;
    }

    /**
     * Helper method to call dim() except for specified view
     */
    public void dimExceptForView(View view, View backgroundView) {
        reset();
        setBackgroundView(backgroundView);
        setHelpView(view);
        dim();
    }

    public void setHelpView(View view) {
        if (getViewFromRef(helpViewRef) != view) {
            helpViewRef = new WeakReference<>(view);
        }
        setExcludedRect(Convenience.getViewRect(view));
        view.buildDrawingCache();
        helpViewBitmap = Bitmap.createBitmap(view.getDrawingCache());
        view.destroyDrawingCache();
    }

    public void setAdditionalView(@Nullable View view) {
        if (view != null) {
            if (getViewFromRef(additionalViewRef) != view) {
                additionalViewRef = new WeakReference<>(view);
            }
            additionalViewRect = Convenience.getViewRect(view);
            shiftRectVertically(getTutorialRect(), additionalViewRect);
            view.buildDrawingCache();
            Bitmap cache = view.getDrawingCache();
            if (cache != null) {
                additionalViewBitmap = Bitmap.createBitmap(cache);
            }
            view.destroyDrawingCache();
        } else {
            additionalViewBitmap = null;
            additionalViewRef = null;
        }
    }

    private void shiftRectVertically(RectF src, RectF dst) {
        dst.top -= src.top;
        dst.bottom -= src.top;
    }

    private void shiftRectToBackground(RectF rect) {
        if (rect == null)
            return;
        float offset = getTutorialRect().left - backgroundViewRect.left;
        rect.left += offset;
        rect.right += offset;
    }

    public void dismiss() {
        if (dimAnimator != null && dimmed) {
            dimAnimator.cancel();
            dimAnimator.setIntValues(dimValue, 0);
            dimAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    reset();
                    dimAnimator.removeAllUpdateListeners();
                    dimAnimator.removeAllListeners();
                    if (onTutorialEventListener != null) {
                        onTutorialEventListener.onDismiss();
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            dimAnimator.start();
        }
    }

    public void clear() {
        reset();
        postInvalidate();
    }

    public void dismissSoftly(final OnTutorialEventListener customListener) {
        if (customListener != null && dimAnimator != null && dimmed) {
            dimAnimator.cancel();
            dimAnimator.setIntValues(dimValue, 0);
            dimAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    reset();
                    dimAnimator.removeAllUpdateListeners();
                    dimAnimator.removeAllListeners();
                    customListener.onDismiss();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            dimAnimator.start();
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        dimPaint.setAlpha(dimValue);
        if (dimmed) {
            canvas.drawRect(0, 0, getRight(), getBottom(), dimPaint);
            if (helpViewBitmap != null) {
                canvas.drawBitmap(helpViewBitmap, dimExcludedRect.left, dimExcludedRect.top, bitmapPaint);
            }
            if (additionalViewBitmap != null && additionalViewRect != null) {
                canvas.drawBitmap(additionalViewBitmap, additionalViewRect.left, additionalViewRect.top, additionalBitmapPaint);
            }
        }
        super.dispatchDraw(canvas);
    }

    public void setOnTutorialEventListener(OnTutorialEventListener listener) {
        onTutorialEventListener = listener;
    }

    public RectF getExcludedRect() {
        return dimExcludedRect;
    }

    public void setExcludedRect(RectF rect) {
        setExcludedRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    public void setExcludedRect(float left, float top, float right, float bottom) {
        dimExcludedRect.set(left, top, right, bottom);
        shiftRectVertically(getTutorialRect(), dimExcludedRect);
    }

    /**
     * Set location rect of View on which tutorial will be shown
     * @param view
     */
    public void setBackgroundView(View view) {
        if (getViewFromRef(backgroundViewRef) != view) {
            backgroundViewRef = new WeakReference<>(view);
        }
        RectF rect = Convenience.getViewRect(view);
        backgroundViewRect.set(rect);
        shiftRectVertically(getTutorialRect(), backgroundViewRect);
    }

    public void setHintText(String text) {
        hintText = text;
    }

    public void setArrowAnchorRect(RectF rect) {
        arrowAnchorRect = rect;
    }

    public void hideButton() {
        gotItButton.setVisibility(INVISIBLE);
    }

    public void setButtonText(String gotItText) {
        buttonText = gotItText;
    }

    public void setIcon(int id) {
        ImageView icon = (ImageView) findViewById(R.id.tutorial_icon);
        if (id > 0) {
            icon.setImageResource(id);
        } else {
            icon.setImageBitmap(null);
        }
    }

    private RectF getTutorialRect() {
        if (tutorialRect == null) {
            tutorialRect = Convenience.getViewRect(this);
        }
        return tutorialRect;
    }

    private void reset() {
        setVisibility(INVISIBLE);
        gotItButton.setVisibility(VISIBLE);
        setIcon(0);
        dimmed = false;
        dimValue = 0;
        dimExcludedRect.set(0, 0, 0, 0);
        backgroundViewRect.set(0, 0, 0, 0);
        arrowAnchorRect = null;
        tutorialRect = null;
        helpViewBitmap = null;
        additionalViewBitmap = null;
        additionalViewRef = null;
        backgroundViewRef = null;
        helpViewRef = null;
    }

    public void updateViews() {
        if (!dimmed) {
            return;
        }
        if (getMeasuredHeight() != getHeight()) {
            shouldUpdateViews = true;
            return;
        }
        View backgroundView = getViewFromRef(backgroundViewRef);
        if (backgroundView != null) {
            setBackgroundView(backgroundView);
        }
        View helpView = getViewFromRef(helpViewRef);
        if (helpView != null) {
            setHelpView(helpView);
        }
        View additionalView = getViewFromRef(additionalViewRef);
        if (additionalView != null) {
            setAdditionalView(additionalView);
        }
        shiftRectToBackground(dimExcludedRect);
        shiftRectToBackground(additionalViewRect);
        shiftRectToBackground(arrowAnchorRect);
        shiftRectToBackground(backgroundViewRect);
        setUpHintText();
    }

    public View getHelpView() {
        return getViewFromRef(helpViewRef);
    }

    private View getViewFromRef(WeakReference<View> ref) {
        return (ref != null) ? ref.get() : null;
    }

    public interface OnTutorialEventListener {
        void onDismiss();
        void onDimmed();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            tutorialRect = null;
            if (shouldUpdateViews) {
                updateViews();
            }
        }
    }

    public enum ArrowPosition {
        TOP_LEFT(R.drawable.tutorial_arrow_top_left, Gravity.BOTTOM | Gravity.LEFT) {
            @Override
            void locate(View baseView, RectF anchorRect, int width, int height, LayoutParams arrowParams) {
                int arrowLeftMargin = (int) anchorRect.right;
                int arrowBottomMargin = height - (int) anchorRect.bottom - baseView.getPaddingTop();
                arrowParams.setMargins(arrowLeftMargin, 0, 0, arrowBottomMargin);
            }
        },
        TOP_CENTER(R.drawable.tutorial_arrow_top_center, Gravity.BOTTOM | Gravity.LEFT) {
            @Override
            void locate(View baseView, RectF anchorRect, int width, int height, LayoutParams arrowParams) {
                int arrowLeftMargin = (int) anchorRect.right;
                int arrowBottomMargin = height - (int) anchorRect.bottom - baseView.getPaddingTop();
                arrowParams.setMargins(arrowLeftMargin, 0, 0, arrowBottomMargin);
            }
        },
        TOP_RIGHT(R.drawable.tutorial_arrow_top_right, Gravity.BOTTOM | Gravity.RIGHT) {
            @Override
            void locate(View baseView, RectF anchorRect, int width, int height, LayoutParams arrowParams) {
                int arrowRightMargin = (int) (width - anchorRect.left);
                int arrowBottomMargin = height - (int) anchorRect.bottom - baseView.getPaddingTop();
                arrowParams.setMargins(0, 0, arrowRightMargin, arrowBottomMargin);
            }
        },
        BOTTOM_LEFT(R.drawable.tutorial_arrow_bottom_left, Gravity.TOP | Gravity.LEFT) {
            @Override
            void locate(View baseView, RectF anchorRect, int width, int height, LayoutParams arrowParams) {
                int arrowLeftMargin = (int) anchorRect.right;
                int arrowTopMargin = (int) anchorRect.top - baseView.getPaddingBottom();
                arrowParams.setMargins(arrowLeftMargin, arrowTopMargin, 0, 0);
            }
        },
        BOTTOM_CENTER(R.drawable.tutorial_arrow_bottom_center, Gravity.TOP | Gravity.LEFT) {
            @Override
            void locate(View baseView, RectF anchorRect, int width, int height, LayoutParams arrowParams) {
                int arrowLeftMargin = (int) anchorRect.right;
                int arrowTopMargin = (int) anchorRect.top - baseView.getPaddingBottom();
                arrowParams.setMargins(arrowLeftMargin, arrowTopMargin, 0, 0);
            }
        },
        BOTTOM_RIGHT(R.drawable.tutorial_arrow_bottom_right, Gravity.TOP | Gravity.RIGHT) {
            @Override
            void locate(View baseView, RectF anchorRect, int width, int height, LayoutParams arrowParams) {
                int arrowRightMargin = (int) (width - anchorRect.left);
                int arrowTopMargin = (int) anchorRect.top - baseView.getPaddingBottom();
                arrowParams.setMargins(0, arrowTopMargin, arrowRightMargin, 0);
            }
        };

        private final int image;
        private final int gravity;

        ArrowPosition(int image, int gravity) {
            this.image = image;
            this.gravity = gravity;
        }

        public static void setUpArrowView(ImageView arrowView, View baseView, RectF anchorRect, ArrowPosition position, int width, int height) {
            LayoutParams arrowParams = (LayoutParams) arrowView.getLayoutParams();
            arrowView.setImageResource(position.image);
            position.locate(baseView, anchorRect, width, height, arrowParams);
            arrowParams.gravity = position.gravity;
            arrowView.setLayoutParams(arrowParams);
        }

        abstract void locate(View baseView, RectF anchorRect, int width, int height, LayoutParams arrowParams);

        public ArrowPosition left() {
            return values()[ordinal() / 3 * 3];
        }

        public ArrowPosition center() {
            return values()[ordinal() / 3 * 3 + 1];
        }

        public ArrowPosition right() {
            return values()[ordinal() / 3 * 3 + 2];
        }

        public ArrowPosition top() {
            return values()[ordinal() % 3];
        }

        public ArrowPosition bottom() {
            return values()[ordinal() % 3 + 3];
        }

    }
}
