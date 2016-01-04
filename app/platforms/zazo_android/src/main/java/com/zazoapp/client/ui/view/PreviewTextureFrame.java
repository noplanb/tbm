package com.zazoapp.client.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import butterknife.ButterKnife;
import com.zazoapp.client.R;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.utilities.Convenience;

public class PreviewTextureFrame extends FrameLayout {

    private static final String TAG = "PreviewTextureFrame";
    private TextureView textureView;
    private View recordBorder;
    private StatusIndicator switchCameraIcon;
	private boolean isRecording;
    private View recordingMarker;
    private View recordingMarkerLayout;
    private TextView recordingLabel;
    private ValueAnimator labelAnimator;

    private static final float SWITCH_ICON_ALPHA = 0.7f;

	public PreviewTextureFrame(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public PreviewTextureFrame(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public PreviewTextureFrame(Context context) {
		super(context);
		init();
	}

	private void init() {
		textureView = new TextureView(getContext());
		addView(textureView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        recordBorder = new View(getContext());
        recordBorder.setBackgroundResource(R.drawable.record_frame_border);
        recordBorder.setAlpha(0);
        addView(recordBorder, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        switchCameraIcon = new StatusIndicator(getContext());
        switchCameraIcon.setRatio(0.3f);
        switchCameraIcon.setImageResource(R.drawable.ic_camera_switch);
        switchCameraIcon.setVisibility(Features.Feature.SWITCH_CAMERA.isUnlocked(getContext()) ? VISIBLE : INVISIBLE);
        LayoutParams switchCameraIconParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        switchCameraIconParams.gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;
        switchCameraIcon.setAlpha(SWITCH_ICON_ALPHA);
        addView(switchCameraIcon, switchCameraIconParams);
        View recordingIconLayout = View.inflate(getContext(), R.layout.recording_icon, null);
        recordingMarker = ButterKnife.findById(recordingIconLayout, R.id.record_marker);
        recordingMarkerLayout = ButterKnife.findById(recordingIconLayout, R.id.record_marker_layout);
        recordingLabel = ButterKnife.findById(recordingIconLayout, R.id.record_label);
        recordingMarkerLayout.setAlpha(0);
        recordingLabel.setTypeface(Convenience.getTypeface(getContext()));
        addView(recordingIconLayout);
        setClipToPadding(true);
	}

	public boolean isRecording() {
		return isRecording;
	}

    public void setRecording(boolean isRecording) {
        this.isRecording = isRecording;
        recordingMarkerLayout.animate().alpha(isRecording ? 1f : 0f).start();
        recordBorder.animate().alpha(isRecording ? 1f : 0f).start();
        animateRecordingMarker();
        if (Features.Feature.SWITCH_CAMERA.isUnlocked(getContext())) {
            switchCameraIcon.setVisibility(VISIBLE);
            switchCameraIcon.animate().alpha(isRecording ? 0 : SWITCH_ICON_ALPHA).start();
        } else {
            switchCameraIcon.setVisibility(INVISIBLE);
        }
        invalidate();
    }

    private void animateRecordingMarker() {
        if (isRecording) {
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

	public void setSurfaceTextureListener(SurfaceTextureListener listener) {
		textureView.setSurfaceTextureListener(listener);
	}

    public void setTransformMatrix(Matrix matrix) {
        textureView.setTransform(matrix);
    }

    public int getPreviewWidth() {
        return textureView.getWidth();
    }

    public int getPreviewHeight() {
        return textureView.getHeight();
    }
}
