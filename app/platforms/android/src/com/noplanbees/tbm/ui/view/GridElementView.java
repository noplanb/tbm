package com.noplanbees.tbm.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.noplanbees.tbm.R;
import com.noplanbees.tbm.ShortClickListener;
import com.noplanbees.tbm.multimedia.VideoPlayer;
import com.noplanbees.tbm.utilities.Convenience;

public class GridElementView extends RelativeLayout implements View.OnClickListener {

	private static final String TAG = "GridElementView";

    private FriendViewListener viewEventListener;

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

	private TextView twName;
	private TextView twUnreadCount;
	private TextView twNundge;
	private TextView twRecord;
	private ImageView imgThumb;
	private ImageView imgViewed;
	private ImageView imgDownloading;
	private ImageView imgUploading;
	private View progressLine;
	private View unreadBorder;
    private View mEmptyView;
    private View bodyLayout;

	private ClickListener mClickListener;

	private VideoPlayer videoPlayer;
	private boolean needToHideIndicators;
	private View buttonsBody;

    public GridElementView(Context context) {
        super(context);
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
		unreadBorder = findViewById(R.id.unread_border);

        mEmptyView = findViewById(R.id.empty_view);
        bodyLayout = findViewById(R.id.body);

		twName = (TextView) findViewById(R.id.tw_name);
		twNundge = (TextView) findViewById(R.id.tw_nudge);
		twRecord = (TextView) findViewById(R.id.tw_record);
		twUnreadCount = (TextView) findViewById(R.id.tw_unread_count);
		imgThumb = (ImageView) findViewById(R.id.img_thumb);
		imgViewed = (ImageView) findViewById(R.id.img_viewed);
		imgDownloading = (ImageView) findViewById(R.id.img_downloading);
		imgUploading = (ImageView) findViewById(R.id.img_uploading);
		progressLine = findViewById(R.id.line);
		buttonsBody = findViewById(R.id.buttons_body);

		// allow childrens to extends parent border
		setClipChildren(false);
		setClipToPadding(false);

        twNundge.setOnClickListener(this);
        twRecord.setOnClickListener(this);
        mEmptyView.setOnClickListener(this);
        imgThumb.setOnClickListener(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (viewEventListener != null) {
            viewEventListener.onAttached();
        }
        moveUnviewedCountToPosition();
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
            case R.id.tw_nudge:
                mClickListener.onNudgeClicked();
                break;
            case R.id.tw_record:
                mClickListener.onRecordClicked();
                break;
            case R.id.empty_view:
                mClickListener.onEmptyViewClicked();
                break;
            case R.id.img_thumb:
                mClickListener.onThumbViewClicked();
        }
    }

	private void moveUnviewedCountToPosition() {
		int dpToPx = Convenience.dpToPx(getContext(), 3);
		twUnreadCount.setX(getWidth() - twUnreadCount.getMeasuredWidth() + dpToPx);
		twUnreadCount.setY(- dpToPx);
	}

    public void showNudge(boolean visible) {
        twNundge.setVisibility(visible ? View.VISIBLE : View.GONE);
        twRecord.requestLayout();
    }

    public void showEmpty(boolean visible) {
        mEmptyView.setVisibility(visible ? VISIBLE : GONE);
        bodyLayout.setVisibility(visible ? GONE : VISIBLE);
    }

    public boolean isEmpty() {
        return mEmptyView.getVisibility() == VISIBLE;
    }

    public void setName(String name) {
        twName.setText(name);
    }

    public void setVideoViewed(boolean visible) {
        imgViewed.setVisibility(visible ? VISIBLE : INVISIBLE);
    }

