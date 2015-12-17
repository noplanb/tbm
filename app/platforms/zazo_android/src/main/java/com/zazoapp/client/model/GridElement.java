package com.zazoapp.client.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GridElement extends ActiveModel {

    static class Attributes {
        public static final String FRIEND_ID = "friendId";
    }

    @Override
    public List<String> attributeList() {
        final String[] a = {
                Attributes.FRIEND_ID,
        };
        return new ArrayList<>(Arrays.asList(a));
    }

    public Friend getFriend() {
        String fid = attributes.get(Attributes.FRIEND_ID);
        if (fid.equals(""))
            return null;

        return FriendFactory.getFactoryInstance().find(fid);
    }

    public boolean hasFriend() {
        return !attributes.get(GridElement.Attributes.FRIEND_ID).equals("");
    }

    public void setFriend(Friend f) {
        setFriend(f, true);
    }

    public void setFriend(Friend f, boolean notify) {
        final String currentFriendId = getFriendId();
        String newId = (f != null) ? f.getId() : "";
        if (!currentFriendId.equals(newId)) {
            if (notify) {
                set(Attributes.FRIEND_ID, newId);
            } else {
                notifyOnChanged(false);
                set(Attributes.FRIEND_ID, newId);
                notifyOnChanged(true);
            }
        }
    }

    public void notifyUpdate() {
        notifyCallbacks(false);
    }

    public void forceUpdate() {
        notifyCallbacks(true);
    }

    public String getFriendId() {
        return attributes.get(Attributes.FRIEND_ID);
    }

    public boolean isNextEmpty() {
        return this.equals(GridElementFactory.getFactoryInstance().firstEmptyGridElement());
    }
}
