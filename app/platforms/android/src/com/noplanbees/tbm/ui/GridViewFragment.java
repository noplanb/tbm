package com.noplanbees.tbm.ui;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.VideoView;

import com.noplanbees.tbm.ActiveModelsHandler;
import com.noplanbees.tbm.CameraManager;
import com.noplanbees.tbm.CameraManager.CameraExceptionHandler;
import com.noplanbees.tbm.Friend;
import com.noplanbees.tbm.Friend.VideoStatusChangedCallback;
import com.noplanbees.tbm.GridElement;
import com.noplanbees.tbm.GridManager;
import com.noplanbees.tbm.GridManager.GridEventNotificationDelegate;
import com.noplanbees.tbm.IntentHandler;
import com.noplanbees.tbm.R;
import com.noplanbees.tbm.VideoPlayer;
import com.noplanbees.tbm.VideoRecorder;
import com.noplanbees.tbm.ui.CustomAdapterView.OnChildLayoutCompleteListener;
import com.noplanbees.tbm.utilities.Logger;

public class GridViewFragment extends Fragment implements GridEventNotificationDelegate,
		VideoRecorder.VideoRecorderExceptionHandler, CameraExceptionHandler, VideoStatusChangedCallback {

	public interface Callbacks {
		void onFinish();
	}

	private static final String TAG = "GridViewFragment";
	private boolean isRecording;
	private int currentItemPlayed = -1;
	private CustomAdapterView gridView;
	private FriendsAdapter adapter;
	private ActiveModelsHandler activeModelsHandler;
	private VideoPlayer videoPlayer;
	private VideoRecorder videoRecorder;
	private Camera camera;
	private Callbacks callbacks;
	private Handler handler = new Handler(Looper.getMainLooper());

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(TAG, "" + this);

		activeModelsHandler = ActiveModelsHandler.getActiveModelsHandler();
		activeModelsHandler.getFf().addVideoStatusChangedCallbackDelegate(this);

		videoPlayer = new VideoPlayer(getActivity());

		CameraManager.addExceptionHandlerDelegate(this);
		videoRecorder = new VideoRecorder(getActivity());
		videoRecorder.addExceptionHandlerDelegate(this);

		setupGrid();

		mUnexpectedTerminationHelper.init();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		callbacks = (Callbacks) activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.video_gridview_fragment, null);

		View videoBody = v.findViewById(R.id.video_body);
		VideoView videoView = (VideoView) v.findViewById(R.id.video_view);

		videoPlayer.setVideoView(videoView);
		videoPlayer.setVideoViewBody(videoBody);

		gridView = (CustomAdapterView) v.findViewById(R.id.grid_view);
		gridView.setItemClickListener(new CustomAdapterView.OnItemTouchListener() {
			@Override
			public boolean onItemClick(CustomAdapterView parent, View view, int position, long id) {
				if (id == -1)
					return false;

				if (currentItemPlayed == position && videoPlayer.isPlaying()) {
					videoPlayer.stop();
					currentItemPlayed = -1;
				} else {
					GridElement ge = (GridElement) ((FriendsAdapter) parent.getAdapter()).getItem(position);
					String friendId = ge.getFriendId();
					if (friendId != null && !friendId.equals("")) {
						videoPlayer.setVideoViewSize(view.getX(), view.getY(), view.getWidth(), view.getHeight());
						videoPlayer.play(friendId);
						currentItemPlayed = position;
					} else {
						// show bench
					}
				}
				return true;
			}

			@Override
			public boolean onItemLongClick(CustomAdapterView parent, View view, int position, long id) {
				if (id == -1)
					return false;

				GridElement ge = (GridElement) ((FriendsAdapter) parent.getAdapter()).getItem(position);
				String friendId = ge.getFriendId();
				if (friendId != null && !friendId.equals("")) {
					Logger.d("START RECORD");
					onRecordStart(friendId);
					adapter.setRecording(true);
					isRecording = true;
				}

				return true;
			}

			@Override
			public boolean onItemStopTouch() {
				if (isRecording) {
					Logger.d("STOP RECORD");
					adapter.setRecording(false);
					isRecording = false;

					onRecordStop();
				}
				return false;
			}

			@Override
			public boolean onCancelTouch() {
				Log.d(getTag(), "onCancelTouch");
				if (isRecording) {
					Logger.d("STOP RECORD");
					adapter.setRecording(false);
					onRecordCancel();
					isRecording = false;
				}
				return false;
			}
		});
		
		gridView.setChildLayoutCompleteListener(new OnChildLayoutCompleteListener() {
			@Override
			public void onChildLayoutComplete(int childWidth, int childHeight) {
				handleIntentAction(getActivity().getIntent());
			}
		});
		
		Log.d(TAG, "onCreateView");

		return v;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		adapter = new FriendsAdapter(getActivity(), activeModelsHandler.getGf().all());
		gridView.setAdapter(adapter);
		adapter.setListener(new SurfaceTextureListener() {
			@Override
			public void onSurfaceTextureUpdated(SurfaceTexture surface) {
			}

			@Override
			public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
			}

			@Override
			public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
				camera.stopPreview();
				camera.release();
				camera = null;
				Log.d(getTag(), "onSurfaceTextureDestroyed" + camera);
				return true;
			}

			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
				Log.d(getTag(), "onSurfaceTextureAvailable" + camera);

				try {
					camera = CameraManager.getCamera(getActivity());
					camera.setPreviewTexture(surface);
					camera.setDisplayOrientation(90);
					camera.startPreview();
				} catch (IOException ioe) {
					// Something bad happened
				}
			}
		});
		
		
		Log.d(TAG, "View created");
	}

	@Override
	public void onResume() {
		Logger.d(TAG, "onResume");
		super.onResume();
	}

	@Override
	public void onPause() {
		Logger.d(TAG, "onPause");
		super.onPause();
		if (isRecording) {
			videoRecorder.stopRecording();
			camera.lock();
		}
		videoPlayer.release(getActivity());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mUnexpectedTerminationHelper.fini();
	}

	// -------
	// Events
	// -------
	private void onRecordStart(String friendId) {
		videoPlayer.stop();
		GridElement ge = activeModelsHandler.getGf().getGridElementByFriendId(friendId);
		if (!ge.hasFriend())
			return;

		Friend f = ge.friend();
		GridManager.rankingActionOccurred(f);
		if (videoRecorder.startRecording(f)) {
			Log.i(TAG, "onRecordStart: START RECORDING: " + f.get(Friend.Attributes.FIRST_NAME));
		} else {
			Log.e(TAG, "onRecordStart: unable to start recording" + f.get(Friend.Attributes.FIRST_NAME));
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

	private void setupGrid() {
		GridManager.setGridEventNotificationDelegate(this);

		if (activeModelsHandler.getGf().all().size() == 8)
			return;

		activeModelsHandler.getGf().destroyAll(getActivity());
		ArrayList<Friend> allFriends = activeModelsHandler.getFf().all();
		for (Integer i = 0; i < 8; i++) {
			GridElement g = activeModelsHandler.getGf().makeInstance(getActivity());
			if (i < allFriends.size()) {
				Friend f = allFriends.get(i);
				g.set(GridElement.Attributes.FRIEND_ID, f.getId());
			}
		}
	}

	@Override
	public void gridDidChange() {
		adapter.notifyDataSetChanged();
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

	private void showCameraExceptionDialog(String title, String message, String negativeButton, String positiveButton) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(title).setMessage(message)
				.setNegativeButton(negativeButton, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						callbacks.onFinish();
					}
				}).setPositiveButton(positiveButton, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// videoRecorder.dispose();
						// videoRecorder.restore();
						// ????
					}
				});
		AlertDialog alertDialog = builder.create();
		alertDialog.setCanceledOnTouchOutside(false);
		alertDialog.show();
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
		handler.post(new Runnable() {
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
		handler.post(new Runnable() {
			@Override
			public void run() {
				GridManager.moveFriendToGrid(getActivity(), friend);

				adapter.notifyDataSetChanged();
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
				camera.stopPreview();
				camera.release();
				camera = null;
				if (mOldUncaughtExceptionHandler != null) {
					// it displays the "force close" dialog
					mOldUncaughtExceptionHandler.uncaughtException(thread, ex);
				}
				android.os.Process.killProcess(android.os.Process.myPid());
				System.exit(10);
			}
		};

		void init() {
			mThread = Thread.currentThread();
			mOldUncaughtExceptionHandler = mThread.getUncaughtExceptionHandler();
			mThread.setUncaughtExceptionHandler(mUncaughtExceptionHandler);
		}

		void fini() {
			mThread.setUncaughtExceptionHandler(mOldUncaughtExceptionHandler);
			mOldUncaughtExceptionHandler = null;
			mThread = null;
		}
	}

	public void play(String friendId) {
		int i = 0;
		for (GridElement ge : activeModelsHandler.getGf().all()) {
			if (ge.getFriendId().equals(friendId))
				break;
			i++;
		}
		
		View view = gridView.getChildAt(i);
		videoPlayer.setVideoViewSize(view.getX(), view.getY(), view.getWidth(), view.getHeight());
		videoPlayer.play(friendId);
		currentItemPlayed = i;
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
		Log.i(TAG, "handleIntentAction: " + currentIntent .toString());
		if (currentIntent == null) {
			Log.i(TAG, "handleIntentAction: no intent. Exiting.");
			return;
		}
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
}
