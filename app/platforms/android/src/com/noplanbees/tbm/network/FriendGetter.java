package com.noplanbees.tbm.network;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.noplanbees.tbm.Config;
import com.noplanbees.tbm.DataHolderService;
import com.noplanbees.tbm.GridManager;
import com.noplanbees.tbm.RemoteStorageHandler;
import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.FriendFactory;
import com.noplanbees.tbm.model.Video;
import com.noplanbees.tbm.notification.NotificationHandler;
import com.noplanbees.tbm.ui.MainActivity;

import java.util.ArrayList;
import java.util.List;

public class FriendGetter {
	
	private final String TAG = getClass().getSimpleName();
	
	private Context context;
	private boolean destroyAll;
	private FriendGetterCallback delegate;
	private List<LinkedTreeMap<String, String>> friendList = new ArrayList<LinkedTreeMap<String, String>>();
	
	public interface FriendGetterCallback{
		public void gotFriends();
	}
	
	public FriendGetter(Context c, boolean destroyAll, FriendGetterCallback delegate){
		context = c;
		this.destroyAll = destroyAll;
		this.delegate = delegate;
		getFriends();
	}
	
	private void getFriends(){
		LinkedTreeMap<String, String>params = new LinkedTreeMap<String, String>();
		String uri = new Uri.Builder().appendPath("reg").appendPath("get_friends").build().toString();
		new GetFriends(uri, params);
	}

	class GetFriends extends Server {
        private final ProgressDialog pd;

        public GetFriends(String uri, LinkedTreeMap<String, String>params){
			super(uri, params);
//            pd = ProgressDialog.show(context, "Checking", null);
            pd = null;
		}

		@Override
		public void success(String response) {	
			gotFriends(context, response);
            if(pd!=null)
                pd.dismiss();
		}
		@Override
		public void error(String errorString) {
			serverError();
            if(pd!=null)
                pd.dismiss();
		}
	}

	@SuppressWarnings("unchecked")
	public void gotFriends(Context context, String r) {
        Log.i(TAG, "gotRegResponse: " + r);
		Gson g = new Gson();
		friendList = g.fromJson(r, friendList.getClass());
		Log.i(TAG, "gotRegResponse: " + friendList.toString());
		
		if (destroyAll)
			FriendFactory.getFactoryInstance().destroyAll(context);

        GridManager gm = GridManager.getInstance();
		for (LinkedTreeMap<String, String> fm : friendList){
            Friend f = FriendFactory.getFactoryInstance().createFriendFromServerParams(context, fm);
            if(f !=null)
                gm.moveFriendToGrid(context, f);
		}
        new Poller(context).pollAll();
        if(delegate!=null)
		    delegate.gotFriends();
	}
	
	private void serverError(){
		showErrorDialog("No Connection", "Can't reach " + Config.appName + ".\n\nCheck your connection and try again.");
	}

	private void showErrorDialog(String title, String message){
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title)
		.setMessage(message)
		.setPositiveButton("Ok", null)
		.create().show();
	}

    // Polls for new videos and schedules downloads.
    public static class Poller {

        private static String TAG = Poller.class.getSimpleName();
        private Context context;

        public Poller(Context context){
            this.context = context;
        }

        public void pollAll(){
            for (Friend f : FriendFactory.getFactoryInstance().all()){
                poll(f);
            }
        }

        public void poll(Friend friend){
            Log.i(TAG, "poll: " + friend.get(Friend.Attributes.FIRST_NAME));
            new GetRemoteVideoIds(new RemoteStorageHandler(), friend);
        }

        private class GetRemoteVideoIds extends RemoteStorageHandler.GetRemoteIncomingVideoIds {
            public GetRemoteVideoIds(RemoteStorageHandler remoteStoragehander, Friend friend) {
                remoteStoragehander.super(friend);
            }
            @Override
            public void gotVideoIds(ArrayList<String> videoIds) {
                Log.i(TAG, "gotVideoIds: " + videoIds);
                handleVideoIds(friend, videoIds);
            }
        }

        public void handleVideoIds(Friend friend, ArrayList<String> videoIds) {
            for (String videoId : videoIds){
                Intent intent = new Intent();

                intent.putExtra(FileTransferService.IntentFields.TRANSFER_TYPE_KEY, FileTransferService.IntentFields.TRANSFER_TYPE_DOWNLOAD);
                intent.putExtra(FileTransferService.IntentFields.STATUS_KEY, Video.IncomingVideoStatus.NEW);
                intent.putExtra(FileTransferService.IntentFields.VIDEO_ID_KEY, videoId);
                intent.putExtra("friendId", friend.getId());
                intent.setClass(context, DataHolderService.class);

                context.startService(intent);
            }
        }

    }
}
