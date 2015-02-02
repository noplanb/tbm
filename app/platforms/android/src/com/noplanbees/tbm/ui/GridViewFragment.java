package com.noplanbees.tbm.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.VideoView;
import com.noplanbees.tbm.GridElementController;
import com.noplanbees.tbm.GridManager;
import com.noplanbees.tbm.GridManager.GridEventNotificationDelegate;
import com.noplanbees.tbm.IntentHandler;
import com.noplanbees.tbm.R;
import com.noplanbees.tbm.crash_dispatcher.Dispatch;
import com.noplanbees.tbm.model.ActiveModelsHandler;
import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.Friend.VideoStatusChangedCallback;
import com.noplanbees.tbm.model.FriendFactory;
import com.noplanbees.tbm.model.GridElement;
import com.noplanbees.tbm.model.GridElementFactory;
import com.noplanbees.tbm.multimedia.CameraManager;
import com.noplanbees.tbm.multimedia.CameraManager.CameraExceptionHandler;
import com.noplanbees.tbm.multimedia.VideoPlayer;
import com.noplanbees.tbm.multimedia.VideoRecorder;
import com.noplanbees.tbm.network.FileDownloadService;
import com.noplanbees.tbm.ui.view.NineViewGroup;
import com.noplanbees.tbm.ui.view.NineViewGroup.LayoutCompleteListener;
import com.noplanbees.tbm.ui.view.PreviewTextureFrame;
import com.noplanbees.tbm.utilities.Logger;

import java.util.ArrayList;

public class GridViewFragment extends Fragment implements GridEventNotificationDelegate,
		VideoRecorder.VideoRecorderExceptionHandler, CameraExceptionHandler, VideoStatusChangedCallback,
