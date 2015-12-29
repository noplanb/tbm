package com.zazoapp.client.ui.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.zazoapp.client.R;
import com.zazoapp.client.ui.view.ThumbView.MapArea;
import com.zazoapp.client.ui.view.downloadingview.DownloadingView;
import com.zazoapp.client.ui.view.downloadingview.animation.listener.IDownloadAnimationListener;
import com.zazoapp.client.ui.view.rotationcircleview.view.RotationCircleView;
import com.zazoapp.client.ui.view.uploadingview.UploadingView;
import com.zazoapp.client.utilities.Convenience;

public class GridElementView extends RelativeLayout implements View.OnClickListener, View.OnLongClickListener {

    private static final int ANIMATION_DELAY_MILLIS = 200;
    private static final int ANIMATION_DURATION_MILLIS = 400;

    public interface ClickListener {
		void onNudgeClicked();
		void onRecordClicked();
        void onEmptyViewClicked();
        void onThumbViewClicked();
	}

    public interface FriendViewListener {
        void onAttached();
        void onDetached();
    }

    @InjectView(R.id.tw_name) TextView twName;
    @InjectView(R.id.tw_unread_count) TextView twUnreadCount;
    @InjectView(R.id.img_thumb) ThumbView imgThumb;
    @InjectView(R.id.img_viewed) ImageView imgViewed;
    @InjectView(R.id.img_uploading) ImageView imgUploading;
    @InjectView(R.id.downloading_animation_view) DownloadingView downloadingView;
    @InjectView(R.id.uploading_animation_view) UploadingView uploadingView;
    @InjectView(R.id.animation_background) View animationBackground;
    @InjectView(R.id.empty_view) View mEmptyView;
    @InjectView(R.id.body) View bodyLayout;
    @InjectView(R.id.hold_to_record) RotationCircleView holdToRecordView;
    @InjectView(R.id.card_empty_icon) ImageView emptyIcon;
    @InjectView(R.id.card_empty_text) TextView emptyText;
    @InjectView(R.id.card_layout) RelativeLayout cardLayout;

	private ClickListener mClickListener;

    private FriendViewListener viewEventListener;

    private final int colors[];
    private final MapArea areas[] = {MapArea.LEFT_BOTTOM,  MapArea.RIGHT_TOP, MapArea.LEFT_TOP,  MapArea.LEFT_TOP, MapArea.RIGHT_BOTTOM, MapArea.LEFT_BOTTOM, MapArea.RIGHT_TOP, MapArea.RIGHT_BOTTOM};

    public GridElementView(Context context) {
        super(context);
        colors = getResources().getIntArray(R.array.thumb_colors);
        init();
    }

    public void setOnClickListener(ClickListener clickListener) {
        mClickListener = clickListener;
    }

    public void setEventListener(FriendViewListener listener) {
        viewEventListener = listener;
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.grid_element_view, this, true);
        ButterKnife.inject(this);

        // allow childrens to extends parent border
        setClipChildren(false);
        setClipToPadding(false);

        Typeface face = Typeface.createFromAsset(getContext().getAssets(), "fonts/Roboto-Medium.ttf");
        twUnreadCount.setTypeface(face);
        emptyText.setTypeface(face);
        twName.setTypeface(face);

