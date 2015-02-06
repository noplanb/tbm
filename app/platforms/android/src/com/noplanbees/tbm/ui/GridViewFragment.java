package com.noplanbees.tbm.ui;

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
import com.noplanbees.tbm.IntentHandler;
import com.noplanbees.tbm.R;
import com.noplanbees.tbm.dispatch.Dispatch;
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
import com.noplanbees.tbm.ui.dialogs.ActionInfoDialogFragment;
import com.noplanbees.tbm.ui.dialogs.InfoDialogFragment;
import com.noplanbees.tbm.ui.view.NineViewGroup;
import com.noplanbees.tbm.ui.view.NineViewGroup.LayoutCompleteListener;
import com.noplanbees.tbm.ui.view.PreviewTextureFrame;
import com.noplanbees.tbm.utilities.Logger;

import java.util.ArrayList;

// TODO: This file is still really ugly and needs to be made more organized and more readable. Some work may need to be factored out. -- Sani

public class GridViewFragment extends Fragment implements VideoRecorder.VideoRecorderExceptionHandler, CameraExceptionHandler, VideoStatusChangedCallback,
        VideoPlayer.StatusCallbacks, SensorEventListener, GridElementController.Callbacks {

	private static final String TAG = GridViewFragment.class.getSimpleName();

    private ArrayList<GridElementController> viewControllers;

    public interface Callbacks {
        void onFinish();
        void onBenchRequest();
        void onGridUpdated();
        void onNudgeFriend(Friend f);
    }

	private NineViewGroup nineViewGroup;
	private ActiveModelsHandler activeModelsHandler;
	private VideoPlayer videoPlayer;
	private VideoRecorder videoRecorder;
	private Handler uiHandler = new Handler(Looper.getMainLooper());

    private SensorManager sensorManager;
    private Sensor proximitySensor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(TAG, "onCreate" + this);

		activeModelsHandler = ActiveModelsHandler.getActiveModelsHandler();
        activeModelsHandler.getFf().addVideoStatusObserver(this);

		videoPlayer = VideoPlayer.getInstance(getActivity());

		CameraManager.addExceptionHandlerDelegate(this);
		videoRecorder = new VideoRecorder(getActivity());
		videoRecorder.addExceptionHandlerDelegate(this);

		mUnexpectedTerminationHelper.init();

        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (proximitySensor == null) {
            Log.i(TAG, "Proximity sensor not found");
        }

        viewControllers = new ArrayList<>(GridManager.GRID_ELEMENTS_COUNT);
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
        activeModelsHandler.getFf().removeOnVideoStatusChangedObserver(this);
		mUnexpectedTerminationHelper.finish();
	}

	//-------------------
	// Setup gridElements
	//-------------------
	private void setupGridElements(){
        if (!viewControllers.isEmpty()) {
            for (GridElementController controller : viewControllers) {
                controller.cleanUp();
            }
            viewControllers.clear();
        }
        int i = 0;
        for (GridElement ge : GridElementFactory.getFactoryInstance().all()){
            GridElementController gec = new GridElementController(getActivity(), ge, nineViewGroup.getSurroundingFrame(i), GridViewFragment.this);
            viewControllers.add(gec);
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
		fl.addView(vrFrame, new PreviewTextureFrame.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
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
		GridManager.getInstance().rankingActionOccurred(f);
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
	public void unableToSetPreview() {
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
	public void runtimeErrorOnStart() {
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
                            getCallbacks().onFinish();
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
    public void onVideoStatusChanged(final Friend friend) {
        if (getActivity() != null) {
            //GridManager.getInstance().moveFriendToGrid(getActivity(), friend);
            notifyViewControllers(new ViewControllerTask() {
                @Override
                public void onEvent(GridElementController controller) {
                    controller.onVideoStatusChanged(friend.getId());
                }
            });
        }
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
		
		//GridManager.getInstance().moveFriendToGrid(getActivity(), f);
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
        getCallbacks().onBenchRequest();
    }

    @Override
    public void onGridUpdated() {
        getCallbacks().onGridUpdated();
    }

    @Override
    public void onNudgeFriend(Friend f) {
        getCallbacks().onNudgeFriend(f);
    }

    @Override
    public void onRecordDialogRequested() {
        // show record dialog
        InfoDialogFragment info = new InfoDialogFragment();
        Bundle args = new Bundle();
        args.putString(InfoDialogFragment.TITLE, "Hold to Record");
        args.putString(InfoDialogFragment.MSG, "Press and hold the RECORD button to record.");
        info.setArguments(args);
        info.show(getFragmentManager(), null);
    }

    @Override
    public void onVideoPlaying(String friendId, String videoId) {}

    @Override
    public void onVideoStopPlaying(String friendId) {}

    @Override
    public void onFileDownloading() {
        showToast("Downloading...");
    }

    @Override
    public void onFileDownloadingRetry() {
        FileDownloadService.restartTransfersPendingRetry(getActivity());

        // show bad connection dialog
        ActionInfoDialogFragment actionDialogFragment = new ActionInfoDialogFragment();
        Bundle args = new Bundle();
        args.putString(ActionInfoDialogFragment.TITLE, "Bad Connection");
        args.putString(ActionInfoDialogFragment.MSG, "Trouble downloading. Check your connection");
        args.putString(ActionInfoDialogFragment.ACTION, "Try Again");
        args.putBoolean(ActionInfoDialogFragment.NEED_CANCEL, false);
        actionDialogFragment.setArguments(args);
        actionDialogFragment.show(getFragmentManager(), null);
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

    private void notifyViewControllers(ViewControllerTask task) {
        for (GridElementController controller : viewControllers) {
            task.onEvent(controller);
        }
    }

    private Callbacks getCallbacks() {
        return (Callbacks) getActivity();
    }

    private interface ViewControllerTask {
        void onEvent(GridElementController controller);
    }

    //------------------------------
    // nineViewGroup Gesture listner
    //------------------------------
    private class NineViewGestureListener implements NineViewGroup.GestureCallbacks {
        @Override
        public boolean onSurroundingClick(View view, int position) {
        	// TODO: Possibly remove all of this from here and start play or show bench from gridElementController -- Sani
            Log.d(TAG, "onSurroundingClick: " + position);

            GridElement gridElement = GridElementFactory.getFactoryInstance().get(position);
            String friendId = gridElement.getFriendId();
            if (friendId != null && !friendId.equals("")) {
                videoPlayer.playOverView(view, friendId);
            } else {
            	// TODO: This is delegated to the gridElementController. But this entire click handler should really be handled there.
            	// We should really only use the NineViewGesture listener for longpress gestures. All clicks should be registerd for 
            	// and handled by the gridViewController.
                getCallbacks().onBenchRequest();
            }
            return true;
        }

        @Override
		public boolean onSurroundingStartLongpress(View view, int position) {
            Log.d(TAG, "onSurroundingStartLongpress: " + position);
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
            Log.d(TAG, "onEndLongpress");
            onRecordStop();
            return false;
        }

        @Override
        public boolean onCancelLongpress(String reason) {
            Log.d(TAG, "onCancelLongpress: " + reason);
            showToast(reason);
            onRecordCancel();
            return false;
        }

		@Override
		public boolean onCenterClick(View view) {
			// TODO: add proper alert
            Log.d(TAG, "onCenterClick");
			return false;
		}

		@Override
		public boolean onCenterStartLongpress(View view) {
			// TODO: add proper alert
            Log.d(TAG, "onCenterStartLongpress");
			return false;
		}
    }
}
