package com.zazoapp.client.core;

import android.text.TextUtils;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.IncomingMessage;
import com.zazoapp.client.model.IncomingMessageFactory;
import com.zazoapp.client.model.OutgoingMessage;
import com.zazoapp.client.network.HttpRequest;

import java.util.ArrayList;
// Polls for new videos and schedules downloads.
public class Poller {

    private static final String TAG = Poller.class.getSimpleName();

    //-----------------
    // Public Interface
    //-----------------
    public void pollAll() {
        new GetAllRemoteMessages();
    }

    //----------------
    // Private helpers
    //----------------

    static class MessagesData {
        ArrayList<FriendMessageData> data;
    }

    static class FriendMessageData {
        String mkey;
        ArrayList<MessageInfo> messages;
        ArrayList<StatusInfo> statuses;
    }

    static class MessageInfo {
        String type;
        @SerializedName("message_id") String messageId;
        String body;
    }

    static class StatusInfo {
        String type;
        @SerializedName("message_id") String messageId;
        String status;
    }

    public class GetAllRemoteMessages {

        public GetAllRemoteMessages() {
            new HttpRequest("api/v1/messages", new HttpRequest.Callbacks() {
                @Override
                public void success(String response) {
                    Gson g = new Gson();
                    MessagesData data = null;
                    try {
                        data = g.fromJson(response, MessagesData.class);
                        if (data != null) {
                            gotRemoteKVs(data);
                        }
                    } catch (JsonSyntaxException e) {
                        error("JsonSyntaxException");
                    }
                }

                @Override
                public void error(String errorString) {
                    Log.e(TAG, "GetAllRemoteMessages: " + errorString);
                }
            });
        }

        private void gotRemoteKVs(MessagesData info) {
            if (info.data != null) {
                for (FriendMessageData messageInfo : info.data) {
                    if (messageInfo.mkey != null) {
                        if (messageInfo.messages != null) {
                            gotVideoIds(messageInfo.mkey, messageInfo.messages);
                        }
                        if (messageInfo.statuses != null) {
                            gotVideoIdStatus(messageInfo.mkey, messageInfo.statuses);
                        }
                    }
                }
            }
        }

        protected void gotVideoIds(String mkey, ArrayList<MessageInfo> messages) {
            Friend friend = FriendFactory.getFriendFromMkey(mkey);
            if (friend == null) {
                return;
            }
            handleIncomingMessages(friend, messages);
        }

        protected void gotVideoIdStatus(String mkey, ArrayList<StatusInfo> statuses) {
            for (StatusInfo info : statuses) {
                if (TextUtils.isEmpty(info.messageId) || TextUtils.isEmpty(info.status)) {
                    return;
                }
                Friend friend = FriendFactory.getFriendFromMkey(mkey);
                handleOutgoingMessageStatus(friend, info.messageId, info.status);
            }
        }
    }

    private void handleIncomingMessages(Friend friend, ArrayList<MessageInfo> messageInfos) {
        for (MessageInfo messageInfo : messageInfos) {
            IncomingMessage message = IncomingMessageFactory.getFactoryInstance().find(messageInfo.messageId);
            if (message == null) {
                friend.requestDownload(messageInfo.messageId, messageInfo.type, messageInfo.body);
            } else if (message.isDownloaded() || message.isFailed()) {
                message.deleteFromRemote();
            }
        }
    }

    private void handleOutgoingMessageStatus(Friend friend, String videoId, String status) {
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

