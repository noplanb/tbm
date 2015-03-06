package com.zazoapp.client.multimedia;

import com.zazoapp.client.R;

/**
 * Created by skamenkovych@codeminders.com on 2/10/2015.
 */
public enum CameraException {
    NO_HARDWARE(R.string.dialog_camera_exception_no_camera_title, R.string.dialog_camera_exception_no_camera_message),
    NO_FRONT_CAMERA(R.string.dialog_camera_exception_no_front_camera_title, R.string.dialog_camera_exception_no_front_camera_message),
    CAMERA_IN_USE(R.string.dialog_camera_exception_camera_in_use_title, R.string.dialog_camera_exception_camera_in_use_message),
    UNABLE_TO_SET_PARAMS(R.string.dialog_camera_exception_unable_to_set_params_title, R.string.dialog_camera_exception_unable_to_set_params_message),
    UNABLE_TO_FIND_APPROPRIATE_VIDEO_SIZE(R.string.dialog_camera_exception_unable_to_find_appropriate_video_size_title, R.string.dialog_camera_exception_unable_to_find_appropriate_video_size_message);

    private int titleId;
    private int messageId;

    CameraException(int titleId, int messageId) {
        this.titleId = titleId;
        this.messageId = messageId;
    }

    public int getTitleId() {
        return titleId;
    }

    public int getMessageId() {
        return messageId;
    }
}