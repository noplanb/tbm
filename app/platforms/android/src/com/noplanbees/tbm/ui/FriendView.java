package com.noplanbees.tbm.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.noplanbees.tbm.Convenience;
import com.noplanbees.tbm.Friend;
import com.noplanbees.tbm.Friend.OutgoingVideoStatus;
import com.noplanbees.tbm.R;
import com.noplanbees.tbm.Video.IncomingVideoStatus;
import com.noplanbees.tbm.VideoPlayer;
import com.noplanbees.tbm.VideoPlayer.StatusCallbacks;

public class FriendView extends RelativeLayout implements StatusCallbacks {

	private static final String TAG = "FriendView";

	private Friend friend;
	private TextView twName;
	private TextView twUnreadCount;
	private ImageView imgThumb;
	private ImageView imgViewed;
	private ImageView imgDownloading;
	private ImageView imgUploading;
	private View progressLine;
	private View body;

	private VideoPlayer videoPlayer;

	private boolean needToHideIndicators;

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

	private void init() {
		LayoutInflater.from(getContext()).inflate(R.layout.friendview_item, this, true);
		body = findViewById(R.id.body);

		twName = (TextView) findViewById(R.id.tw_name);
		twUnreadCount = (TextView) findViewById(R.id.tw_unread_count);
		imgThumb = (ImageView) findViewById(R.id.img_thumb);
		imgViewed = (ImageView) findViewById(R.id.img_viewed);
		imgDownloading = (ImageView) findViewById(R.id.img_downloading);
		imgUploading = (ImageView) findViewById(R.id.img_uploading);
		progressLine = findViewById(R.id.line);

		// allow childrens to extends parent border
		setClipChildren(false);
		setClipToPadding(false);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		updateVideoStatus();
		moveUnviewCountToPosition();
		
		videoPlayer = VideoPlayer.getInstance(getContext());
		videoPlayer.registerStatusCallbacks(this);
	}

	private void moveUnviewCountToPosition() {
		int dpToPx = Convenience.dpToPx(getContext(), 5);
		twUnreadCount.setX(getWidth() - twUnreadCount.getMeasuredWidth() + dpToPx);
		twUnreadCount.setY(- dpToPx);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		videoPlayer.unregisterStatusCallbacks(this);
	}

	public void setFriend(Friend friend) {
		this.friend = friend;
		updateContent();
	}

	private void updateContent() {
		
		int unreadMsgCount = friend.incomingVideoNotViewedCount();

		if (friend.getOutgoingVideoStatus() == OutgoingVideoStatus.VIEWED && !needToHideIndicators) {
			imgViewed.setVisibility(View.VISIBLE);
		} else {
			imgViewed.setVisibility(View.INVISIBLE);
		}

		if (unreadMsgCount > 0 && !needToHideIndicators) {
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
		
		
		if(friend.getOutgoingVideoStatus() != OutgoingVideoStatus.NONE &&
				friend.getOutgoingVideoStatus() != OutgoingVideoStatus.VIEWED &&
				friend.getIncomingVideoStatus() != IncomingVideoStatus.DOWNLOADING)
			imgUploading.setVisibility(View.VISIBLE);
		else
			imgUploading.setVisibility(View.INVISIBLE);
	}

	private void updateVideoStatus() {
		int incomingStatus = friend.getIncomingVideoStatus();
		int outgoingStatus = friend.getOutgoingVideoStatus();

		switch (incomingStatus) {
		case IncomingVideoStatus.NEW:
			break;
		case IncomingVideoStatus.QUEUED:
			break;
		case IncomingVideoStatus.DOWNLOADING:
			needToHideIndicators = true;
			updateContent();
			animateDownloading();
			break;
		case IncomingVideoStatus.DOWNLOADED:
			needToHideIndicators = false;
			progressLine.setVisibility(View.INVISIBLE);
			break;
		case IncomingVideoStatus.VIEWED:
			break;
		case IncomingVideoStatus.FAILED_PERMANENTLY:
			break;
		}

		switch (outgoingStatus) {
		case OutgoingVideoStatus.NEW:
			break;
		case OutgoingVideoStatus.QUEUED:
			break;
		case OutgoingVideoStatus.UPLOADING:
			needToHideIndicators = true;
			updateContent();
			animateUploading();
			break;
		case OutgoingVideoStatus.UPLOADED:
			progressLine.setVisibility(View.INVISIBLE);
			needToHideIndicators = false;
			break;
		case OutgoingVideoStatus.DOWNLOADED:
			break;
		case OutgoingVideoStatus.VIEWED:
			break;
		case OutgoingVideoStatus.FAILED_PERMANENTLY:
			break;
		}
	}

	private void animateUploading() {

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
		imgUploading.startAnimation(trAn);
		progressLine.startAnimation(scale);
	}

	private void animateDownloading() {
		int durationMillis = 500;
		progressLine.setBackgroundColor(getContext().getResources().getColor(R.color.bg_downloading));

		ScaleAnimation scale = new ScaleAnimation(
				0f, 1f, 
				1f, 1f, 
				Animation.RELATIVE_TO_SELF, (float) 1.0f,
				Animation.RELATIVE_TO_SELF, (float) 0);
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

	@Override
	public void onVideoPlaying(String friendId, String videoId) {
		Log.d(TAG, "onVideoPlaying " + friend.getId() + " ? " + friendId);
		needToHideIndicators = friend.getId().equals(friendId);
		updateContent();
//		invalidate();
	}

	@Override
	public void onVideoStopPlaying() {
		Log.d(TAG, "onVideoStopPlaying");
		needToHideIndicators = false;
		updateContent();
	}
}
