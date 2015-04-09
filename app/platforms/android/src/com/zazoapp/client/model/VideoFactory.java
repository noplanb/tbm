package com.zazoapp.client.model;

import java.util.ArrayList;

public class VideoFactory extends ActiveModelFactory<Video> {
    private static VideoFactory instance = null;

    public static VideoFactory getFactoryInstance() {
        if (instance == null)
            instance = new VideoFactory();
        return instance;
    }

    public ArrayList<Video> allWithFriendId(String FriendId) {
        return allWhere(Video.Attributes.FRIEND_ID, FriendId);
    }

    public int allNotViewedCount() {
        return allWhere(Video.Attributes.STATUS, String.valueOf(Video.IncomingVideoStatus.DOWNLOADED)).size();
    }

    @Override
    public Class<Video> getModelClass() {
        return Video.class;
    }
}
