package com.zazoapp.client;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.zazoapp.client.RemoteStorageHandler.StatusEnum;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.Video;
import com.zazoapp.client.network.FileTransferService;

import java.util.ArrayList;
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
            }
            @Override
            public void gotVideoIds(ArrayList<String> videoIds) {
                Log.i(TAG, "gotVideoIds: " + getFriend().get(Friend.Attributes.FIRST_NAME) + ": " + videoIds);
                handleVideoIds(getFriend(), videoIds);
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
                Log.i(TAG, "Got video status: " + getFriend().getUniqueName() + ": vId:" + videoId + " sts: " + status );
                if (videoId == null)
                    return;

                if(!getFriend().getOutgoingVideoId().equals(videoId)){
                    Log.i(TAG, "gotVideoIdStatus: got status for "  + getFriend().get(Friend.Attributes.FIRST_NAME) + " for non current videoId. Ignoring");
                    return;
                }

                if(status.equals(StatusEnum.DOWNLOADED))
                    getFriend().setAndNotifyOutgoingVideoStatus(Friend.OutgoingVideoStatus.DOWNLOADED);
                else if(status.equals(StatusEnum.VIEWED))
                    getFriend().setAndNotifyOutgoingVideoStatus(Friend.OutgoingVideoStatus.VIEWED);
            }
        }

    }

