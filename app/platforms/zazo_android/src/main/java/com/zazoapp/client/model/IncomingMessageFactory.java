package com.zazoapp.client.model;

import java.util.ArrayList;

public class IncomingMessageFactory extends ActiveModelFactory<IncomingMessage> {
    private static IncomingMessageFactory instance = null;

    public static IncomingMessageFactory getFactoryInstance() {
        if (instance == null)
            instance = new IncomingMessageFactory();
        return instance;
    }

    public ArrayList<IncomingMessage> allWithFriendId(String FriendId) {
        return allWhere(Message.Attributes.FRIEND_ID, FriendId);
    }

    public int allNotViewedCount() {
        return allNotViewed().size();
    }

    public ArrayList<IncomingMessage> allNotViewed() {
        return allWhere(Message.Attributes.STATUS, String.valueOf(IncomingMessage.Status.READY_TO_VIEW));
    }

    @Override
    public Class<IncomingMessage> getModelClass() {
        return IncomingMessage.class;
    }

    @Override
    protected boolean checkAndNormalize() {
        return false;
    }
}
