package com.noplanbees.tbm.model;

import android.content.Context;


public class GridElement extends ActiveModel {

    static class Attributes {
        public static final String FRIEND_ID = "friendId";
        public static final String ALTERNATIVE_NAME = "alterName";
    }

    @Override
    public String[] attributeList() {
        final String[] a = {
                Attributes.FRIEND_ID,
                Attributes.ALTERNATIVE_NAME,
        };
        return a;
    }

	@Override
	public void init(Context context) {
		super.init(context);
		this.context = context;
	}

	public Friend getFriend(){
		String fid = attributes.get(Attributes.FRIEND_ID);
		if (fid.equals(""))
			return null;
		
		return (Friend) FriendFactory.getFactoryInstance().find(fid);
	}

	public boolean hasFriend(){
		return !attributes.get(GridElement.Attributes.FRIEND_ID).equals("");
	}

    public void setFriend(Friend f, boolean useAlterName) {
        setFriend(f, useAlterName, true);
    }

    public void setFriend(Friend f, boolean useAlterName, boolean notify) {
        final String currentFriendId = getFriendId();
        if (!currentFriendId.equals(f.getId())) {
            set(Attributes.FRIEND_ID, f.getId());
            set(Attributes.ALTERNATIVE_NAME, Boolean.toString(useAlterName));
            if (notify) {
                notifyCallbacks();
            }
        }
    }

	public String getFriendId(){
		return attributes.get(Attributes.FRIEND_ID);
	}

    public boolean shouldUseAlterName() {
        return Boolean.parseBoolean(attributes.get(Attributes.ALTERNATIVE_NAME));
    }
}