    public void setUnreadCount(boolean visible, int unreadMsgCount) {
        if (visible) {
            twName.setBackgroundColor(getResources().getColor(R.color.bg_unread_msg));
            twUnreadCount.setVisibility(VISIBLE);
            unreadBorder.setVisibility(VISIBLE);
            twUnreadCount.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_red_circle));
            twUnreadCount.setText(String.valueOf(unreadMsgCount));
        } else {
            twName.setBackgroundColor(getResources().getColor(R.color.bg_name));
            twUnreadCount.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_transparent_circle));
            twUnreadCount.postInvalidate();
            twUnreadCount.setVisibility(INVISIBLE);
            unreadBorder.setVisibility(INVISIBLE);
        }
    }

    public void setThumbnail(Bitmap bitmap) {
        if (bitmap != null) {
            imgThumb.setImageBitmap(bitmap);
            imgThumb.setVisibility(VISIBLE);
        } else {
            imgThumb.setVisibility(GONE);
        }
    }

    public void showButtons(boolean visible) {
        buttonsBody.setVisibility(visible ? VISIBLE : GONE);
    }

    public void showUploadingMark(boolean visible) {
        imgUploading.setVisibility(visible ? VISIBLE : GONE);
        if (!visible) {
            imgUploading.clearAnimation();
            progressLine.setVisibility(INVISIBLE);
        }
    }

    public void showDownloadingMark(boolean visible) {
        imgDownloading.setVisibility(visible ? VISIBLE : GONE);
        if (!visible) {
            imgDownloading.clearAnimation();
            progressLine.setVisibility(INVISIBLE);
        }
    }

    public void showProgressLine(boolean visible) {
        progressLine.setVisibility(visible ? VISIBLE : INVISIBLE);
    }

	public void animateUploading() {
        Log.d(TAG, this + "animateUploading");

		int durationMillis = 400;
		progressLine.setBackgroundColor(getContext().getResources().getColor(R.color.bg_uploading));
        progressLine.setVisibility(VISIBLE);
        Interpolator interpolator = new AccelerateDecelerateInterpolator();
		ScaleAnimation scale = new ScaleAnimation(
				0f, 1f, 
				1f, 1f, 
				Animation.RELATIVE_TO_SELF, (float) 0,
				Animation.RELATIVE_TO_SELF, (float) 0);
		scale.setDuration(durationMillis);
        scale.setInterpolator(interpolator);

		float fromYDelta = 0;
		float toYDelta = 0;
		float fromXDelta = -getMeasuredWidth() + imgUploading.getMeasuredWidth();
		float toXDelta = 0;

		TranslateAnimation trAn = new TranslateAnimation(
				Animation.ABSOLUTE, fromXDelta, 
				Animation.RELATIVE_TO_SELF,	toXDelta, 
				Animation.RELATIVE_TO_SELF, fromYDelta, 
				Animation.RELATIVE_TO_SELF, toYDelta);
		trAn.setDuration(durationMillis);
        trAn.setFillAfter(true);
        trAn.setInterpolator(interpolator);
        trAn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                Log.d(TAG, "onAnimationStart");
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Log.d(TAG, "onAnimationEnd");
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                Log.d(TAG, "");
            }
        });
		imgUploading.startAnimation(trAn);
		progressLine.startAnimation(scale);
	}

	public void animateDownloading(final Runnable task) {
		int durationMillis = 400;
		progressLine.setBackgroundColor(getContext().getResources().getColor(R.color.bg_uploading));
        progressLine.setVisibility(VISIBLE);
        Interpolator interpolator = new AccelerateDecelerateInterpolator();
		final ScaleAnimation scale = new ScaleAnimation(
				0f, 1f, 
				1f, 1f, 
				Animation.RELATIVE_TO_SELF, 1.0f,
				Animation.RELATIVE_TO_SELF, 0.0f);
		scale.setDuration(durationMillis);
        scale.setInterpolator(interpolator);

		float fromYDelta = 0;
		float toYDelta = 0;
		float fromXDelta = 0;
		float toXDelta = -getMeasuredWidth() + imgDownloading.getMeasuredWidth();

		TranslateAnimation trAn = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, fromXDelta,
				Animation.ABSOLUTE,	toXDelta,
				Animation.RELATIVE_TO_SELF, fromYDelta, 
				Animation.RELATIVE_TO_SELF, toYDelta);
		trAn.setDuration(durationMillis);
        trAn.setFillAfter(true);
        trAn.setInterpolator(interpolator);
        trAn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                postDelayed(task, 200);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        imgDownloading.startAnimation(trAn);
        progressLine.startAnimation(scale);
	}

}
