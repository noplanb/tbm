package com.noplanbees.tbm.ui.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.noplanbees.tbm.R;
import com.noplanbees.tbm.ShortClickListener;
import com.noplanbees.tbm.multimedia.VideoPlayer;
import com.noplanbees.tbm.utilities.Convenience;

public class GridElementView extends RelativeLayout implements View.OnClickListener, ShortClickListener.Callbacks {

	private static final String TAG = "GridElementView";
    private boolean isAlterName;
    private FriendViewListener mEventListener;

    // TODO: Serhii I really dont have a problem for the effects of (some of) these events to be handled here rather than bubbling them up.
    // In my mind a viewController should have the ability to do some entire screen popups for example. I consider the following actions to 
    // fall under that category:
    //  - alerts (definitely)
    //  - toasts (definitely)
    //  - opening the bench. (Maybe - it is kind of a pop up)
    //  - Starting a nudge sequence (Maybe)
    // I would be interested to hear what your thoughts are regarding making these actions accessible from any controller and letting the conroller call them directly.
    public interface ClickListener {
		void onNudgeClicked();
		void onRecordClicked();
        void onEmptyViewClicked();
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
    private View mBody;


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
        mEventListener = listener;
    }

	private void init() {
		LayoutInflater.from(getContext()).inflate(R.layout.grid_element_view, this, true);
		unreadBorder = findViewById(R.id.unread_border);

        mEmptyView = findViewById(R.id.empty_view);
        mBody = findViewById(R.id.body);

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
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mEventListener != null) {
            mEventListener.onAttached();
        }
        moveUnviewedCountToPosition();
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mEventListener != null) {
            mEventListener.onDetached();
        }
    }

    @Override
    public void onShortClick(View v){
    	Log.d(TAG, "shortClick!");
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
        }
    }

	private void moveUnviewedCountToPosition() {
		int dpToPx = Convenience.dpToPx(getContext(), 5);
		twUnreadCount.setX(getWidth() - twUnreadCount.getMeasuredWidth() + dpToPx);
		twUnreadCount.setY(- dpToPx);
	}

    public void showNudge(boolean visible) {
        twNundge.setVisibility(visible ? View.VISIBLE : View.GONE);
        twRecord.requestLayout();
    }

    public void showEmpty(boolean visible) {
        mEmptyView.setVisibility(visible ? VISIBLE : GONE);
        mBody.setVisibility(visible ? GONE : VISIBLE);
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
            unreadBorder.setVisibility(VISIBLE);
            twUnreadCount.setVisibility(VISIBLE);
            twUnreadCount.setText(String.valueOf(unreadMsgCount));
        } else {
            twName.setBackgroundColor(getResources().getColor(R.color.bg_name));
            unreadBorder.setVisibility(INVISIBLE);
            twUnreadCount.setVisibility(INVISIBLE);
        }
    }


    public void setThumbnail(Bitmap bitmap) {
        imgThumb.setImageBitmap(bitmap);
    }

    public void showButtons(boolean visible) {
        buttonsBody.setVisibility(visible ? VISIBLE : GONE);
    }

    public void showUploadingMark(boolean visible) {
        imgUploading.setVisibility(visible ? VISIBLE : GONE);
    }

    public void showProgressLine(boolean visible) {
        progressLine.setVisibility(visible ? VISIBLE : INVISIBLE);
    }

	public void animateUploading() {
        Log.d(TAG, this + "animateUploading");

		int durationMillis = 400;
		progressLine.setBackgroundColor(getContext().getResources().getColor(R.color.bg_uploading));
		ScaleAnimation scale = new ScaleAnimation(
				0f, 1f, 
				1f, 1f, 
				Animation.RELATIVE_TO_SELF, (float) 0,
				Animation.RELATIVE_TO_SELF, (float) 0);
		scale.setDuration(durationMillis);
		scale.setFillAfter(true);

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

	public void animateDownloading() {
		int durationMillis = 500;
		progressLine.setBackgroundColor(getContext().getResources().getColor(R.color.bg_uploading));

		ScaleAnimation scale = new ScaleAnimation(
				0f, 1f, 
				1f, 1f, 
				Animation.RELATIVE_TO_SELF, 1.0f,
				Animation.RELATIVE_TO_SELF, 0.0f);
		scale.setDuration(durationMillis);
		scale.setFillAfter(true);

		float fromYDelta = 0;
		float toYDelta = 0;
		float fromXDelta = getMeasuredWidth() - imgDownloading.getMeasuredWidth();
		float toXDelta = 0;

		TranslateAnimation trAn = new TranslateAnimation(
				Animation.ABSOLUTE, fromXDelta, 
				Animation.RELATIVE_TO_SELF,	toXDelta, 
				Animation.RELATIVE_TO_SELF, fromYDelta, 
				Animation.RELATIVE_TO_SELF, toYDelta);
		trAn.setDuration(durationMillis);
		trAn.setFillAfter(true);
		imgDownloading.startAnimation(trAn);
		progressLine.startAnimation(scale);
	}

}
