package com.zazoapp.client.model;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by skamenkovych@codeminders.com on 5/29/2015.
 */
public class OutgoingMessageFactory extends ActiveModelFactory<OutgoingMessage>  {

    private static OutgoingMessageFactory instance = null;

    public static OutgoingMessageFactory getFactoryInstance() {
        if (instance == null)
            instance = new OutgoingMessageFactory();
        return instance;
    }

    @Override
    public Class<OutgoingMessage> getModelClass() {
        return OutgoingMessage.class;
    }

    @Override
    protected boolean checkAndNormalize() {
        return false;
    }

    public ArrayList<OutgoingMessage> allWithFriendId(String friendId) {
        return allWhere(Message.Attributes.FRIEND_ID, friendId);
    }

    public void deleteAllSent(String friendId) {
        ArrayList<OutgoingMessage> videos = allWithFriendId(friendId);
        for (OutgoingMessage video : videos) {
            if (video.isSent() || video.getStatus() == OutgoingMessage.Status.FAILED_PERMANENTLY) {
                Friend friend = FriendFactory.getFactoryInstance().find(friendId);
                if (friend != null) {
                    File videoFile = friend.videoToFile(video.getId());
                    if (videoFile.exists()) {
                        videoFile.delete();
                    }
                }
                delete(video.getId());
            }
        }
    }

}
