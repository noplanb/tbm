package com.zazoapp.client.model;

import com.zazoapp.client.core.MessageType;

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
        ArrayList<OutgoingMessage> messages = allWithFriendId(friendId);
        for (OutgoingMessage message : messages) {
            if (message.isSent() || message.getStatus() == OutgoingMessage.Status.FAILED_PERMANENTLY) {
                Friend friend = FriendFactory.getFactoryInstance().find(friendId);
                if (friend != null) {
                    if (MessageType.VIDEO.is(message.getType())) {
                        Friend.File.OUT_VIDEO.delete(friend, message.getId());
                    } else if (MessageType.TEXT.is(message.getType())) {
                        Friend.File.OUT_TEXT.delete(friend, message.getId());
                    }
                }
                delete(message.getId());
            }
        }
    }

}
