package com.zazoapp.client.ui.view;

import android.content.Context;
import android.view.View;
import butterknife.ButterKnife;
import com.zazoapp.client.R;

/**
 * Created by skamenkovych@codeminders.com on 4/25/2016.
 */
public class WelcomeScreenPreview extends BasePreviewTextureFrame {
    public WelcomeScreenPreview(Context context) {
        super(context);
        init();
    }

    @Override
    public void init() {
        View contentView = View.inflate(getContext(), R.layout.welcome_multiple_layout, this);
        textureView = ButterKnife.findById(contentView, R.id.video_preview);
        switchCameraIcon = ButterKnife.findById(contentView, R.id.switch_camera_icon);
    }

    @Override
    public View getSwitchAnimationView() {
        return textureView;
    }
}
