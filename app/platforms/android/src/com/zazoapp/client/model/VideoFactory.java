package com.zazoapp.client.model;

import java.util.ArrayList;

public class VideoFactory extends ActiveModelFactory<IncomingVideo> {
    private static VideoFactory instance = null;

    public static VideoFactory getFactoryInstance() {
        if (instance == null)
            instance = new VideoFactory();
        return instance;
    }

    public ArrayList<IncomingVideo> allWithFriendId(String FriendId) {
        return allWhere(IncomingVideo.Attributes.FRIEND_ID, FriendId);
    }

    public int allNotViewedCount() {
        return allWhere(IncomingVideo.Attributes.STATUS, String.valueOf(IncomingVideo.IncomingVideoStatus.DOWNLOADED)).size();
    }

    @Override
    public Class<IncomingVideo> getModelClass() {
        return IncomingVideo.class;
    }
}
