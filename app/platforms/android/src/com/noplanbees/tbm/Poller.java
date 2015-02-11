package com.noplanbees.tbm;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.noplanbees.tbm.RemoteStorageHandler.StatusEnum;
import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.FriendFactory;
import com.noplanbees.tbm.model.Video;
import com.noplanbees.tbm.network.FileTransferService;
// Polls for new videos and schedules downloads.
public class Poller {

        private static String TAG = Poller.class.getSimpleName();
        private Context context;

        //-----------------
        // Public Interface
        //-----------------
        public Poller(Context context){
            this.context = context;
        }

        public void pollAll(){
            for (Friend f : FriendFactory.getFactoryInstance().all()){
                poll(f);
            }
        }

        public void poll(Friend friend){
            new GetRemoteVideoIds(friend);

            if(friend.getOutgoingVideoStatus()!=Friend.OutgoingVideoStatus.VIEWED)
                new GetVideoStatus(friend);
        }

        //----------------
       // Private helpers
        //----------------
        private class GetRemoteVideoIds extends RemoteStorageHandler.GetRemoteIncomingVideoIds {
            public GetRemoteVideoIds(Friend friend) {
                super(friend);
                Log.d(TAG, "GetRemoteVideoIds for: " + friend.get(Friend.Attributes.FIRST_NAME));
                Log.d(TAG, "GetRemoteVideoIds for: " + this.friend.get(Friend.Attributes.FIRST_NAME));
            }
            @Override
            public void gotVideoIds(ArrayList<String> videoIds) {
                Log.i(TAG, "gotVideoIds: " + friend.get(Friend.Attributes.FIRST_NAME) + ": " + videoIds);
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

        private class GetVideoStatus extends RemoteStorageHandler.GetRemoteOutgoingVideoStatus{
            public GetVideoStatus(Friend friend) {
                super(friend);
            }

            @Override
            protected void gotVideoIdStatus(String videoId, String status) {
                Log.i(TAG, "Got video status: " + friend.get(Friend.Attributes.FIRST_NAME) + ": vId:" + videoId + " sts: " + status );
                if (videoId == null)
                    return;
                
                if(friend.get(Friend.Attributes.OUTGOING_VIDEO_ID).equals(videoId)){
                    Log.i(TAG, "gotVideoIdStatus: got status for "  + friend.get(Friend.Attributes.FIRST_NAME) + " for non current videoId. Ignoring");
                    return;
                }
                
                if(status.equals(StatusEnum.DOWNLOADED))
                    friend.setAndNotifyOutgoingVideoStatus(Friend.OutgoingVideoStatus.DOWNLOADED);
                else if(status.equals(StatusEnum.VIEWED))
                    friend.setAndNotifyOutgoingVideoStatus(Friend.OutgoingVideoStatus.VIEWED);
            }
        }


    }

