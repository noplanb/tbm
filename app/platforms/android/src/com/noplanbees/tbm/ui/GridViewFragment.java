package com.noplanbees.tbm.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Fragment;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.VideoView;

import com.noplanbees.tbm.ActiveModelsHandler;
import com.noplanbees.tbm.Friend;
import com.noplanbees.tbm.GridElement;
import com.noplanbees.tbm.GridManager;
import com.noplanbees.tbm.GridManager.GridEventNotificationDelegate;
import com.noplanbees.tbm.R;
import com.noplanbees.tbm.VideoPlayer;
import com.noplanbees.tbm.ui.CustomAdapterView.OnItemTouchListener;
import com.noplanbees.tbm.utilities.CameraHelper;
import com.noplanbees.tbm.utilities.Logger;

public class GridViewFragment extends Fragment implements GridEventNotificationDelegate {

	private static final String TAG = "GridViewFragment";

	private Camera camera;

	private MediaRecorder mediaRecorder;
	private boolean isRecording;
	private int currentItemPlayed = -1;
	
	private CustomAdapterView gridView;
	private List<FriendStub> list;

	private FriendsAdapter adapter;


	private ActiveModelsHandler activeModelsHandler;

	private VideoPlayer videoPlayer;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		activeModelsHandler = ActiveModelsHandler.getActiveModelsHandler();
		
