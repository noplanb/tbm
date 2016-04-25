package com.zazoapp.client.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PorterDuff;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import butterknife.ButterKnife;
import com.zazoapp.client.R;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.utilities.Convenience;

import java.lang.ref.WeakReference;

public class GridPreviewFrame extends BasePreviewTextureFrame {

    private static final String TAG = "PreviewTextureFrame";
    private View recordBorder;
    private View recordingMarker;
    private View recordingMarkerLayout;
    private ValueAnimator labelAnimator;
    private WeakReference<View> outerBorderRef;

    public GridPreviewFrame(Context context) {
        super(context);
        init();
    }

    @Override
    public void init() {
        textureView = new TextureView(getContext());
        addView(textureView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        recordBorder = new View(getContext());
        recordBorder.setBackgroundResource(R.drawable.record_frame_border);
        recordBorder.setAlpha(0);
        addView(recordBorder, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        StatusIndicator switchCameraIcon = new StatusIndicator(getContext());
        switchCameraIcon.setRatio(0.3f);
        switchCameraIcon.setImageResource(R.drawable.ic_camera_switch);
        switchCameraIcon.setVisibility(Features.Feature.SWITCH_CAMERA.isUnlocked(getContext()) ? VISIBLE : INVISIBLE);
        this.switchCameraIcon = switchCameraIcon;
        LayoutParams switchCameraIconParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        switchCameraIconParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        switchCameraIcon.setAlpha(SWITCH_ICON_ALPHA);
        addView(switchCameraIcon, switchCameraIconParams);
        View recordingIconLayout = View.inflate(getContext(), R.layout.recording_icon, null);
        recordingMarker = ButterKnife.findById(recordingIconLayout, R.id.record_marker);
        recordingMarkerLayout = ButterKnife.findById(recordingIconLayout, R.id.record_marker_layout);
        TextView recordingLabel = ButterKnife.findById(recordingIconLayout, R.id.record_label);
        recordingMarkerLayout.setAlpha(0);
        recordingLabel.setTypeface(Convenience.getTypeface(getContext()));
        addView(recordingIconLayout);
        setClipToPadding(true);
    }

    public void setRecording(boolean isRecording) {
        super.setRecording(isRecording);
        recordingMarkerLayout.animate().alpha(isRecording ? 1f : 0f).start();
        final View outerRecordingBorder = (outerBorderRef != null) ? outerBorderRef.get() : null;
        if (outerRecordingBorder != null) {
            if (isRecording) {
                outerRecordingBorder.getBackground().setColorFilter(getResources().getColor(R.color.recording_border_color), PorterDuff.Mode.SRC_IN);
            } else {
                outerRecordingBorder.getBackground().setColorFilter(null);
            }
        } else {
            recordBorder.animate().alpha(isRecording ? 1f : 0f).start();
        }
        animateRecordingMarker();
        invalidate();
    }

    public void setOuterRecordingBorder(View outerRecordingBorder) {
        outerBorderRef = new WeakReference<View>(outerRecordingBorder);
    }

    private void animateRecordingMarker() {
        if (isRecording()) {
            if (labelAnimator != null && labelAnimator.isRunning()) {
                labelAnimator.cancel();
            }
            labelAnimator = ValueAnimator.ofFloat(0f, 1f, 1f, 0f);
            labelAnimator.setDuration(2000);
            labelAnimator.setInterpolator(new LinearInterpolator());
            labelAnimator.setRepeatCount(ValueAnimator.INFINITE);
            labelAnimator.setRepeatMode(ValueAnimator.INFINITE);
            labelAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    recordingMarker.setAlpha((Float) animation.getAnimatedValue());
                }
            });
            labelAnimator.start();
        } else {
            if (labelAnimator != null) {
                labelAnimator.end();
            }
        }
    }

}
