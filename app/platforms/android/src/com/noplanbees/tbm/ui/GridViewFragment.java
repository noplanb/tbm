package com.noplanbees.tbm.ui;

import java.util.ArrayList;

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
import com.noplanbees.tbm.ui.view.GridElementView;
import com.noplanbees.tbm.ui.view.NineViewGroup;
import com.noplanbees.tbm.ui.view.NineViewGroup.LayoutCompleteListener;
import com.noplanbees.tbm.utilities.Convenience;
import com.noplanbees.tbm.utilities.Logger;

public class GridViewFragment extends Fragment implements GridEventNotificationDelegate,
		VideoRecorder.VideoRecorderExceptionHandler, CameraExceptionHandler, VideoStatusChangedCallback,
VideoPlayer.StatusCallbacks, SensorEventListener, GridElementController.Callbacks {
	private static final String TAG = "GridViewFragment";
    private ArrayList<GridElementController> mViewControllers;

    public interface Callbacks {
		void onFinish();
		void onBenchRequest(int pos);
		void onNudgeFriend(Friend f);
		void showRecordDialog();
        void showBadConnectionDialog();
    }

	private NineViewGroup gridView;
	private ActiveModelsHandler activeModelsHandler;
	private VideoPlayer videoPlayer;
	private VideoRecorder videoRecorder;
	private Callbacks callbacks;
	private Handler mainLooper = new Handler(Looper.getMainLooper());

    private SensorManager mSensorManager;
    private Sensor mProximitySensor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(TAG, "onCreate" + this);

		activeModelsHandler = ActiveModelsHandler.getActiveModelsHandler();
		activeModelsHandler.getFf().addVideoStatusChangedCallbackDelegate(this);

		videoPlayer = VideoPlayer.getInstance(getActivity());

		// TODO: Let us have a convention that if initalizing something  requires 3 or more lines of code then 
		// it should be put in its own method. For example. setupVideoRecorder(). That way one can read through
		// and intialization sequence like this and quickly see what is happening at a high level. -- Sani (Serhii please delete this comment when read)
		CameraManager.addExceptionHandlerDelegate(this);
		videoRecorder = new VideoRecorder(getActivity());
		videoRecorder.addExceptionHandlerDelegate(this);

		GridManager.initGrid(getActivity());
		GridManager.setGridEventNotificationDelegate(this);
		
		// TODO: Naming convention: I know that in android and java it is customary to name member variables with mVarirable.
		// However in this project if it is ok I would like to stick to the convention of just using the classname and lowercasing the first
		// letter. For example "private smartClass = new SmartClass()" rather than mSmartClass = new SmartClass. I have to read java, javascript, coffescript, objC, ruby, python
		// etc and it is easier on the eyes to have a common convention across the entire code base. -- Sani (Serhii please deleete this comment when read) 
		mUnexpectedTerminationHelper.init();

        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (mProximitySensor == null) {
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

		gridView = (NineViewGroup) v.findViewById(R.id.grid_view);
		
		// TODO: Convention: I know that it is conventional to to do inline anonymous class declarations. And this is fine.
		// However, if the handler for one or more of the overridden methods in the class ends up being more than 3 lines of code
		// Then let us please capture the action in a method and call that method from the overridden callback. 
		// It is hard on the eyes to review code where the handler is long and complicated as below. -- Sani
		gridView.setGestureListener(new NineViewGroup.GestureListener() {
			@Override
			public boolean onClick(NineViewGroup parent, View view, int position, long id) {
				Log.d(TAG, "onItemClick: " + position + ", " + id);
				if (id == -1)
					return false;
				GridElement ge = GridElementFactory.getFactoryInstance().get(position);
				String friendId = ge.getFriendId();
				if (friendId != null && !friendId.equals("")) {
					videoPlayer.playOverView(view, friendId);
				} else {
					callbacks.onBenchRequest(position);
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
				toast("Dragged Finger Away.");
				Logger.d("STOP RECORD");
				onRecordCancel();
				return false;
			}

			@Override
			public boolean onCancelLongpress(String reason) {
				toast(reason);
				onRecordCancel();
				return false;
			}
		});

		gridView.setChildLayoutCompleteListener(new LayoutCompleteListener() {
			@Override
			public void onLayoutComplete() {
				setupGridElements();
				layoutVideoRecorderPreview();
                handleIntentAction(getActivity().getIntent());
			}
		});

		return v;
	}

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        gridView.setVideoRecorder(videoRecorder);
        Log.d(TAG, "View created");
    }

	@Override
	public void onResume() {
		Logger.d(TAG, "onResume");
		videoRecorder.onResume();
        videoPlayer.registerStatusCallbacks(this);
        mSensorManager.registerListener(this, mProximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
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
        mSensorManager.unregisterListener(this);
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
        	GridElementController gec = new GridElementController(ge, (GridElementView) gridView.getSurroundingFrame(i), GridViewFragment.this);
        	mViewControllers.add(gec);
        	i++;
        }
	}
	
	//---------------------
	// Layout videoRecorder
	//---------------------
	private void layoutVideoRecorderPreview(){
	}
	
	// -------
	// Events
	// -------
	private void onRecordStart(String friendId) {
		videoPlayer.stop();
		GridElement ge = GridElementFactory.getFactoryInstance().getGridElementByFriendId(friendId);
		if (!ge.hasFriend())
			return;

		Friend f = ge.friend();
		GridManager.rankingActionOccurred(f);
		if (videoRecorder.startRecording(f)) {
			Log.i(TAG, "onRecordStart: START RECORDING: " + f.get(Friend.Attributes.FIRST_NAME));
		} else {
			Dispatch.dispatch("onRecordStart: unable to start recording" + f.get(Friend.Attributes.FIRST_NAME));
		}
	}

	private void onRecordCancel() {
		// Different from abortAnyRecording becuase we always toast here.
		videoRecorder.stopRecording();
		toast("Not sent.");
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
		toast("unable to set preview");
	}

	@Override
	public void unableToPrepareMediaRecorder() {
		toast("Unable to prepare MediaRecorder");
	}

	@Override
	public void recordingAborted() {
		toast("Recording Aborted due to Release before Stop.");
	}

	@Override
	public void recordingTooShort() {
		toast("Not sent. Too short.");
	}

	@Override
	public void illegalStateOnStart() {
		toast("Runntime exception on MediaRecorder.start. Quitting app.");
	}

	@Override
	public void runntimeErrorOnStart() {
		toast("Unable to start recording. Try again.");
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

	private void showCameraExceptionDialog(final String title, final String message, final String negativeButton,
			final String positiveButton) {
		mainLooper.post(new Runnable() {
			@Override
			public void run() {
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setTitle(title).setMessage(message)
						.setNegativeButton(negativeButton, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								callbacks.onFinish();
							}
						}).setPositiveButton(positiveButton, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								videoRecorder.dispose();
								videoRecorder.restore();
							}
						});
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

	private void toast(final String msg) {
		mainLooper.post(new Runnable() {
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
		mainLooper.post(new Runnable() {
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
		
		View view = gridView.getSurroundingFrame(index);
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
    public void onBenchRequest(int pos) {
        callbacks.onBenchRequest(pos);
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
        toast("Downloading...");
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
}
