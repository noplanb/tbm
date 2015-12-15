package com.zazoapp.client.ui.view;

/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.MediaController;

import java.io.IOException;
import java.util.Map;

/**
 * Displays a video file.  The VideoView class
 * can load images from various sources (such as resources or content
 * providers), takes care of computing its measurement from the video so that
 * it can be used in any layout manager, and provides various display options
 * such as scaling and tinting.<p>
 *
 * <em>Note: VideoView does not retain its full state when going into the
 * background.</em>  In particular, it does not restore the current play state,
 * play position, selected tracks. Applications should
 * save and restore these on their own in
 * {@link android.app.Activity#onSaveInstanceState} and
 * {@link android.app.Activity#onRestoreInstanceState}.<p>
 * Also note that the audio session id (from {@link #getAudioSessionId}) may
 * change from its previously returned value when the VideoView is restored.
 */
public class VideoView extends TextureView
        implements MediaController.MediaPlayerControl {
    private String TAG = "VideoView";
    // settable by the client
    private Uri mUri;
    private Map<String, String> mHeaders;
    private Context mContext;
    private final VideoSizeCalculator videoSizeCalculator;
    private VideoStateListener onPlayStateListener;

    // all possible internal states
    private static final int STATE_ERROR              = -1;
    private static final int STATE_IDLE               = 0;
    private static final int STATE_PREPARING          = 1;
    private static final int STATE_PREPARED           = 2;
    private static final int STATE_PLAYING            = 3;
    private static final int STATE_PAUSED             = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    // mCurrentState is a VideoView object's current state.
    // mTargetState is the state that a method caller intends to reach.
    // For instance, regardless the VideoView object's current state,
    // calling pause() intends to bring the object to a target state
    // of STATE_PAUSED.
    private int mCurrentState = STATE_IDLE;
    private int mTargetState  = STATE_IDLE;

    // All the stuff we need for playing and showing a video
    private SurfaceTexture mSurfaceTexture = null;
    private MediaPlayer mMediaPlayer = null;
    private int         mAudioSession;
    private MediaController mMediaController;
    private MediaPlayer.OnCompletionListener mOnCompletionListener;
    private MediaPlayer.OnPreparedListener mOnPreparedListener;
    private int         mCurrentBufferPercentage;
    private MediaPlayer.OnErrorListener mOnErrorListener;
    private MediaPlayer.OnInfoListener mOnInfoListener;
    private int         mSeekWhenPrepared;  // recording the seek position while preparing
    private boolean     mCanPause;
    private boolean     mCanSeekBack;
    private boolean     mCanSeekForward;
    private boolean     speakerPhoneOn = true;

    public VideoView(Context context) {
        this(context, null);
    }

    public VideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        videoSizeCalculator = new VideoSizeCalculator();
        initVideoView();
    }

    @SuppressLint("NewApi")
    public VideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        videoSizeCalculator = new VideoSizeCalculator();
        initVideoView();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        VideoSizeCalculator.Dimens dimens = videoSizeCalculator.measure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(dimens.getWidth(), dimens.getHeight());
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(VideoView.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(VideoView.class.getName());
    }

    public int resolveAdjustedSize(int desiredSize, int measureSpec) {
        return getDefaultSize(desiredSize, measureSpec);
    }

    private void initVideoView() {
        mContext = getContext();
        videoSizeCalculator.setVideoSize(0, 0);
        mSurfaceTexture = getSurfaceTexture();
        setSurfaceTextureListener(mSTListener);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        mCurrentState = STATE_IDLE;
        mTargetState  = STATE_IDLE;
        setOnInfoListener(onInfoToPlayStateListener);
    }

    /**
     * Sets video path.
     *
     * @param path the path of the video.
     */
    public void setVideoPath(String path) {
        setVideoURI(Uri.parse(path));
    }

    /**
     * Sets video URI.
     *
     * @param uri the URI of the video.
     */
    public void setVideoURI(Uri uri) {
        setVideoURI(uri, null);
    }

    /**
     * Sets video URI using specific headers.
     *
     * @param uri     the URI of the video.
     * @param headers the headers for the URI request.
     *                Note that the cross domain redirection is allowed by default, but that can be
     *                changed with key/value pairs through the headers parameter with
     *                "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value
     *                to disallow or allow cross domain redirection.
     */
    public void setVideoURI(Uri uri, Map<String, String> headers) {
        mUri = uri;
        mHeaders = headers;
        mSeekWhenPrepared = 0;
        openVideo();
        requestLayout();
        invalidate();
    }

    public void stopPlayback() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            setKeepScreenOn(false);
            mCurrentState = STATE_IDLE;
            mTargetState  = STATE_IDLE;
        }
    }

    private void openVideo() {
        if (mUri == null || mSurfaceTexture == null) {
            // not ready for playback just yet, will try again later
            return;
        }
        // we shouldn't clear the target state, because somebody might have
        // called start() previously
        release(false);
        try {
            mMediaPlayer = new MediaPlayer();
            if (mAudioSession != 0) {
                mMediaPlayer.setAudioSessionId(mAudioSession);
            } else {
                mAudioSession = mMediaPlayer.getAudioSessionId();
            }
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnInfoListener(mInfoListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mCurrentBufferPercentage = 0;
            mMediaPlayer.setDataSource(mContext, mUri, mHeaders);
            mMediaPlayer.setSurface(new Surface(mSurfaceTexture));
            mMediaPlayer.setAudioStreamType(speakerPhoneOn ? AudioManager.STREAM_MUSIC : AudioManager.STREAM_VOICE_CALL);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.prepareAsync();

            // we don't set the target state here either, but preserve the
            // target state that was there before.
            mCurrentState = STATE_PREPARING;
            attachMediaController();
        } catch (IOException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        }
    }

    public void setMediaController(MediaController controller) {
        if (mMediaController != null) {
            mMediaController.hide();
        }
        mMediaController = controller;
        attachMediaController();
    }

    private void attachMediaController() {
        if (mMediaPlayer != null && mMediaController != null) {
            mMediaController.setMediaPlayer(this);
            View anchorView = this.getParent() instanceof View ?
                    (View)this.getParent() : this;
            mMediaController.setAnchorView(anchorView);
            mMediaController.setEnabled(isInPlaybackState());
        }
    }

    MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
            new MediaPlayer.OnVideoSizeChangedListener() {
                public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                    videoSizeCalculator.setVideoSize(mp.getVideoWidth(), mp.getVideoHeight());
                    if (videoSizeCalculator.hasASizeYet()) {
                        requestLayout();
                    }
                }
            };

    MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {
            mCurrentState = STATE_PREPARED;

            // Get the capabilities of the player for this stream
            mCanPause = mCanSeekBack = mCanSeekForward = true;

            if (mOnPreparedListener != null) {
                mOnPreparedListener.onPrepared(mMediaPlayer);
            }
            if (mMediaController != null) {
                mMediaController.setEnabled(true);
            }
            videoSizeCalculator.setVideoSize(mp.getVideoWidth(), mp.getVideoHeight());

            int seekToPosition = mSeekWhenPrepared;  // mSeekWhenPrepared may be changed after seekTo() call
            if (seekToPosition != 0) {
                seekTo(seekToPosition);
            }
            if (mTargetState == STATE_PLAYING) {
                start();
                if (mMediaController != null) {
                    mMediaController.show();
                }
            } else if (!isPlaying() &&
                    (seekToPosition != 0 || getCurrentPosition() > 0)) {
                if (mMediaController != null) {
                    // Show the media controls when we're paused into a video and make 'em stick.
                    mMediaController.show(0);
                }
            }
        }
    };

    private MediaPlayer.OnCompletionListener mCompletionListener =
            new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    setKeepScreenOn(false);
                    mCurrentState = STATE_PLAYBACK_COMPLETED;
                    mTargetState = STATE_PLAYBACK_COMPLETED;
                    if (mMediaController != null) {
                        mMediaController.hide();
                    }
                    if (mOnCompletionListener != null) {
                        mOnCompletionListener.onCompletion(mMediaPlayer);
                    }
                }
            };

    private MediaPlayer.OnInfoListener mInfoListener =
            new MediaPlayer.OnInfoListener() {
                public  boolean onInfo(MediaPlayer mp, int arg1, int arg2) {
                    if (mOnInfoListener != null) {
                        mOnInfoListener.onInfo(mp, arg1, arg2);
                    }
                    return true;
                }
            };

    private MediaPlayer.OnErrorListener mErrorListener =
            new MediaPlayer.OnErrorListener() {
                public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
                    Log.d(TAG, "Error: " + framework_err + "," + impl_err);
                    mCurrentState = STATE_ERROR;
                    mTargetState = STATE_ERROR;
                    if (mMediaController != null) {
                        mMediaController.hide();
                    }

            /* If an error handler has been supplied, use it and finish. */
                    if (mOnErrorListener != null) {
                        if (mOnErrorListener.onError(mMediaPlayer, framework_err, impl_err)) {
                            return true;
                        }
                    }

            /* Otherwise, pop up an error dialog so the user knows that
             * something bad has happened. Only try and pop up the dialog
             * if we're attached to a window. When we're going away and no
             * longer have a window, don't bother showing the user an error.
             */
                    if (getWindowToken() != null) {
                        Resources r = mContext.getResources();
                        int messageId;

                        if (framework_err == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
                            messageId = android.R.string.VideoView_error_text_invalid_progressive_playback;
                        } else {
                            messageId = android.R.string.VideoView_error_text_unknown;
                        }

                        new AlertDialog.Builder(mContext)
                                .setMessage(messageId)
                                .setPositiveButton(android.R.string.VideoView_error_button,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                        /* If we get here, there is no onError listener, so
                                         * at least inform them that the video is over.
                                         */
                                                if (mOnCompletionListener != null) {
                                                    mOnCompletionListener.onCompletion(mMediaPlayer);
                                                }
                                            }
                                        })
                                .setCancelable(false)
                                .show();
                    }
                    return true;
                }
            };

    private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
            new MediaPlayer.OnBufferingUpdateListener() {
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    mCurrentBufferPercentage = percent;
                }
            };

    /**
     * Register a callback to be invoked when the media file
     * is loaded and ready to go.
     *
     * @param l The callback that will be run
     */
    public void setOnPreparedListener(MediaPlayer.OnPreparedListener l)
    {
        mOnPreparedListener = l;
    }

    /**
     * Register a callback to be invoked when the end of a media file
     * has been reached during playback.
     *
     * @param l The callback that will be run
     */
    public void setOnCompletionListener(MediaPlayer.OnCompletionListener l)
    {
        mOnCompletionListener = l;
    }

    /**
     * Register a callback to be invoked when an error occurs
     * during playback or setup.  If no listener is specified,
     * or if the listener returned false, VideoView will inform
     * the user of any errors.
     *
     * @param l The callback that will be run
     */
    public void setOnErrorListener(MediaPlayer.OnErrorListener l)
    {
        mOnErrorListener = l;
    }

    /**
     * Register a callback to be invoked when an informational event
     * occurs during playback or setup.
     *
     * @param l The callback that will be run
     */
    public void setOnInfoListener(MediaPlayer.OnInfoListener l) {
        mOnInfoListener = l;
    }

    private SurfaceTextureListener mSTListener = new SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int w, int h) {
            boolean isValidState =  (mTargetState == STATE_PLAYING);
            boolean hasValidSize = videoSizeCalculator.currentSizeIs(w, h);
            if (mMediaPlayer != null && isValidState && hasValidSize) {
                if (mSeekWhenPrepared != 0) {
                    seekTo(mSeekWhenPrepared);
                }
                start();
            }
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mSurfaceTexture = surface;
            openVideo();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            // after we return from this we can't use the surface any more
            mSurfaceTexture = null;
            if (mMediaController != null) mMediaController.hide();
            release(true);
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    /*
     * release the media player in any state
     */
    private void release(boolean cleartargetstate) {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            if (cleartargetstate) {
                mTargetState  = STATE_IDLE;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (isInPlaybackState() && mMediaController != null) {
            toggleMediaControlsVisiblity();
        }
        return false;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        if (isInPlaybackState() && mMediaController != null) {
            toggleMediaControlsVisiblity();
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK &&
                keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
                keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
                keyCode != KeyEvent.KEYCODE_VOLUME_MUTE &&
                keyCode != KeyEvent.KEYCODE_MENU &&
                keyCode != KeyEvent.KEYCODE_CALL &&
                keyCode != KeyEvent.KEYCODE_ENDCALL;
        if (isInPlaybackState() && isKeyCodeSupported && mMediaController != null) {
            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                    keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (mMediaPlayer.isPlaying()) {
                    pause();
                    mMediaController.show();
                } else {
                    start();
                    mMediaController.hide();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                if (!mMediaPlayer.isPlaying()) {
                    start();
                    mMediaController.hide();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                    || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                if (mMediaPlayer.isPlaying()) {
                    pause();
                    mMediaController.show();
                }
                return true;
            } else {
                toggleMediaControlsVisiblity();
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    private void toggleMediaControlsVisiblity() {
        if (mMediaController.isShowing()) {
            mMediaController.hide();
        } else {
            mMediaController.show();
        }
    }

    @Override
    public void start() {
        if (isInPlaybackState()) {
            mMediaPlayer.start();
            setKeepScreenOn(true);
            mCurrentState = STATE_PLAYING;
        }
        mTargetState = STATE_PLAYING;
    }

    @Override
    public void pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
                setKeepScreenOn(false);
            }
        }
        mTargetState = STATE_PAUSED;
    }

    public void suspend() {
        release(false);
    }

    public void resume() {
        openVideo();
    }

    @Override
    public int getDuration() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getDuration();
        }

        return -1;
    }

    @Override
    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public void seekTo(int msec) {
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(msec);
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = msec;
        }
    }

    @Override
    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        if (mMediaPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    private boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    @Override
    public boolean canPause() {
        return mCanPause;
    }

    @Override
    public boolean canSeekBackward() {
        return mCanSeekBack;
    }

    @Override
    public boolean canSeekForward() {
        return mCanSeekForward;
    }

    @Override
    public int getAudioSessionId() {
        if (mAudioSession == 0) {
            MediaPlayer foo = new MediaPlayer();
            mAudioSession = foo.getAudioSessionId();
            foo.release();
        }
        return mAudioSession;
    }

    public void setVolume(float volume) {
        if (isInPlaybackState()) {
            mMediaPlayer.setVolume(volume, volume);
        }
    }

    public void changeAudioStream(boolean speakerPhoneOn) {
        this.speakerPhoneOn = speakerPhoneOn;
        if (isPlaying()) {
            mSeekWhenPrepared = mMediaPlayer.getCurrentPosition();
            openVideo();
        }
    }

    private boolean hasPlayStateListener() {
        return onPlayStateListener != null;
    }

    public void setOnPlayStateListener(final VideoStateListener onPlayStateListener) {
        this.onPlayStateListener = onPlayStateListener;
    }

    private final MediaPlayer.OnInfoListener onInfoToPlayStateListener = new MediaPlayer.OnInfoListener() {

        @Override
        public boolean onInfo(final MediaPlayer mp, final int what, final int extra) {
            if (!hasPlayStateListener()) {
                return false;
            }

            if (MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START == what) {
                onPlayStateListener.onFirstVideoFrameRendered();
                onPlayStateListener.onPlay();
            }
            if (MediaPlayer.MEDIA_INFO_BUFFERING_START == what) {
                onPlayStateListener.onBuffer();
            }
            if (MediaPlayer.MEDIA_INFO_BUFFERING_END == what) {
                onPlayStateListener.onPlay();
            }

            return false;
        }
    };

    private static class VideoSizeCalculator {

        private Dimens dimens;

        private int mVideoWidth;
        private int mVideoHeight;

        public VideoSizeCalculator() {
            dimens = new Dimens();
        }

        public void setVideoSize(int mVideoWidth, int mVideoHeight) {
            this.mVideoWidth = mVideoWidth;
            this.mVideoHeight = mVideoHeight;
        }

        public boolean hasASizeYet() {
            return mVideoWidth > 0 && mVideoHeight > 0;
        }

        protected Dimens measure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = View.getDefaultSize(mVideoWidth, widthMeasureSpec);
            int height = View.getDefaultSize(mVideoHeight, heightMeasureSpec);
            if (hasASizeYet()) {

                int widthSpecMode = View.MeasureSpec.getMode(widthMeasureSpec);
                int widthSpecSize = View.MeasureSpec.getSize(widthMeasureSpec);
                int heightSpecMode = View.MeasureSpec.getMode(heightMeasureSpec);
                int heightSpecSize = View.MeasureSpec.getSize(heightMeasureSpec);

                if (widthSpecMode == View.MeasureSpec.EXACTLY && heightSpecMode == View.MeasureSpec.EXACTLY) {
                    // the size is fixed
                    width = widthSpecSize;
                    height = heightSpecSize;

                    // for compatibility, we adjust size based on aspect ratio
                    if (mVideoWidth * height < width * mVideoHeight) {
                        width = height * mVideoWidth / mVideoHeight;
                    } else if (mVideoWidth * height > width * mVideoHeight) {
                        height = width * mVideoHeight / mVideoWidth;
                    }
                } else if (widthSpecMode == View.MeasureSpec.EXACTLY) {
                    // only the width is fixed, adjust the height to match aspect ratio if possible
                    width = widthSpecSize;
                    height = width * mVideoHeight / mVideoWidth;
                    if (heightSpecMode == View.MeasureSpec.AT_MOST && height > heightSpecSize) {
                        // couldn't match aspect ratio within the constraints
                        height = heightSpecSize;
                    }
                } else if (heightSpecMode == View.MeasureSpec.EXACTLY) {
                    // only the height is fixed, adjust the width to match aspect ratio if possible
                    height = heightSpecSize;
                    width = height * mVideoWidth / mVideoHeight;
                    if (widthSpecMode == View.MeasureSpec.AT_MOST && width > widthSpecSize) {
                        // couldn't match aspect ratio within the constraints
                        width = widthSpecSize;
                    }
                } else {
                    // neither the width nor the height are fixed, try to use actual video size
                    width = mVideoWidth;
                    height = mVideoHeight;
                    if (heightSpecMode == View.MeasureSpec.AT_MOST && height > heightSpecSize) {
                        // too tall, decrease both width and height
                        height = heightSpecSize;
                        width = height * mVideoWidth / mVideoHeight;
                    }
                    if (widthSpecMode == View.MeasureSpec.AT_MOST && width > widthSpecSize) {
                        // too wide, decrease both width and height
                        width = widthSpecSize;
                        height = width * mVideoHeight / mVideoWidth;
                    }
                }
            }
            dimens.width = width;
            dimens.height = height;
            return dimens;
        }

        public boolean currentSizeIs(int w, int h) {
            return mVideoWidth == w && mVideoHeight == h;
        }

        public void updateHolder(SurfaceHolder holder) {
            holder.setFixedSize(mVideoWidth, mVideoHeight);
        }

        static class Dimens {
            int width;
            int height;

            public int getWidth() {
                return width;
            }

            public int getHeight() {
                return height;
            }
        }
    }

    public interface VideoStateListener {
        void onFirstVideoFrameRendered();
        void onPlay();
        void onBuffer();
    }
}
