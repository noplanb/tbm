package com.noplanbees.tbm.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
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

import com.noplanbees.tbm.ActiveModelsHandler;
import com.noplanbees.tbm.multimedia.CameraManager;
import com.noplanbees.tbm.multimedia.CameraManager.CameraExceptionHandler;
import com.noplanbees.tbm.utilities.Convenience;
import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.Friend.VideoStatusChangedCallback;
import com.noplanbees.tbm.model.GridElement;
import com.noplanbees.tbm.GridManager;
import com.noplanbees.tbm.GridManager.GridEventNotificationDelegate;
import com.noplanbees.tbm.IntentHandler;
import com.noplanbees.tbm.R;
import com.noplanbees.tbm.multimedia.VideoPlayer;
import com.noplanbees.tbm.multimedia.VideoRecorder;
import com.noplanbees.tbm.crash_dispatcher.Dispatch;
import com.noplanbees.tbm.ui.view.FriendView.ClickListener;
import com.noplanbees.tbm.ui.view.NineViewGroup;
import com.noplanbees.tbm.ui.view.NineViewGroup.OnChildLayoutCompleteListener;
import com.noplanbees.tbm.utilities.Logger;

import java.util.ArrayList;

public class GridViewFragment extends Fragment implements GridEventNotificationDelegate,
		VideoRecorder.VideoRecorderExceptionHandler, CameraExceptionHandler, VideoStatusChangedCallback, ClickListener,
VideoPlayer.StatusCallbacks{
	private static final String TAG = "GridViewFragment";

    public interface Callbacks {
		void onFinish();
		void onBenchRequest(int pos);
		void onNudgeFriend(Friend f);
		void showRecordDialog();
        void showBadConnectionDialog();
    }

	private NineViewGroup gridView;
	private FriendsAdapter adapter;
	private ActiveModelsHandler activeModelsHandler;
	private VideoPlayer videoPlayer;
	private VideoRecorder videoRecorder;
	private Callbacks callbacks;
	private Handler handler = new Handler(Looper.getMainLooper());

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(TAG, "" + this);

		activeModelsHandler = ActiveModelsHandler.getActiveModelsHandler();
		activeModelsHandler.getFf().addVideoStatusChangedCallbackDelegate(this);

		videoPlayer = VideoPlayer.getInstance(getActivity());

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
		View v = inflater.inflate(R.layout.nineviewgroup_fragment, container, false);

		View videoBody = v.findViewById(R.id.video_body);
		VideoView videoView = (VideoView) v.findViewById(R.id.video_view);

		videoPlayer.setVideoView(videoView);
		videoPlayer.setVideoViewBody(videoBody);

		gridView = (NineViewGroup) v.findViewById(R.id.grid_view);
		gridView.setItemClickListener(new NineViewGroup.OnItemTouchListener() {
			@Override
			public boolean onItemClick(NineViewGroup parent, View view, int position, long id) {
				Log.d(TAG, "onItemClick: " + position + ", " + id);
				if (id == -1)
					return false;
				GridElement ge = (GridElement) ((FriendsAdapter) parent.getAdapter()).getItem(position);
				String friendId = ge.getFriendId();
				if (friendId != null && !friendId.equals("")) {
					videoPlayer.playAtPos(view.getX(), view.getY(), 
							view.getWidth() + 1, view.getHeight() + 1, friendId);
				} else {
					callbacks.onBenchRequest(position);
				}
				return true;
			}

			@Override
			public boolean onItemLongClick(NineViewGroup parent, View view, int position, long id) {
				Log.d(TAG, "onItemLongClick: " + position + ", " + id);
				if (id == -1)
					return false;

				GridElement ge = (GridElement) ((FriendsAdapter) parent.getAdapter()).getItem(position);
				String friendId = ge.getFriendId();
				if (friendId != null && !friendId.equals("")) {
					Logger.d("START RECORD");
					onRecordStart(friendId);
				}

				return true;
			}

			@Override
			public boolean onItemStopTouch() {
				Logger.d("STOP RECORD");
				onRecordStop();
				return false;
			}

			@Override
			public boolean onCancelTouch() {
				Log.d(getTag(), "onCancelTouch");
				toast("Dragged Finger Away.");
				Logger.d("STOP RECORD");
				onRecordCancel();
				return false;
			}

			@Override
			public boolean onCancelTouch(String reason) {
				toast(reason);
				onRecordCancel();
				return false;
			}
		});

		gridView.setChildLayoutCompleteListener(new OnChildLayoutCompleteListener() {
			@Override
			public void onChildLayoutComplete() {
				handleIntentAction(getActivity().getIntent());
			}
		});

		return v;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		adapter = new FriendsAdapter(getActivity(), activeModelsHandler.getGf().all(), this);
		gridView.setAdapter(adapter);
		gridView.setVideoRecorder(videoRecorder);

		Log.d(TAG, "View created");
	}

	@Override
	public void onResume() {
		Logger.d(TAG, "onResume");
		videoRecorder.onResume();
        videoPlayer.registerStatusCallbacks(this);
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
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mUnexpectedTerminationHelper.finish();
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
		handler.post(new Runnable() {
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
	public void gridDidChange() {
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onVideoStatusChanged(final Friend friend) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (getActivity() != null)
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
				CameraManager.releaseCamera();
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

		void finish() {
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
		// TODO: remove this ugly hack with magic number
		int uiPosForFriendPos = Convenience.getUiPosForFriendPos(i);
		if (uiPosForFriendPos >= 4)
			uiPosForFriendPos++;
		View view = gridView.getChildAt(uiPosForFriendPos);
		videoPlayer.setVideoViewSize(view.getX(), view.getY(), view.getWidth(), view.getHeight());
		videoPlayer.play(friendId);
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
	public void onNudgeClicked(Friend f) {
		callbacks.onNudgeFriend(f);
	}

	@Override
	public void onRecordClicked(Friend f) {
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
        callbacks.showBadConnectionDialog();
    }

}
