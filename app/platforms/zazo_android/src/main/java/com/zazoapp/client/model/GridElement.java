package com.zazoapp.client.model;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GridElement extends ActiveModel {

    static class Attributes {
        public static final String FRIEND_ID = "friendId";
    }

    public interface GridElementChangedCallback extends ModelChangeCallback {
        void onModelUpdated(boolean changed, boolean onlyMoved);
    }

    @Override
    public List<String> attributeList() {
        final String[] a = {
                Attributes.FRIEND_ID,
        };
        return new ArrayList<>(Arrays.asList(a));
    }

    @Override
    public boolean validate() {
        return true;
    }

    public Friend getFriend() {
        String fid = attributes.get(Attributes.FRIEND_ID);
        if (TextUtils.isEmpty(fid))
            return null;

        return FriendFactory.getFactoryInstance().find(fid);
    }

    public boolean hasFriend() {
        String fid = attributes.get(Attributes.FRIEND_ID);
        return !TextUtils.isEmpty(fid);
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

    @Override
    public void addCallback(ModelChangeCallback callback) {
        if (callback instanceof GridElementChangedCallback) {
            super.addCallback(callback);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    protected void notifyCallbacks(boolean changed, boolean onlyMoved) {
        if (isNotifyOnChanged()) {
            for (ModelChangeCallback callback : callbacks) {
                ((GridElementChangedCallback) callback).onModelUpdated(changed, onlyMoved);
            }
        }
    }
}
