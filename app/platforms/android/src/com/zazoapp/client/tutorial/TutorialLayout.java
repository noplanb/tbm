package com.zazoapp.client.tutorial;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.zazoapp.client.R;
import com.zazoapp.client.utilities.Convenience;

import java.util.Locale;

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
    private ValueAnimator dimAnimator;

    private RectF dimExcludedRect = new RectF();
    private RectF arrowAnchorRect;
    private int dimExcludedCircleX;
    private int dimExcludedCircleY;
    private int dimExcludedCircleRadius;

    private String hintText;
    private String buttonText;
    private OnTutorialEventListener onTutorialEventListener;

    private ImageView arrowView;
    private TextView tutorialHintView;
    private Button gotItButton;

    public TutorialLayout(Context context) {
        super(context);
        // we set it to software because of clipPath that doesn't work on hardware accelerated canvas before API 18
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public TutorialLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public TutorialLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TutorialLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void dim() {
        dimmed = true;
        arrowView = (ImageView) findViewById(R.id.tutorial_arrow);
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
    }

    private void setUpHintText() {
        tutorialHintView = (TextView) findViewById(R.id.tutorial_hint);
        tutorialHintView.setText(hintText);
        Typeface tf = getTypeface();
        tutorialHintView.setTypeface(tf);
        if (arrowAnchorRect == null) {
            arrowAnchorRect = dimExcludedRect;
        }
        ArrowPosition arrowPosition;
        if (dimExcludedRect.intersects(0, 0, getRight(), getHeight() / 3)) {
            // TOP part, place just below
            LayoutParams p = (LayoutParams) tutorialHintView.getLayoutParams();
            p.setMargins(0, (int) dimExcludedRect.bottom, 0, 0);
            tutorialHintView.setLayoutParams(p);
            tutorialHintView.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            arrowPosition = ArrowPosition.TOP_CENTER;
        } else {
            LayoutParams p = (LayoutParams) tutorialHintView.getLayoutParams();
            p.setMargins(0, 0, 0, (int) (getHeight() - dimExcludedRect.top));
            tutorialHintView.setLayoutParams(p);
            tutorialHintView.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            arrowPosition = ArrowPosition.BOTTOM_CENTER;
        }
        if (dimExcludedRect.right < getWidth() / 2) {
            arrowPosition = arrowPosition.left();
        } else if (dimExcludedRect.left > getWidth() / 2) {
            arrowPosition = arrowPosition.right();
        }
        ArrowPosition.setUpArrowView(arrowView, tutorialHintView, arrowAnchorRect, arrowPosition, getWidth(), getHeight());
    }

    private void setUpGotItButton() {
        gotItButton = (Button) findViewById(R.id.tutorial_btn);
        gotItButton.setTypeface(tutorialHintView.getTypeface());
        if (buttonText == null) {
            gotItButton.setVisibility(INVISIBLE);
        } else {
            gotItButton.setText(buttonText);
        }
    }

    private Typeface getTypeface() {
        boolean isLanguageSupported = Convenience.isLanguageSupported(getContext());
        AssetManager am = getContext().getAssets();
        Typeface tf = null;
        if (isLanguageSupported) {
            try {
                tf = Typeface.createFromAsset(am, "fonts/tutorial-" + Locale.getDefault().getLanguage() + ".ttf");
            } catch (RuntimeException e) {}
        } else {
            tf = Typeface.createFromAsset(am, "fonts/tutorial-en.ttf");
        }
        return tf;
    }

    public void dimExceptForRect(RectF rect) {
        reset();
        dimExcludedRect.set(rect);
        dim();
    }

    public void dimExceptForRect(int left, int top, int right, int bottom) {
        reset();
        dimExcludedRect.set(left, top, right, bottom);
        dim();
    }

    public void dimExceptForCircle(int xCenter, int yCenter, int radius) {
        reset();
        setExcludedCircle(xCenter, yCenter, radius);
        dim();
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
            Path path = new Path();
            path.setFillType(Path.FillType.WINDING);
            path.addCircle(dimExcludedCircleX, dimExcludedCircleY, dimExcludedCircleRadius, Path.Direction.CCW);
            path.addRect(dimExcludedRect, Path.Direction.CCW);
            canvas.clipPath(path, Region.Op.DIFFERENCE);
            canvas.drawRect(0, 0, getRight(), getBottom(), dimPaint);
            canvas.clipRect(0, 0, getRight(), getBottom(), Region.Op.UNION); // restore clip
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
        dimExcludedRect.set(rect);
    }

    public void setExcludedRect(int left, int top, int right, int bottom) {
        dimExcludedRect.set(left, top, right, bottom);
    }

    public void setHintText(String text) {
        hintText = text;
    }

    public int[] getDimExcludedCircle() {
        return new int[] {dimExcludedCircleX, dimExcludedCircleY, dimExcludedCircleRadius};
    }

    public void setExcludedCircle(int... data) {
        dimExcludedCircleX = data[0];
        dimExcludedCircleY = data[1];
        dimExcludedCircleRadius = data[2];
    }

    public void setArrowAnchorRect(RectF rect) {
        arrowAnchorRect = rect;
    }

    public void hideButton() {
        findViewById(R.id.tutorial_btn).setVisibility(INVISIBLE);
    }

    public void setButtonText(String gotItText) {
        buttonText = gotItText;
    }

    private void reset() {
        setVisibility(INVISIBLE);
        findViewById(R.id.tutorial_btn).setVisibility(VISIBLE);
        dimmed = false;
        dimValue = 0;
        dimExcludedCircleX = 0;
        dimExcludedCircleY = 0;
        dimExcludedCircleRadius = 0;
        dimExcludedRect.set(0, 0, 0, 0);
        arrowAnchorRect = null;
    }

    public interface OnTutorialEventListener {
        void onDismiss();
        void onDimmed();
    }

    public enum ArrowPosition {
        TOP_LEFT(R.drawable.arrow_yellow_top_left, Gravity.BOTTOM | Gravity.LEFT) {
            @Override
            void locate(View baseView, RectF anchorRect, int width, int height, LayoutParams arrowParams) {
                int arrowLeftMargin = (int) anchorRect.right;
                int arrowBottomMargin = height - (int) anchorRect.bottom - baseView.getPaddingTop();
                arrowParams.setMargins(arrowLeftMargin, 0, 0, arrowBottomMargin);
            }
        },
        TOP_CENTER(R.drawable.arrow_yellow_top_center, Gravity.BOTTOM | Gravity.LEFT) {
            @Override
            void locate(View baseView, RectF anchorRect, int width, int height, LayoutParams arrowParams) {
                int arrowLeftMargin = (int) anchorRect.right;
                int arrowBottomMargin = height - (int) anchorRect.bottom - baseView.getPaddingTop();
                arrowParams.setMargins(arrowLeftMargin, 0, 0, arrowBottomMargin);
            }
        },
        TOP_RIGHT(R.drawable.arrow_yellow_top_right, Gravity.BOTTOM | Gravity.RIGHT) {
            @Override
            void locate(View baseView, RectF anchorRect, int width, int height, LayoutParams arrowParams) {
                int arrowRightMargin = (int) (width - anchorRect.left);
                int arrowBottomMargin = height - (int) anchorRect.bottom - baseView.getPaddingTop();
                arrowParams.setMargins(0, 0, arrowRightMargin, arrowBottomMargin);
            }
        },
        BOTTOM_LEFT(R.drawable.arrow_yellow_bottom_left, Gravity.TOP | Gravity.LEFT) {
            @Override
            void locate(View baseView, RectF anchorRect, int width, int height, LayoutParams arrowParams) {
                int arrowLeftMargin = (int) anchorRect.right;
                int arrowTopMargin = (int) anchorRect.top - baseView.getPaddingBottom();
                arrowParams.setMargins(arrowLeftMargin, arrowTopMargin, 0, 0);
            }
        },
        BOTTOM_CENTER(R.drawable.arrow_yellow_bottom_center, Gravity.TOP | Gravity.LEFT) {
            @Override
            void locate(View baseView, RectF anchorRect, int width, int height, LayoutParams arrowParams) {
                int arrowLeftMargin = (int) anchorRect.right;
                int arrowTopMargin = (int) anchorRect.top - baseView.getPaddingBottom();
                arrowParams.setMargins(arrowLeftMargin, arrowTopMargin, 0, 0);
            }
        },
        BOTTOM_RIGHT(R.drawable.arrow_yellow_bottom_right, Gravity.TOP | Gravity.RIGHT) {
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
