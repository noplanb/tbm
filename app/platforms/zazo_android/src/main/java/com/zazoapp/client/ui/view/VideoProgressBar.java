package com.zazoapp.client.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.zazoapp.client.BuildConfig;
import com.zazoapp.client.R;
import com.zazoapp.client.ui.animations.VideoProgressBarAnimation;
import com.zazoapp.client.utilities.Convenience;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 1/19/2016.
 */
public class VideoProgressBar extends FrameLayout {
    private static final String TAG = VideoProgressBar.class.getSimpleName();

    @InjectView(R.id.slider_view) AutoResizeTextView sliderView;
    private float progress = 0.1f;
    private float secondaryProgress = 1f;
    private float progressLineHeight;

    private Paint primaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Paint secondaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Paint testPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    {
        testPaint.setColor(Color.GREEN);
        testPaint.setStyle(Paint.Style.STROKE);
        testPaint.setStrokeWidth(1);
    }
    private ValueAnimator progressAnimator;
    private Scheme scheme = new VideoProgressBar.Scheme.SchemeBuilder().build();
    private int current;
    private int barPadding;
    private int layoutPadding;
    private int dotSize;
    private HashMap<Integer, Bitmap> icons;

    public VideoProgressBar(Context context) {
        this(context, null);
    }

    public VideoProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.playback_slider, this);
        ButterKnife.inject(this);
        progressLineHeight = Convenience.dpToPx(getContext(), 3f);
        barPadding = Convenience.dpToPx(getContext(), 2);
        dotSize = Convenience.dpToPx(getContext(), 24);
        layoutPadding = getContext().getResources().getDimensionPixelSize(R.dimen.abc_action_bar_content_inset_material);
        iconPaint.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
    }

    public void setScheme(Scheme scheme) {
        if (scheme != null) {
            for (int i = 0; i < scheme.getCount(); i++) {
                Scheme.Element element = scheme.getElementAt(i);
                if (element == Scheme.Element.POINT) {
                    Integer id = (Integer) scheme.getDataAt(i);
                    if (icons == null) {
                        icons = new HashMap<>();
                    }
                    if (!icons.containsKey(id)) {
                        icons.put(id, BitmapFactory.decodeResource(getResources(), id));
                    }
                }
            }
            scheme.recalculate(this);
            this.scheme = scheme;
        }
    }

    public void setCurrent(int current, boolean animate) {
        final String currentText = (current > 0) ? String.valueOf(current) : "";
        if (!currentText.equals(sliderView.getText())) {
            if (animate) {
                VideoProgressBarAnimation.animateValueChange(sliderView, new Runnable() {
                    @Override
                    public void run() {
                        sliderView.setText(currentText);
                    }
                }, null);
            } else {
                sliderView.setText(currentText);
            }
        }
        this.current = current;
    }

    public void setProgress(@FloatRange(from = 0f, to = 1f) float progress) {
        this.progress = progress;
        setSliderPosition();
        invalidate();
    }

    public @FloatRange(from = 0f, to = 1f) float getProgress() {
        return progress;
    }

    public void animateProgress(int item, @FloatRange(from = 0f, to = 1f) float startRelativeProgress, int duration) {
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
        if (scheme.getCount() < 1) {
            return;
        }
        if (item < 0) {
            ItemInfo info = getCurrentItemInfo();
            if (info != null) {
                startRelativeProgress = info.currentProgress;
                item = info.position;
            } else {
                startRelativeProgress = 0;
                item = 0;
            }
        }
        if (scheme.getElementAt(item) == Scheme.Element.BAR) {
            RectF rect = scheme.getRectAtPosition(item);
            float width = scheme.getRectAtPosition(scheme.getCount() - 1).right - scheme.getRectAtPosition(0).left;
            float endProgress = rect.right / width;
            float startProgress = (rect.left + startRelativeProgress * rect.width()) / width;
            progressAnimator = ValueAnimator.ofFloat(startProgress, endProgress);
            progressAnimator.setInterpolator(new LinearInterpolator());
            progressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    setProgress((float) animation.getAnimatedValue());
                }
            });
            progressAnimator.setDuration(Math.max(duration, 0));
            progressAnimator.start();
        }
    }

    public ItemInfo getCurrentItemInfo() {
        Scheme scheme = this.scheme;
        if (scheme.getCount() < 1) {
            return null;
        }
        float width = scheme.getRectAtPosition(scheme.getCount() - 1).right - scheme.getRectAtPosition(0).left;
        if (width <= 0) {
            return ItemInfo.EMPTY; // shouldn't normally happen
        }
        float pointer = progress * width + scheme.getRectAtPosition(0).left;
        int i = 0;
        for (; i < scheme.getCount() - 1; i++) {
            if (scheme.getRectAtPosition(i).left <= pointer && scheme.getRectAtPosition(i + 1).left > pointer) {
                ItemInfo info = new ItemInfo();
                info.position = i;
                info.currentProgress = (pointer - scheme.getRectAtPosition(i).left) / scheme.getRectAtPosition(i).width();
                info.progress = pointer / scheme.getRectAtPosition(i).width();
                return info;
            }
        }
        ItemInfo info = new ItemInfo();
        info.position = i;
        info.currentProgress = (pointer - scheme.getRectAtPosition(i).left) / scheme.getRectAtPosition(i).width();
        info.progress = pointer / scheme.getRectAtPosition(i).width();
        return info;
    }

    private void setSliderPosition() {
        if (scheme.getCount() < 1) {
            sliderView.setAlpha(0);
            return;
        }

        ItemInfo info = getCurrentItemInfo();
        if (info == null || info == ItemInfo.EMPTY) {
            sliderView.setAlpha(0);
            return;
        }
        if (scheme.getElementAt(info.position) == Scheme.Element.BAR) {
            sliderView.setAlpha(1f);
            RectF rect = scheme.getRectAtPosition(info.position);
            sliderView.setX(rect.left + rect.width() * info.currentProgress - sliderView.getWidth() / 2);
        } else {
            sliderView.setAlpha(0f);
        }
        //sliderView.setX((getWidth() - (layoutPadding + barPadding) * 2) * progress - sliderView.getWidth() / 2 + layoutPadding);
    }

    public void setSecondaryProgress(@FloatRange(from = 0f, to = 1f) float progress) {
        this.secondaryProgress = progress;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        recalculatePaints(w, h);
        scheme.recalculate(this);
        setSliderPosition();
    }

    private void recalculatePaints(int w, int h) {
        primaryPaint.setShader(new LinearGradient(0, 0, 0, progressLineHeight,
                new int[]{
                        Color.parseColor("#eeeeee"),
                        Color.WHITE,
                        Color.WHITE,
                        Color.parseColor("#eeeeee")
                }, new float[]{0f, 0.1f, 0.8f, 0.9f}, Shader.TileMode.CLAMP));
        secondaryPaint.setShader(new LinearGradient(0, 0, 0, progressLineHeight,
                new int[]{
                        Color.parseColor("#8aeeeeee"),
                        Color.parseColor("#8affffff"),
                        Color.parseColor("#8affffff"),
                        Color.parseColor("#8aeeeeee")
                }, new float[]{0f, 0.1f, 0.8f, 0.9f}, Shader.TileMode.CLAMP));
    }

    private RectF drawRoundRect = new RectF();
    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (isInEditMode()) {
            return;
        }
        int barsCount = scheme.getBarCount();
        if (barsCount < 1) {
            return;
        }
        int cY = getHeight() / 2;
        canvas.save();
        canvas.translate(0, cY - progressLineHeight / 2f);
        int count = scheme.getCount();
        ItemInfo info = getCurrentItemInfo();
        float curProgress = info.currentProgress;
        float scaledSecProgress = count * secondaryProgress;
        float curSecProgress = scaledSecProgress - (int)  scaledSecProgress;

        float radius = Convenience.dpToPx(getContext(), 1f);

        for (int i = 0; i < count; i++) {
            Scheme.Element item = scheme.getElementAt(i);
            RectF rect = scheme.getRectAtPosition(i);
            if (BuildConfig.DEBUG) {
                canvas.drawRect(rect, testPaint);
            }
            if (item == Scheme.Element.BAR) {
                drawRoundRect.set(rect);
                if (i < Math.floor(scaledSecProgress)) {
                    canvas.drawRoundRect(rect, radius, radius, secondaryPaint);
                } else if (i == Math.floor(scaledSecProgress)) {
                    drawRoundRect.right = rect.left + rect.width() * curSecProgress;
                    canvas.drawRoundRect(drawRoundRect, radius, radius, secondaryPaint);
                }
                if (i < info.position) {
                    canvas.drawRoundRect(rect, radius, radius, primaryPaint);
                } else if (i == info.position) {
                    drawRoundRect.right = rect.left + rect.width() * curProgress;
                    canvas.drawRoundRect(drawRoundRect, radius, radius, primaryPaint);
                }
            } else if (item == Scheme.Element.POINT) {
                Integer id = (Integer) scheme.getDataAt(i);
                Bitmap bitmap = icons.get(id);
                canvas.drawBitmap(bitmap, null, rect, iconPaint);
            }
        }
        //canvas.drawRect(0, 0, getWidth() * secondaryProgress, progressLineHeight, secondaryPaint);
        //canvas.drawRect(0, 0, (getWidth() - sliderView.getWidth()) * progress + sliderView.getPivotX(), progressLineHeight, primaryPaint);
        canvas.restore();
        super.dispatchDraw(canvas);
    }

    public void initState() {
        setProgress(0);
    }

    public void dropState() {
        setCurrent(0, false);
    }

    public void pause() {
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
    }

    public int getLayoutPadding() {
        return layoutPadding;
    }

    public static class Scheme {
        private List<ElementItem> graphicScheme;
        private int barCount;

        public enum Element {
            BAR('-'),
            POINT('.');

            private char representation;
            Element(char c) {
                representation = c;
            }

            public char getRepresentation() {
                return representation;
            }
        }

        private Scheme(SchemeBuilder b) {
            graphicScheme = b.graphicScheme;
            barCount = b.barCount;
        }

        public int getBarCount() {
            return barCount;
        }

        public int getCount() {
            return graphicScheme.size();
        }

        public Element getElementAt(int pos) {
            if (pos < 0 || pos >= graphicScheme.size()) {
                return null;
            }
            return graphicScheme.get(pos).element;
        }

        public Object getDataAt(int pos) {
            if (pos < 0 || pos >= graphicScheme.size()) {
                return null;
            }
            return graphicScheme.get(pos).data;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            if (graphicScheme.size() == 0) {
                return "<Empty>";
            }
            for (ElementItem element : graphicScheme) {
                builder.append(element.element.getRepresentation());
            }
            return builder.toString();
        }

        public RectF getRectAtPosition(int pos) {
            return graphicScheme.get(pos).rect;
        }

        private void recalculate(VideoProgressBar bar) {
            int barsCount = getBarCount();
            if (barsCount < 1) {
                return;
            }
            int count = getCount();
            int dotsCount = count - barsCount;
            int left = bar.barPadding + bar.layoutPadding;
            int maxRight = bar.getWidth() - left;
            int maxBarSize = (maxRight - left - bar.barPadding * (barsCount - 1) * 2 - dotsCount * bar.dotSize) / barsCount;

            for (int i = 0; i < count; i++) {
                ElementItem item = graphicScheme.get(i);
                if (item.element == Scheme.Element.BAR) {
                    item.rect.left = left;
                    item.rect.top = 0;
                    item.rect.bottom = bar.progressLineHeight;
                    item.rect.right = left + maxBarSize;
                    boolean nextIsBar = (i + 1) < count && graphicScheme.get(i + 1).element == Element.BAR;
                    left += maxBarSize + bar.barPadding * (nextIsBar ? 2 : 1);
                } else if (item.element == Scheme.Element.POINT) {
                    item.rect.left = left;
                    item.rect.top = (bar.progressLineHeight - bar.dotSize) / 2;
                    item.rect.bottom = item.rect.top + bar.dotSize;
                    item.rect.right = left + bar.dotSize;
                    left += bar.dotSize + bar.barPadding;
                }
            }
        }

        public static class SchemeBuilder {
            private List<ElementItem> graphicScheme = new ArrayList<>();
            private int barCount;
            public SchemeBuilder() {
            }

            public void addBar() {
                graphicScheme.add(new ElementItem(Element.BAR));
                barCount++;
            }

            public void addPoint(@DrawableRes int id) {
                graphicScheme.add(new ElementItem(Element.POINT, id));
            }

            public Scheme build() {
                return new Scheme(this);
            }
        }

        static class ElementItem {
            Element element;
            Object data;
            RectF rect = new RectF();

            ElementItem(Element element) {
                this.element = element;
            }

            ElementItem(Element element, Object data) {
                this.element = element;
                this.data = data;
            }
        }
    }

    public static class ItemInfo {
        public int position;
        public float currentProgress;
        public float progress;
        static final ItemInfo EMPTY = new ItemInfo();
    }
}
