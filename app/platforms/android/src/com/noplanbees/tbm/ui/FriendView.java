package com.noplanbees.tbm.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.noplanbees.tbm.Friend;
import com.noplanbees.tbm.Friend.OutgoingVideoStatus;
import com.noplanbees.tbm.R;
import com.noplanbees.tbm.Video;

public class FriendView extends FrameLayout {

	private static final String TAG = "FriendView";
	
	private Friend friend;
	private TextView twName;
	private TextView twUnreadCount;
	private ImageView imgThumb;
	private ImageView imgViewed;
	private ImageView imgDownloading;
	private ImageView imgUploading;
	private View progressUploading;
	private View progressDownloading;
	private View body;

	public FriendView(Context context) {
		super(context);
		init();
	}

	public FriendView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public FriendView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		updateVideoStatus();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
	}

	private void init() {
		LayoutInflater.from(getContext()).inflate(R.layout.friendview_item, this, true);
		body  = findViewById(R.id.body);

		twName = (TextView) findViewById(R.id.tw_name);
		twUnreadCount = (TextView) findViewById(R.id.tw_unread_count);
		imgThumb = (ImageView) findViewById(R.id.img_thumb);
		imgViewed = (ImageView) findViewById(R.id.img_viewed);
		imgDownloading = (ImageView) findViewById(R.id.img_downloading);
		imgUploading = (ImageView) findViewById(R.id.img_uploading);
		progressUploading = findViewById(R.id.line);
		progressDownloading = findViewById(R.id.line2);
	}

	public void setFriend(Friend friend) {
		this.friend = friend;
		updateContent();
	}

	private void updateContent() {
		int unreadMsgCount = friend.incomingVideoNotViewedCount();
		
		if(friend.getOutgoingVideoStatus() ==  OutgoingVideoStatus.VIEWED){
			imgViewed.setVisibility(View.VISIBLE);
		}else{
			imgViewed.setVisibility(View.INVISIBLE);
		}

		if (unreadMsgCount>0) {
			body.setBackgroundResource(R.drawable.friend_body_unread_border);
			twName.setBackgroundColor(getResources().getColor(R.color.bg_unread_msg));
			
			twUnreadCount.setVisibility(View.VISIBLE);
			twUnreadCount.setText("" + unreadMsgCount);
		} else {
			body.setBackgroundColor(getResources().getColor(R.color.bg_name));
			twName.setBackgroundColor(getResources().getColor(R.color.bg_name));
			twUnreadCount.setVisibility(View.INVISIBLE);
		}

		if (friend.thumbExists())
			imgThumb.setImageBitmap(friend.lastThumbBitmap());

		twName.setText(friend.getStatusString());
	}

	private void updateVideoStatus() {
		int incomingStatus = friend.getIncomingVideoStatus();
		int outgoingStatus = friend.getOutgoingVideoStatus();
		
		switch (incomingStatus) {
		case Video.IncomingVideoStatus.NEW:
			break;
		case Video.IncomingVideoStatus.QUEUED:
			break;
		case Video.IncomingVideoStatus.DOWNLOADING:
			animateDownloading(0, 
					-getMeasuredWidth() + imgDownloading.getMeasuredWidth());
			break;
		case Video.IncomingVideoStatus.DOWNLOADED:
			imgDownloading.setVisibility(View.INVISIBLE);
			progressDownloading.setVisibility(View.INVISIBLE);
			break;
		case Video.IncomingVideoStatus.VIEWED:
			break;
		case Video.IncomingVideoStatus.FAILED_PERMANENTLY:
			break;
		}
		
		switch (outgoingStatus) {
		case OutgoingVideoStatus.NEW:
			break;
		case OutgoingVideoStatus.QUEUED:
			break;
		case OutgoingVideoStatus.UPLOADING:
			animateUploading(0, getMeasuredWidth() - imgUploading.getMeasuredWidth());
			break;
		case OutgoingVideoStatus.UPLOADED:
			imgUploading.setVisibility(View.INVISIBLE);
			progressUploading.setVisibility(View.INVISIBLE);
			break;
		case OutgoingVideoStatus.DOWNLOADED:
			break;
		case OutgoingVideoStatus.VIEWED:
			break;
		case OutgoingVideoStatus.FAILED_PERMANENTLY:
			break;
		}

	}

	
	private void animateUploading(float fromXDelta, float toXDelta) {
		
		int durationMillis = 500;

		int width = getMeasuredWidth();
		progressUploading.setBackgroundColor(getContext().getResources().getColor(R.color.bg_uploading));
		

		ScaleAnimation scale = new ScaleAnimation(
				0f, width, 
				1f, 1f, 
				Animation.RELATIVE_TO_SELF, (float) 0.5,
				Animation.RELATIVE_TO_SELF, (float) 0.5);
		scale.setDuration(durationMillis);
		scale.setFillAfter(true);
		// scale.setInterpolator(new AccelerateInterpolator(1.0f));
		scale.setInterpolator(new AccelerateDecelerateInterpolator());

		float fromYDelta = 0;
		float toYDelta = 0;
		TranslateAnimation trAn = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, fromXDelta, 
				Animation.ABSOLUTE,toXDelta, 
				Animation.RELATIVE_TO_SELF, fromYDelta, 
				Animation.RELATIVE_TO_SELF, toYDelta);
		// trAn.setInterpolator(new AccelerateInterpolator(1.0f));
		trAn.setInterpolator(new AccelerateDecelerateInterpolator());
		trAn.setDuration(durationMillis);
		trAn.setFillAfter(true);
		AnimationListener listener = new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {}
			@Override
			public void onAnimationRepeat(Animation animation) {}
			@Override
			public void onAnimationEnd(Animation animation) {
			}
		};
		trAn.setAnimationListener(listener);
		imgUploading.startAnimation(trAn);
		progressUploading.startAnimation(scale);
	}
	
	private void animateDownloading(float fromXDelta, float toXDelta) {
		int durationMillis = 500;
		int width = getMeasuredWidth();
		progressDownloading.setBackgroundColor(getContext().getResources().getColor(R.color.bg_unread_msg));
		
		ScaleAnimation scale = new ScaleAnimation(
				0f, width, 
				1f, 1f, 
				Animation.RELATIVE_TO_SELF, (float) 0.5,
				Animation.RELATIVE_TO_SELF, (float) 0.5);
		scale.setDuration(durationMillis);
		scale.setFillAfter(true);
		// scale.setInterpolator(new AccelerateInterpolator(1.0f));
		scale.setInterpolator(new AccelerateDecelerateInterpolator());

		float fromYDelta = 0;
		float toYDelta = 0;
		TranslateAnimation trAn = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, fromXDelta, 
				Animation.ABSOLUTE,	toXDelta, 
				Animation.RELATIVE_TO_SELF, fromYDelta, 
				Animation.RELATIVE_TO_SELF, toYDelta);
		// trAn.setInterpolator(new AccelerateInterpolator(1.0f));
		trAn.setInterpolator(new AccelerateDecelerateInterpolator());
		trAn.setDuration(durationMillis);
		trAn.setFillAfter(true);
		AnimationListener listener = new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {}
			
			@Override
			public void onAnimationEnd(Animation animation) {
			}
		};
		trAn.setAnimationListener(listener);
		imgDownloading.startAnimation(trAn);
		progressDownloading.startAnimation(scale);
	}

}
