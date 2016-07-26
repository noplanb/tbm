package com.zazoapp.client.core;

import android.text.TextUtils;
import android.util.Log;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.IncomingMessage;
import com.zazoapp.client.model.IncomingMessageFactory;
import com.zazoapp.client.model.OutgoingMessage;

import java.util.ArrayList;
// Polls for new videos and schedules downloads.
public class Poller {

    private static final String TAG = Poller.class.getSimpleName();

    //-----------------
    // Public Interface
    //-----------------
    public void pollAll() {
        new GetAllRemoteVideoIds();
        new GetAllVideoStatuses();
    }

    //----------------
    // Private helpers
    //----------------
    private class GetAllRemoteVideoIds extends RemoteStorageHandler.GetAllRemoteIncomingVideoIds {

        @Override
        public void gotVideoIds(String mkey, ArrayList<String> videoIds) {
            Friend friend = FriendFactory.getFriendFromMkey(mkey);
            if (friend == null) {
                return;
            }
            Log.i(TAG, "gotVideoIds: " + friend.get(Friend.Attributes.FIRST_NAME) + ": " + videoIds);
            handleVideoIds(friend, videoIds);
        }
    }

    private void handleVideoIds(Friend friend, ArrayList<String> videoIds) {
        for (String videoId : videoIds) {
            IncomingMessage video = IncomingMessageFactory.getFactoryInstance().find(videoId);
            if (video == null) {
                friend.requestDownload(videoId);
            } else if (video.isDownloaded() || video.isFailed()) {
                video.deleteFromRemote();
            }
        }
    }

    private class GetAllVideoStatuses extends RemoteStorageHandler.GetAllRemoteOutgoingVideoStatus {

        @Override
        protected void gotVideoIdStatus(String mkey, String videoId, String status) {
            if (TextUtils.isEmpty(videoId) || TextUtils.isEmpty(status)) {
                return;
            }
            Friend friend = FriendFactory.getFriendFromMkey(mkey);
            handleVideoIdStatus(friend, videoId, status);
        }
    }

    private void handleVideoIdStatus(Friend friend, String videoId, String status) {
        if (friend == null || friend.getOutgoingVideoStatus() == OutgoingMessage.Status.VIEWED) {
            return;
        }
        Log.i(TAG, "Got video status: " + friend.getUniqueName() + ": vId:" + videoId + " sts: " + status);
        if (status.equals(RemoteStorageHandler.StatusEnum.DOWNLOADED) || status.equals(RemoteStorageHandler.StatusEnum.VIEWED)) {
            if (!friend.everSent()) {
                friend.setEverSent(true);
                RemoteStorageHandler.setWelcomedFriends();
            }
        }
        if (!friend.getOutgoingVideoId().equals(videoId)) {
            Log.i(TAG, "gotVideoIdStatus: got status for " + friend.get(Friend.Attributes.FIRST_NAME) +
                    " for non current videoId. Ignoring");
            return;
        }

        if (status.equals(RemoteStorageHandler.StatusEnum.DOWNLOADED))
            friend.setAndNotifyOutgoingVideoStatus(videoId, OutgoingMessage.Status.DOWNLOADED);
        else if (status.equals(RemoteStorageHandler.StatusEnum.VIEWED))
            friend.setAndNotifyOutgoingVideoStatus(videoId, OutgoingMessage.Status.VIEWED);
    }
}

