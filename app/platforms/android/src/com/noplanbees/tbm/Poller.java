package com.noplanbees.tbm;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.noplanbees.tbm.RemoteStorageHandler.GetRemoteIncomingVideoIds;

// Polls for new videos and schedules downloads.
public class Poller {
	
	private static String TAG = Poller.class.getSimpleName();
	private HomeActivity activity;
	
	public Poller(HomeActivity activity){
		this.activity = activity;
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
	
	private class GetRemoteVideoIds extends GetRemoteIncomingVideoIds{
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
			Intent intent = new Intent(activity, HomeActivity.class);
			intent.putExtra(FileTransferService.IntentFields.TRANSFER_TYPE_KEY, FileTransferService.IntentFields.TRANSFER_TYPE_DOWNLOAD);
			intent.putExtra(FileTransferService.IntentFields.STATUS_KEY, Video.IncomingVideoStatus.NEW);
			intent.putExtra(FileTransferService.IntentFields.VIDEO_ID_KEY, videoId); 	
			intent.putExtra("friendId", friend.getId());
			(new IntentHandler(activity, intent)).handle();
		}
	}
	
}