VideoPlayer.StatusCallbacks, SensorEventListener, GridElementController.Callbacks {

	private static final String TAG = GridViewFragment.class.getSimpleName();

    private ArrayList<GridElementController> mViewControllers;

    public interface Callbacks {
        void onFinish();
        void onBenchRequest();
        void onNudgeFriend(Friend f);
        void showRecordDialog();
        void showBadConnectionDialog();
    }

	private NineViewGroup nineViewGroup;
	private ActiveModelsHandler activeModelsHandler;
	private VideoPlayer videoPlayer;
	private VideoRecorder videoRecorder;
	private Callbacks callbacks;
	private Handler uiHandler = new Handler(Looper.getMainLooper());

    private SensorManager sensorManager;
    private Sensor proximitySensor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(TAG, "onCreate" + this);

		activeModelsHandler = ActiveModelsHandler.getActiveModelsHandler();
		activeModelsHandler.getFf().addVideoStatusChangedCallbackDelegate(this);

		videoPlayer = VideoPlayer.getInstance(getActivity());

		CameraManager.addExceptionHandlerDelegate(this);
		videoRecorder = new VideoRecorder(getActivity());
		videoRecorder.addExceptionHandlerDelegate(this);

		GridManager.initGrid(getActivity());
		GridManager.setGridEventNotificationDelegate(this);

		mUnexpectedTerminationHelper.init();

        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (proximitySensor == null) {
            Log.i(TAG, "Proximity sensor not found");
        }

        mViewControllers = new ArrayList<>(GridManager.GRID_ELEMENTS_COUNT);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		callbacks = (Callbacks) activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.nineviewgroup_fragment, container, false);

		// TODO: factor into a setupVideoPlayer() method
		View videoBody = v.findViewById(R.id.video_body);
		VideoView videoView = (VideoView) v.findViewById(R.id.video_view);
		videoPlayer.setVideoView(videoView);
		videoPlayer.setVideoViewBody(videoBody);

		nineViewGroup = (NineViewGroup) v.findViewById(R.id.grid_view);

        nineViewGroup.setGestureListener(new NineViewGestureListener());

		nineViewGroup.setChildLayoutCompleteListener(new LayoutCompleteListener() {
			@Override
			public void onLayoutComplete() {
				setupGridElements();
				layoutVideoRecorder();
                handleIntentAction(getActivity().getIntent());
			}
		});

		return v;
	}

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

	@Override
	public void onResume() {
		Logger.d(TAG, "onResume");
		videoRecorder.onResume();
        videoPlayer.registerStatusCallbacks(this);
        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
		super.onResume();
	}

	@Override
	public void onPause() {
		Logger.d(TAG, "onPause");
		super.onPause();
		videoRecorder.onPause();
		videoRecorder.stopRecording();
        videoPlayer.unregisterStatusCallbacks(this);
		videoPlayer.release(getActivity());
        sensorManager.unregisterListener(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mUnexpectedTerminationHelper.finish();
	}

	
	
	//-------------------
	// Setup gridElements
	//-------------------
	private void setupGridElements(){
        if (!mViewControllers.isEmpty()) {
            mViewControllers.clear();
        }
        int i = 0;
        for (GridElement ge : GridElementFactory.getFactoryInstance().all()){
        	GridElementController gec = new GridElementController(getActivity(), ge, nineViewGroup.getSurroundingFrame(i), GridViewFragment.this);
        	mViewControllers.add(gec);
        	i++;
        }
	}

	//---------------------
	// VideoRecorder Layout
	//---------------------
	private void layoutVideoRecorder(){
		FrameLayout fl = nineViewGroup.getCenterFrame();
		if (fl.getChildCount() != 0){
			Log.w(TAG, "layoutVideoRecorder: not adding preview view as it appears to already have been added.");
			return;
		}
		Log.i(TAG, "layoutVideoRecorder: adding videoRecorder preview");
		PreviewTextureFrame vrFrame = (PreviewTextureFrame) videoRecorder.getView();
		fl.addView(vrFrame, new PreviewTextureFrame.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
	}

	// ----------------------------
	// VideoRecorder event handling
	// ----------------------------
	private void onRecordStart(String friendId) {
		videoPlayer.stop();
		GridElement ge = GridElementFactory.getFactoryInstance().getGridElementByFriendId(friendId);
		if (!ge.hasFriend())
			return;

		Friend f = ge.getFriend();
		GridManager.rankingActionOccurred(f);
		if (videoRecorder.startRecording(f)) {
			Log.i(TAG, "onRecordStart: START RECORDING: " + f.get(Friend.Attributes.FIRST_NAME));
		} else {
			Dispatch.dispatch("onRecordStart: unable to start recording" + f.get(Friend.Attributes.FIRST_NAME));
		}
	}

	private void onRecordCancel() {
		// Different from abortAnyRecording because we always toast here.
		videoRecorder.stopRecording();
		showToast("Not sent.");
	}

	private void onRecordStop() {
		if (videoRecorder.stopRecording()) {
			Friend f = videoRecorder.getCurrentFriend();
			Log.i(TAG, "onRecordStop: STOP RECORDING. to " + f.get(Friend.Attributes.FIRST_NAME));
			f.setAndNotifyOutgoingVideoStatus(Friend.OutgoingVideoStatus.NEW);
			f.uploadVideo();
		}
	}

	// ---------------------------------------
	// Video Recorder ExceptionHandler delegate
	// ----------------------------------------
	@Override
	public void unableToSetPrievew() {
		showToast("unable to set preview");
	}

	@Override
	public void unableToPrepareMediaRecorder() {
		showToast("Unable to prepare MediaRecorder");
	}

	@Override
	public void recordingAborted() {
		showToast("Recording Aborted due to Release before Stop.");
	}

	@Override
	public void recordingTooShort() {
		showToast("Not sent. Too short.");
	}

	@Override
	public void illegalStateOnStart() {
		showToast("Runntime exception on MediaRecorder.start. Quitting app.");
	}

	@Override
	public void runntimeErrorOnStart() {
		showToast("Unable to start recording. Try again.");
	}

	// -------------------------------
	// CameraExceptionHandler delegate
	// -------------------------------
	@Override
	public void noCameraHardware() {
		showCameraExceptionDialog("No Camera",
				"Your device does not seem to have a camera. This app requires a camera.", "Quit", "Try Again");
	}

	@Override
	public void noFrontCamera() {
		showCameraExceptionDialog("No Front Camera",
				"Your device does not seem to have a front facing camera. This app requires a front facing camera.",
				"Quit", "Try Again");
	}

	@Override
	public void cameraInUseByOtherApplication() {
		showCameraExceptionDialog(
				"Camera in Use",
				"Your camera seems to be in use by another application. Please close that app and try again. You may also need to restart your device.",
				"Quit", "Try Again");
	}

	private void showCameraExceptionDialog(final String title, final String message, final String negativeText,
			final String positiveText) {
		uiHandler.post(new Runnable() {
            private DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            callbacks.onFinish();
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            videoRecorder.dispose();
                            videoRecorder.restore();
                            break;
                    }
                }
            };

            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(title).setMessage(message)
                        .setNegativeButton(negativeText, clickListener)
                        .setPositiveButton(positiveText, clickListener);
                AlertDialog alertDialog = builder.create();
                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.show();
            }
        });
	}

	@Override
	public void unableToSetCameraParams() {
		// TODO Auto-generated method stub

	}

	@Override
	public void unableToFindAppropriateVideoSize() {
		// TODO Auto-generated method stub

	}

	private void showToast(final String msg) {
		uiHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        });
	}

	@Override
	public void gridDidChange() {
	}

	@Override
	public void onVideoStatusChanged(final Friend friend) {
		uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null)
                    GridManager.moveFriendToGrid(getActivity(), friend);
            }
        });
	}

	private UnexpectedTerminationHelper mUnexpectedTerminationHelper = new UnexpectedTerminationHelper();

	private class UnexpectedTerminationHelper {
		private Thread mThread;
		private Thread.UncaughtExceptionHandler mOldUncaughtExceptionHandler = null;
		private Thread.UncaughtExceptionHandler mUncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
			// gets called on the same (main) thread
			@Override
			public void uncaughtException(Thread thread, Throwable ex) {
				Log.w("UnexpectedTerminationHelper", "uncaughtException", ex);
				CameraManager.releaseCamera();
				if (mOldUncaughtExceptionHandler != null) {
					// it displays the "force close" dialog
					mOldUncaughtExceptionHandler.uncaughtException(thread, ex);
				}
			}
		};

		void init() {
			mThread = Thread.currentThread();
			mOldUncaughtExceptionHandler = mThread.getUncaughtExceptionHandler();
			mThread.setUncaughtExceptionHandler(mUncaughtExceptionHandler);
		}

		void finish() {
			mThread.setUncaughtExceptionHandler(mOldUncaughtExceptionHandler);
			mOldUncaughtExceptionHandler = null;
			mThread = null;
		}
	}

	public void play(String friendId) {
		Friend f = (Friend) FriendFactory.getFactoryInstance().find(friendId);
		
		if (f == null)
			throw new RuntimeException("Play from notification found no friendId: " + friendId);
		
		GridManager.moveFriendToGrid(getActivity(), f);
		int index = GridElementFactory.getFactoryInstance().gridElementIndexWithFriend(f);
		
		if (index == -1)
			throw new RuntimeException("Play from notification found not grid element index for friendId: " + friendId);
		
		View view = nineViewGroup.getSurroundingFrame(index);
		videoPlayer.playOverView(view, friendId);
	}

	// -------------------
	// HandleIntentAction
	// -------------------
	private void handleIntentAction(Intent currentIntent) {
		// Right now the only actions are
		// 1) to automatically start playing the appropriate video if the user
		// got here by clicking a notification.
		// 2) to notify the user if there was a problem sending Sms invite to a
		// friend. (Not used as decided this is unnecessarily disruptive)
		if (currentIntent == null) {
			Log.i(TAG, "handleIntentAction: no intent. Exiting.");
			return;
		}

		Log.i(TAG, "handleIntentAction: " + currentIntent.toString());

		String action = currentIntent.getAction();
		Uri data = currentIntent.getData();
		if (action == null || data == null) {
			Log.i(TAG, "handleIntentAction: no ation or data. Exiting.");
			return;
		}

		String friendId = currentIntent.getData().getQueryParameter(IntentHandler.IntentParamKeys.FRIEND_ID);
		if (action == null || friendId == null) {
			Log.i(TAG, "handleIntentAction: no friendId or action. Exiting." + currentIntent.toString());
			return;
		}

		if (action.equals(IntentHandler.IntentActions.PLAY_VIDEO)) {
			currentIntent.setAction(IntentHandler.IntentActions.NONE);
			play(friendId);
		}

		// Not used as I decided pending intent coming back from sending sms is
		// to disruptive. Just assume
		// sms's sent go through.
		if (action.equals(IntentHandler.IntentActions.SMS_RESULT)) {
			currentIntent.setAction(IntentHandler.IntentActions.NONE);
			Log.i(TAG, currentIntent.toString());
		}
	}

    @Override
    public void onBenchRequest() {
        callbacks.onBenchRequest();
    }

    @Override
    public void onNudgeFriend(Friend f) {
        callbacks.onNudgeFriend(f);
    }

    @Override
    public void onRecordDialogRequested() {
        callbacks.showRecordDialog();
    }

    @Override
    public void onVideoPlaying(String friendId, String videoId) {   }

    @Override
    public void onVideoStopPlaying() {    }

    @Override
    public void onFileDownloading() {
        showToast("Downloading...");
    }

    @Override
    public void onFileDownloadingRetry() {
        FileDownloadService.restartTransfersPendingRetry(getActivity());
        callbacks.showBadConnectionDialog();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        AudioManager am = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        if (event.values[0] == 0) {
            am.setMode(AudioManager.MODE_IN_CALL);
            am.setSpeakerphoneOn(false);
        } else {
            am.setMode(AudioManager.MODE_NORMAL);
            am.setSpeakerphoneOn(true);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private class NineViewGestureListener implements NineViewGroup.GestureListener {
        @Override
        public boolean onClick(NineViewGroup parent, View view, int position, long id) {
            Log.d(TAG, "onItemClick: " + position + ", " + id);
            if (id == -1)
                return false;
            GridElement gridElement = GridElementFactory.getFactoryInstance().get(position);
            String friendId = gridElement.getFriendId();
            if (friendId != null && !friendId.equals("")) {
                videoPlayer.playOverView(view, friendId);
            } else {
                callbacks.onBenchRequest();
            }
            return true;
        }

        @Override
        public boolean onStartLongpress(NineViewGroup parent, View view, int position, long id) {
            Log.d(TAG, "onItemLongClick: " + position + ", " + id);
            if (id == -1)
                return false;

            GridElement ge = GridElementFactory.getFactoryInstance().get(position);
            String friendId = ge.getFriendId();
            if (friendId != null && !friendId.equals("")) {
                Logger.d("START RECORD");
                onRecordStart(friendId);
            }

            return true;
        }

        @Override
        public boolean onEndLongpress() {
            Logger.d("STOP RECORD");
            onRecordStop();
            return false;
        }

        @Override
        public boolean onCancelLongpress() {
            Log.d(getTag(), "onCancelTouch");
            showToast("Dragged Finger Away.");
            Logger.d("STOP RECORD");
            onRecordCancel();
            return false;
        }

        @Override
        public boolean onCancelLongpress(String reason) {
            showToast(reason);
            onRecordCancel();
            return false;
        }
    }
}