		videoPlayer = new VideoPlayer(getActivity());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.video_gridview_fragment, null);
		
		View videoBody = v.findViewById(R.id.video_body);
		VideoView videoView = (VideoView)v.findViewById(R.id.video_view);
		
		videoPlayer.setVideoView(videoView);
		videoPlayer.setVideoViewBody(videoBody);
	
		gridView = (CustomAdapterView)v.findViewById(R.id.grid_view);
		gridView.setItemClickListener(new OnItemTouchListener() {
			@Override
			public boolean onItemClick(CustomAdapterView parent, View view, int position, long id) {
				if(id == -1)
					return false;
				
				videoPlayer.stop();
				if(currentItemPlayed == position){
					currentItemPlayed = -1;
				}else{
					FriendStub fs = (FriendStub)((FriendsAdapter)parent.getAdapter()).getItem(position);
					
					videoPlayer.setVideoViewSize(view.getX(), view.getY(), view.getWidth(), view.getHeight());
					videoPlayer.play(fs.getFriendId());
					
					currentItemPlayed = position;
				}
				return true;
			}
			@Override
			public boolean onItemLongClick(CustomAdapterView parent, View view, int position, long id) {
				Logger.d("START RECORD");
				if(id == -1)
					return false;

				adapter.setRecording(true);
				new MediaPrepareTask().execute();
				return true;
			}
			@Override
			public boolean onItemStopTouch() {
				if (isRecording) {
					Logger.d("STOP RECORD");
					adapter.setRecording(false);
					// stop recording and release camera
					try{
						mediaRecorder.stop(); // stop the recording
					}catch(RuntimeException e){
						Toast.makeText(getActivity(), "Video is too short", Toast.LENGTH_SHORT).show();
					}
					releaseMediaRecorder(); // release the MediaRecorder object
					camera.lock(); // take camera access back from
									// MediaRecorder

					// inform the user that recording has stopped
					isRecording = false;
				}
				return false;
			}

			@Override
			public boolean onCancelTouch() {
				Log.d(getTag(), "onCancelTouch");
				if (isRecording) {
					Logger.d("STOP RECORD");
					adapter.setRecording(false);
					// stop recording and release camera
					try{
						mediaRecorder.stop(); // stop the recording
					}catch(RuntimeException e){
						Toast.makeText(getActivity(), "Video is too short", Toast.LENGTH_SHORT).show();
					}
					releaseMediaRecorder(); // release the MediaRecorder object
					camera.lock(); // take camera access back from
									// MediaRecorder

					// inform the user that recording has stopped
					isRecording = false;
				}
				return false;
			}
		});
		return v;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		setupGrid();
		
		list = new ArrayList<FriendStub>(8);
		
		
		for (GridElement ge : activeModelsHandler.getGf().all()) {
			FriendStub stub = new FriendStub();
			Friend f = ge.friend();
			if (f != null) {
				stub.setFriendId(f.getId());
				stub.setName(f.getStatusString());
				stub.setNotViewed(f.incomingVideoNotViewed());
				if (!f.thumbExists() ){
					Log.i(TAG, "loadThumb: Loading icon for thumb for friend=" + f.get(Friend.Attributes.FIRST_NAME));
					stub.setThumb(BitmapFactory.decodeResource(getResources(),R.drawable.head));
				}else{
					Log.i(TAG, "loadThumb: Loading bitmap for friend=" + f.get(Friend.Attributes.FIRST_NAME));
					stub.setThumb(f.lastThumbBitmap());
				}
			}
			list.add(stub);
		}
		
		adapter = new FriendsAdapter(getActivity(), list);
		gridView.setAdapter(adapter);
	}

	@Override
	public void onResume() {
		super.onResume();
		// Open the default i.e. the first rear facing camera.
		camera = CameraHelper.getDefaultFrontFacingCameraInstance();
		if (camera == null)
			camera = CameraHelper.getDefaultCameraInstance();
		adapter.setCamera(camera);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (isRecording) {
			mediaRecorder.stop();
			releaseMediaRecorder();
			camera.lock();
		}
		// Because the Camera object is a shared resource, it's very
		// important to release it when the activity is paused.
		if (camera != null) {
			adapter.setCamera(null);
			camera.release();
			camera = null;
		}
	}
	
	void prepareRecorder() {
		CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
		Camera.Parameters parameters = camera.getParameters();

		List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
		Camera.Size optimalSize = CameraHelper.getOptimalPreviewSize(mSupportedPreviewSizes, adapter.getPreviewWidth(),
				adapter.getPreviewHeight());

		// Use the same size for recording profile.
		profile.videoFrameWidth = optimalSize.width;
		profile.videoFrameHeight = optimalSize.height;

		// likewise for the camera object itself.
		parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);

		camera.setParameters(parameters);

		// BEGIN_INCLUDE (configure_media_recorder)
		mediaRecorder = new MediaRecorder();

		// Step 1: Unlock and set camera to MediaRecorder
		camera.startPreview();
		camera.unlock();
		mediaRecorder.setCamera(camera);

		// Step 2: Set sources
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		// Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
		mediaRecorder.setProfile(profile);

		// Step 4: Set output file
		mediaRecorder.setOutputFile(CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO).toString());
		// END_INCLUDE (configure_media_recorder)

		mediaRecorder.setOnInfoListener(new OnInfoListener() {

			@Override
			public void onInfo(MediaRecorder mr, int what, int extra) {
				Logger.d("what = " + what + ", extra = " + extra);
			}
		});
		mediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
			public void onError(MediaRecorder mr, int what, int extra) {
				Logger.d("MediaRecorder error: " + what + " extra: " + extra);
			}
		});

		mediaRecorder.setPreviewDisplay(adapter.getPreviewSurface());
		mediaRecorder.setOrientationHint(90);

		// Step 5: Prepare configured MediaRecorder
		try {
			mediaRecorder.prepare();
		} catch (IllegalStateException e) {
			Logger.d("IllegalStateException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
		} catch (IOException e) {
			Logger.d("IOException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
		}
	}

	private void releaseMediaRecorder() {
		if (mediaRecorder != null) {
			// clear recorder configuration
			mediaRecorder.reset();
			// release the recorder object
			mediaRecorder.release();
			mediaRecorder = null;
			// Lock camera for later use i.e taking it back from MediaRecorder.
			// MediaRecorder doesn't need it anymore and we will release it if
			// the activity pauses.
			camera.lock();
		}
	}

	/**
	 * Asynchronous task for preparing the {@link android.media.MediaRecorder}
	 * since it's a long blocking operation.
	 */
	class MediaPrepareTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... voids) {
			prepareRecorder();
			mediaRecorder.start();
			isRecording = true;
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
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
		// TODO Auto-generated method stub
		
	}
		
}
