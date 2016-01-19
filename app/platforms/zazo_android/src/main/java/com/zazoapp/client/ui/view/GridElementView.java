package com.zazoapp.client.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.zazoapp.client.R;
import com.zazoapp.client.ui.animations.TransferProgressAnimation;
import com.zazoapp.client.ui.animations.UnreadCountAnimation;
import com.zazoapp.client.ui.animations.ViewedAnimation;
import com.zazoapp.client.ui.view.ThumbView.MapArea;
import com.zazoapp.client.ui.view.rotationcircleview.view.RotationCircleView;
import com.zazoapp.client.ui.view.transferview.DownloadingView;
import com.zazoapp.client.ui.view.transferview.UploadingView;
import com.zazoapp.client.utilities.Convenience;

public class GridElementView extends RelativeLayout implements View.OnClickListener, View.OnLongClickListener {

    public interface ClickListener {
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
    @InjectView(R.id.uploading_layout) View uploadingLayout;
    @InjectView(R.id.unread_count_layout) View unreadCountLayout;
    @InjectView(R.id.viewed_layout) View viewedLayout;
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

        Typeface face = Convenience.getTypeface(getContext());
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
        mEmptyView.setVisibility(visible ? VISIBLE : INVISIBLE);
        bodyLayout.setVisibility(visible ? INVISIBLE : VISIBLE);
        emptyIcon.setEnabled(next);
        emptyText.setVisibility(next ? VISIBLE : INVISIBLE);
        if (visible) {
            setVideoViewed(false);
            setUnreadCount(false, 0, false);
            showUploadingMark(false);
        }
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
        viewedLayout.setVisibility(visible ? VISIBLE : INVISIBLE);
    }

    public void setUnreadCountWithAnimation(boolean visible, int unreadMsgCount, Runnable endAction) {
        setUnreadCount(visible, unreadMsgCount, true);
        if (visible) {
            animateUnreadCountChanging(unreadMsgCount, endAction);
        }
    }

    public void setUnreadCount(boolean visible, int unreadMsgCount, boolean willAnimate) {
        if (visible) {
            unreadCountLayout.setVisibility(VISIBLE);
            cardLayout.getBackground().setColorFilter(getResources().getColor(R.color.primary), PorterDuff.Mode.SRC_IN);
            if (!willAnimate) {
                twUnreadCount.setText(String.valueOf(unreadMsgCount));
            }
        } else {
            cardLayout.getBackground().setColorFilter(null);
            twUnreadCount.setText((unreadMsgCount <= 0) ? "" : String.valueOf(unreadMsgCount));
            unreadCountLayout.setVisibility(INVISIBLE);
        }
    }

    private void animateUnreadCountChanging(final int unreadMsgCount, Runnable endAction) {
        UnreadCountAnimation.animate(this, new Runnable() {
            @Override
            public void run() {
                twUnreadCount.setText(String.valueOf(unreadMsgCount));
            }
        }, endAction);
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
        holdToRecordView.setVisibility(visible ? VISIBLE : INVISIBLE);
    }

    public void showUploadingMark(boolean visible) {
        if (visible) {
            RelativeLayout.LayoutParams params = (LayoutParams) uploadingLayout.getLayoutParams();
            params.addRule(ALIGN_PARENT_RIGHT);
            params.addRule(ALIGN_PARENT_LEFT, 0); // remove rule
            uploadingLayout.setLayoutParams(params);
        }
        uploadingLayout.clearAnimation();
        uploadingLayout.setVisibility(visible ? VISIBLE : INVISIBLE);

    }

    public void showDownloadingMark(boolean visible) {
        downloadingView.setVisibility(visible ? VISIBLE : INVISIBLE);
        if (!visible) {
            downloadingView.getAnimationController().cancel();
        }
    }

    public void animateUploading(final Runnable task) {
        TransferProgressAnimation.animateUploading(this, task);
    }

    public void animateDownloading(Runnable task) {
        TransferProgressAnimation.animateDownloading(this, task);
    }

    public void animateViewed(Runnable task) {
        ViewedAnimation.animate(this, imgViewed, task);
    }

    public boolean isAnimating() {
        return downloadingView.getAnimation() != null || uploadingLayout.getAnimation() != null;
    }

    public boolean isReadyToAnimate() {
        return getMeasuredWidth() != 0;
    }
}