        holdToRecordView.setOnClickListener(this);
        mEmptyView.setOnClickListener(this);
        mEmptyView.setOnLongClickListener(this);
        imgThumb.setOnClickListener(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (viewEventListener != null) {
            viewEventListener.onAttached();
        }
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (viewEventListener != null) {
            viewEventListener.onDetached();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.hold_to_record:
                mClickListener.onRecordClicked();
                break;
            case R.id.empty_view:
                mClickListener.onEmptyViewClicked();
                break;
            case R.id.img_thumb:
                mClickListener.onThumbViewClicked();
        }
        //mClickListener.onNudgeClicked(); // TODO UI 2.0. Do after recording a video
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.empty_view:
                mClickListener.onEmptyViewClicked();
                return true;
        }
        return false;
    }

    public void showEmpty(boolean visible, boolean next) {
        mEmptyView.setVisibility(visible ? VISIBLE : GONE);
        bodyLayout.setVisibility(visible ? GONE : VISIBLE);
        emptyIcon.setEnabled(next);
        emptyText.setVisibility(next ? VISIBLE : GONE);
    }

    public boolean isEmpty() {
        return mEmptyView.getVisibility() == VISIBLE;
    }

    public boolean isNext() {
        return isEmpty() && emptyIcon.isEnabled();
    }

    public void setName(String name) {
        twName.setText(name);
    }

    public void setVideoViewed(boolean visible) {
        imgViewed.setVisibility(visible ? VISIBLE : INVISIBLE);
    }

    public void setUnreadCount(boolean visible, int unreadMsgCount) {
        if (visible) {
            twUnreadCount.setVisibility(VISIBLE);
            cardLayout.getBackground().setColorFilter(getResources().getColor(R.color.primary), PorterDuff.Mode.SRC_IN);
            twUnreadCount.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_red_circle));
            twUnreadCount.setText(String.valueOf(unreadMsgCount));
        } else {
            cardLayout.getBackground().setColorFilter(null);
            twUnreadCount.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_transparent_circle));
            twUnreadCount.postInvalidate();
            twUnreadCount.setVisibility(INVISIBLE);
        }
    }

    public void setThumbnail(Bitmap bitmap, int visibility) {
        if (bitmap != null) {
            imgThumb.setImageBitmap(bitmap);
            imgThumb.setMapArea(MapArea.FULL);
            imgThumb.setVisibility(visibility);
        } else {
            imgThumb.setVisibility(INVISIBLE);
        }
    }

    public void setStubThumbnail(CharSequence text, int visibility) {
        int color = Convenience.getStringDependentItem(text, colors);
        imgThumb.setFillColor(color);
        imgThumb.setImageResource(R.drawable.navigation_background_pattern);
        imgThumb.setMapArea(Convenience.getStringDependentItem(text, areas));
        imgThumb.setVisibility(visibility);
        holdToRecordView.getIconView().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        holdToRecordView.getTextView().setTextColor(color);
    }

    public void showButtons(boolean visible) {
        holdToRecordView.setVisibility(visible ? VISIBLE : GONE);
    }

    public void showUploadingMark(boolean visible) {
        if (visible) {
            RelativeLayout.LayoutParams params = (LayoutParams) imgUploading.getLayoutParams();
            params.addRule(ALIGN_PARENT_RIGHT);
            params.addRule(ALIGN_PARENT_LEFT, 0); // remove rule
            imgUploading.setLayoutParams(params);
        }
        imgUploading.clearAnimation();
        imgUploading.setVisibility(visible ? VISIBLE : INVISIBLE);

    }

    public void showDownloadingMark(boolean visible) {
        downloadingView.setVisibility(visible ? VISIBLE : INVISIBLE);
        if (!visible) {
            downloadingView.getAnimationController().cancel();
        }
    }

	public void animateUploading(final Runnable task) {
        Interpolator interpolator = new AccelerateDecelerateInterpolator();

		float fromYDelta = 0;
		float toYDelta = 0;
		float fromXDelta = 0;
		float toXDelta = getMeasuredWidth() - imgUploading.getMeasuredWidth();

        RelativeLayout.LayoutParams params = (LayoutParams) imgUploading.getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params.addRule(ALIGN_PARENT_RIGHT, 0); // remove rule
        imgUploading.setLayoutParams(params);
        imgUploading.setVisibility(VISIBLE);
		TranslateAnimation trAn = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, fromXDelta,
				Animation.ABSOLUTE,	toXDelta,
				Animation.RELATIVE_TO_SELF, fromYDelta, 
				Animation.RELATIVE_TO_SELF, toYDelta);
		trAn.setDuration(ANIMATION_DURATION_MILLIS);
        trAn.setStartOffset(ANIMATION_DELAY_MILLIS);
        trAn.setFillAfter(true);
        trAn.setInterpolator(interpolator);
        trAn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                postDelayed(task, ANIMATION_DELAY_MILLIS);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
		imgUploading.startAnimation(trAn);
	}

	public void animateDownloading(final Runnable task) {
        int duration = 350;
        animationBackground.setAlpha(0f);
        ValueAnimator startAnimation = ValueAnimator.ofFloat(0, 1f);
        startAnimation.setDuration(duration);
        startAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            float size = Math.min(getWidth(), getHeight()) * 0.5f;
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                setProgressAnimation(downloadingView, (int) (size * value));
                animationBackground.setAlpha(value * 0.5f);
            }
        });
        final ValueAnimator endAnimation = ValueAnimator.ofFloat(1f, 0f);
        endAnimation.setDuration(duration);
        endAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            float size = Math.min(getWidth(), getHeight()) * 0.5f;
            int contentRadius = getResources().getDimensionPixelSize(R.dimen.grid_item_status_icon_content_radius);
            float endSize = contentRadius * 2;
            float targetX = twUnreadCount.getX() + contentRadius;
            float targetY = twUnreadCount.getY() + contentRadius;
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                setProgressFinishAnimation(downloadingView, (int) Math.abs((size - endSize) * value + endSize), 1 - value, targetX, targetY);
                animationBackground.setAlpha(value * 0.5f);
            }
        });
        downloadingView.setVisibility(VISIBLE);

        downloadingView.getAnimationController().setExternalDownloadAnimationListener(new IDownloadAnimationListener.Stub() {
            @Override
            public void onArrowHideAnimationFinish() {
                endAnimation.start();
            }

            @Override
            public void onColorChangeAnimationFinish() {
                post(task);
            }
        });

        final ValueAnimator progress = ValueAnimator.ofFloat(0, 100f);
        progress.setInterpolator(new LinearInterpolator());
        progress.setDuration(750);
        progress.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                downloadingView.getAnimationController().updateProgress(value);
            }
        });
        startAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                downloadingView.getAnimationController().start();
                progress.start();
            }
        });
        startAnimation.start();
    }

    /**
     * @param v animated view, should be placed inside FrameLayout
     */
    private void setProgressAnimation(View v, int size) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) v.getLayoutParams();
        params.width = size;
        params.height = size;
        v.setLayoutParams(params);
        v.setX((getWidth() - size) / 2);
        v.setY(2 * getHeight() / 5 - size / 2);
        v.setVisibility(VISIBLE);
    }

    /**
     * @param v animated view, should be placed inside FrameLayout
     */
    private void setProgressFinishAnimation(View v, int size, float fraction, float targetX, float targetY) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) v.getLayoutParams();
        params.width = size;
        params.height = size;
        v.setLayoutParams(params);
        int radius = size / 2;
        int cX = getWidth() / 2;
        int cY = 2 * getHeight() / 5;
        v.setX(cX - radius + (targetX - cX) * fraction);
        v.setY(cY - radius + (targetY - cY) * fraction);
        Log.d("Animation", "" + cX + ":" + cY + " " + radius + " " + targetX + ":" + targetY + " "
         + v.getX() + ":" + v.getY() + " " + twUnreadCount.getX() + ":" + twUnreadCount.getY() + " " + twUnreadCount.getWidth() + ":" + twUnreadCount.getHeight());
        v.setVisibility(VISIBLE);
    }

    public boolean isAnimating() {
        return downloadingView.getAnimation() != null || imgUploading.getAnimation() != null;
    }

    public boolean isReadyToAnimate() {
        return getMeasuredWidth() != 0;
    }
}
