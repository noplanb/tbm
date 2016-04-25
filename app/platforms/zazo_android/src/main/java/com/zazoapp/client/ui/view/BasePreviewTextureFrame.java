package com.zazoapp.client.ui.view;

import android.content.Context;
import android.graphics.Matrix;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.zazoapp.client.features.Features;

/**
 * Created by skamenkovych@codeminders.com on 4/25/2016.
 */
public abstract class BasePreviewTextureFrame extends FrameLayout {
    protected static final float SWITCH_ICON_ALPHA = 0.7f;

    protected TextureView textureView;
    protected ImageView switchCameraIcon;

    private boolean isRecording;


    public BasePreviewTextureFrame(Context context) {
        super(context);
    }

    public abstract void init();

    public boolean isRecording() {
        return isRecording;
    }

    public void setRecording(boolean isRecording) {
        this.isRecording = isRecording;
        if (Features.Feature.SWITCH_CAMERA.isUnlocked(getContext())) {
            switchCameraIcon.setVisibility(VISIBLE);
            switchCameraIcon.animate().alpha(isRecording ? 0 : SWITCH_ICON_ALPHA).start();
        } else {
            switchCameraIcon.setVisibility(INVISIBLE);
        }
    }

    public void setSurfaceTextureListener(TextureView.SurfaceTextureListener listener) {
        textureView.setSurfaceTextureListener(listener);
    }

    public View getCameraIconView() {
        return switchCameraIcon;
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

    public void setSwitchCameraIndication() {
        switchCameraIcon.setVisibility(Features.Feature.SWITCH_CAMERA.isUnlocked(getContext()) ? VISIBLE : INVISIBLE);
    }
}
