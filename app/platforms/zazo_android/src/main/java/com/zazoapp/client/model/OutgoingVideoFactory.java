package com.zazoapp.client.model;

import java.util.ArrayList;

/**
 * Created by skamenkovych@codeminders.com on 5/29/2015.
 */
public class OutgoingVideoFactory extends ActiveModelFactory<OutgoingVideo>  {

    private static OutgoingVideoFactory instance = null;

    public static OutgoingVideoFactory getFactoryInstance() {
        if (instance == null)
            instance = new OutgoingVideoFactory();
        return instance;
    }

    @Override
    public Class<OutgoingVideo> getModelClass() {
        return OutgoingVideo.class;
    }

    public ArrayList<OutgoingVideo> allWithFriendId(String friendId) {
        return allWhere(Video.Attributes.FRIEND_ID, friendId);
    }

    public void deleteAllSent(String friendId) {
        ArrayList<OutgoingVideo> videos = allWithFriendId(friendId);
        for (OutgoingVideo video : videos) {
            if (video.isSent() || video.getVideoStatus() == OutgoingVideo.Status.FAILED_PERMANENTLY) {
                delete(video.getId());
            }
        }
    }

}
