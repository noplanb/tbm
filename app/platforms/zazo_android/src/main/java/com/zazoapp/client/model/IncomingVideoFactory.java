package com.zazoapp.client.model;

import java.util.ArrayList;

public class IncomingVideoFactory extends ActiveModelFactory<IncomingVideo> {
    private static IncomingVideoFactory instance = null;

    public static IncomingVideoFactory getFactoryInstance() {
        if (instance == null)
            instance = new IncomingVideoFactory();
        return instance;
    }

    public ArrayList<IncomingVideo> allWithFriendId(String FriendId) {
        return allWhere(Video.Attributes.FRIEND_ID, FriendId);
    }

    public int allNotViewedCount() {
        return allNotViewed().size();
    }

    public ArrayList<IncomingVideo> allNotViewed() {
        return allWhere(Video.Attributes.STATUS, String.valueOf(IncomingVideo.Status.READY_TO_VIEW));
    }

    @Override
    public Class<IncomingVideo> getModelClass() {
        return IncomingVideo.class;
    }

    @Override
    protected boolean checkAndNormalize() {
        return false;
    }
}
