package com.zazoapp.client.model;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.google.gson.annotations.SerializedName;
import com.zazoapp.client.model.Friend.VideoStatusChangedCallback;
import com.zazoapp.client.multimedia.VideoIdUtils;
import com.zazoapp.client.utilities.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class FriendFactory extends ActiveModelFactory<Friend> {
    private static final String TAG = FriendFactory.class.getSimpleName();

    public static class ServerParamKeys{
        private static final String ARRAY_SUFFIX = "[]";
        public static final String ID = "id";
        public static final String FIRST_NAME = "first_name";
        public static final String LAST_NAME = "last_name";
        public static final String MKEY = "mkey";
        public static final String CKEY = "ckey";
        public static final String CID = "cid";
        public static final String MOBILE_NUMBER = "mobile_number";
        public static final String DEVICE_PLATFORM = "device_platform";
        public static final String HAS_APP = "has_app";
        public static final String CONNECTION_CREATED_ON = "connection_created_on";
        public static final String CONNECTION_CREATOR_MKEY = "connection_creator_mkey";
        public static final String EMAILS = "emails";
        public static final String EMAILS_ARRAY = EMAILS + ARRAY_SUFFIX;
        public static final String CONNECTION_STATUS = "connection_status";
        public static final String ABILITIES = "abilities";
        public static final String AVATAR = "avatar";

        public static final String HAS_APP_TRUE_VALUE = "true";
        public static final String HAS_APP_FALSE_VALUE = "false";

        public static final String CONNECTION_VOIDED = "voided";
        public static final String CONNECTION_ESTABLISHED = "established";
        public static final String CONNECTION_HIDDEN_BY_CREATOR = "hidden_by_creator";
        public static final String CONNECTION_HIDDEN_BY_TARGET = "hidden_by_target";
        public static final String CONNECTION_HIDDEN_BY_BOTH = "hidden_by_both";
    }

    public static class ServerFriend {
        @SerializedName(ServerParamKeys.ID) public String id;
        @SerializedName(ServerParamKeys.MKEY) public String mkey;
        @SerializedName(ServerParamKeys.FIRST_NAME) public String firstName;
        @SerializedName(ServerParamKeys.LAST_NAME) public String lastName;
        @SerializedName(ServerParamKeys.MOBILE_NUMBER) public String number;
        @SerializedName(ServerParamKeys.DEVICE_PLATFORM) public String platform;
        @SerializedName(ServerParamKeys.EMAILS) public ArrayList<String> emails;
        @SerializedName(ServerParamKeys.HAS_APP) public String hasApp;
        @SerializedName(ServerParamKeys.CKEY) public String ckey;
        @SerializedName(ServerParamKeys.CID) public String cid;
        @SerializedName(ServerParamKeys.CONNECTION_CREATED_ON) public String connectionDate;
        @SerializedName(ServerParamKeys.CONNECTION_CREATOR_MKEY) public String connectionCreator;
        @SerializedName(ServerParamKeys.CONNECTION_STATUS) public String connectionStatus;
        @SerializedName(ServerParamKeys.ABILITIES) public ArrayList<String> abilities;
        @SerializedName(ServerParamKeys.AVATAR) public AvatarData avatar;
    }

    public static class AvatarData {
        @SerializedName("timestamp") public String timestamp;
        @SerializedName("use_as_thumbnail") public String useOption;
    }

    private static FriendFactory instance = null;

    public static FriendFactory getFactoryInstance(){
        if (instance == null )
            instance = new FriendFactory();
        return instance;
    }

    /**
     *
     * @param context context
     * @param friend friend to update
     * @param serverFriend server params
     * @return Returns a friend only one was found and changed. Otherwise returns null.
     */
    private Friend updateWithServerParams(Context context, Friend friend, ServerFriend serverFriend){
        if (friend != null) {
            Friend changedFriend = null;
            if (setConnectionParams(friend, serverFriend)) {
                changedFriend = friend;
            }
            boolean remoteHasApp = servHasApp(serverFriend);
            if (friend.hasApp() ^ remoteHasApp) {
                friend.setHasApp(remoteHasApp);
                friend.setLastActionTime();
                notifyStatusChanged(friend);
                changedFriend = friend;
            }
            updateParam(friend, serverFriend, ServerParamKeys.CID);
            updateParam(friend, serverFriend, ServerParamKeys.ABILITIES);
            setAvatar(friend, serverFriend);
            return changedFriend;
        }
        return null;
    }

    private boolean setConnectionParams(Friend friend, ServerFriend serverFriend) {
        if (serverFriend.connectionCreator != null) {
            friend.setConnectionCreator(isFriendInviter(serverFriend));
            if (serverFriend.connectionStatus != null) {
                boolean newDeleteStatus = friend.isDeleted();
                switch (serverFriend.connectionStatus) {
                    case ServerParamKeys.CONNECTION_VOIDED:
                    case ServerParamKeys.CONNECTION_ESTABLISHED:
                        newDeleteStatus = false;
                        break;
                    case ServerParamKeys.CONNECTION_HIDDEN_BY_CREATOR:
                        newDeleteStatus = !friend.isConnectionCreator();
                        break;
                    case ServerParamKeys.CONNECTION_HIDDEN_BY_TARGET:
                        newDeleteStatus = friend.isConnectionCreator();
                        break;
                    case ServerParamKeys.CONNECTION_HIDDEN_BY_BOTH:
                        newDeleteStatus = true;
                        break;
                }
                if (newDeleteStatus != friend.isDeleted()) {
                    friend.setDeleted(newDeleteStatus);
                    return true;
                }
            }
        }
        return false;
    }

    private void updateParam(Friend friend, ServerFriend serverFriend, String paramName) {
        String friendParam;
        String value;
        switch (paramName) {
            case ServerParamKeys.CID:
                friendParam = Friend.Attributes.CID;
                value = serverFriend.cid;
                break;
            case ServerParamKeys.ABILITIES:
                friendParam = Friend.Attributes.ABILITIES;
                value = serverFriend.abilities != null ? serverFriend.abilities.toString() : "[]";
                break;
            default:
                return;
        }
        if (value == null) {
            return;
        }
        friend.set(friendParam, value);
    }

    private boolean isFriendInviter(ServerFriend friend) {
        return friend.connectionCreator != null && friend.connectionCreator.equals(friend.mkey);
    }

    /**
     * 
     * @param context
     * @param serverFriend server friend data
     * @param notify true to notify callbacks about changes
     * @return Returns a friend only if a new one was created. It only creates a new friend if
     * none was found with the same id as in params. Returns null if friend is already exist.
     */
    public Friend createWithServerParams(Context context, ServerFriend serverFriend, boolean notify){
        Log.i(TAG, "createFriendFromServerParams: " + serverFriend);
        if (existsWithId(serverFriend.id)){
            Log.i(TAG, "ERROR: attempting to add friend with duplicate id. Ignoring.");
            return null;
        }
        Friend f = makeInstance(context);
        f.notifyOnChanged(false);
        f.set(Friend.Attributes.FIRST_NAME, serverFriend.firstName);
        f.set(Friend.Attributes.LAST_NAME, serverFriend.lastName);
        f.set(Friend.Attributes.ID, serverFriend.id);
        f.set(Friend.Attributes.MKEY, serverFriend.mkey);
        f.set(Friend.Attributes.MOBILE_NUMBER, serverFriend.number);
        f.set(Friend.Attributes.CKEY, serverFriend.ckey);
        f.set(Friend.Attributes.CID, serverFriend.cid);
        f.setHasApp(servHasApp(serverFriend));
        updateParam(f, serverFriend, ServerParamKeys.ABILITIES);
        setAvatar(f, serverFriend);
        f.setLastActionTime();
        setConnectionParams(f, serverFriend);
        f.notifyOnChanged(true);
        if (notify) {
            f.notifyCallbacks(true);
            notifyStatusChanged(f);
        }
        return f;
    }

    private void setAvatar(Friend friend, ServerFriend serverFriend) {
        AvatarData avatarData = serverFriend.avatar;
        if (avatarData != null) {
            Avatar.ThumbnailType useOption = friend.getAvatar().getType();
            Avatar.ThumbnailType newUseOption = Avatar.ThumbnailType.getType(avatarData.useOption);
            long newAvatarTimestamp = VideoIdUtils.timeStampFromVideoId(avatarData.timestamp);
            if (useOption != newUseOption && newUseOption == Avatar.ThumbnailType.LAST_FRAME
                    || newAvatarTimestamp == 0) {
                friend.set(Friend.Attributes.USE_AS_THUMBNAIL, Avatar.ThumbnailType.LAST_FRAME.optionName());
                friend.set(Friend.Attributes.AVATAR_TIMESTAMP, String.valueOf(0));
                // While we don't have a thumbnail but have an avatar we are going to use last one
            } else {
                long currentAvatarTimestamp = VideoIdUtils.timeStampFromVideoId(friend.getAvatarTimestamp());
                if (newUseOption == Avatar.ThumbnailType.PHOTO) {
                    if (newAvatarTimestamp > currentAvatarTimestamp) {
                        Avatar.download(friend.getMkey(), avatarData.timestamp, friend);
                    }
                }
            }
        }
    }

    public Friend getExistingFriend(ServerFriend serverFriend) {
        return find(serverFriend.id);
    }

    public void reconcileFriends(Context context, final List<ServerFriend> remoteFriends) {
        notifyOnChanged(false);
        boolean needToNotify = false;
        if (remoteFriends.size() > 0) {
            ServerFriend firstFriend = remoteFriends.get(0);
            UserFactory.current_user().setInvitee(isFriendInviter(firstFriend));
        }

        for (ServerFriend serverFriend : remoteFriends) {
            Friend f = getFriendFromMkey(serverFriend.mkey);
            if (f != null) {
                f = updateWithServerParams(context, f, serverFriend);
            } else {
                f = createWithServerParams(context, serverFriend, true);
            }
            // if friend was updated or created then move him to grid
            if (f != null) {
                if (f.isDeleted()) {
                    GridElement ge = GridElementFactory.getFactoryInstance().findWithFriendId(f.getId());
                    if (ge != null) {
                        GridManager.getInstance().moveNextFriendTo(ge);
                    }
                } else {
                    GridManager.getInstance().moveFriendToGrid(f);
                }
                needToNotify = true;
            }
        }
        notifyOnChanged(true);
        if (needToNotify) {
            notifyCallbacks(ModelChangeType.UPDATED);
        }
    }

    public boolean servHasApp(ServerFriend friend){
        return ServerParamKeys.HAS_APP_TRUE_VALUE.equalsIgnoreCase(friend.hasApp);
    }

    public static Friend getFriendFromMkey(String mkey){
        return getFactoryInstance().findWhere(Friend.Attributes.MKEY, mkey);
    }

    public Friend getFriendFromIntent(Intent intent) {
        Friend f = null;
        Bundle extras = intent.getExtras();
        if (extras != null){
            if ( extras.get("friendId") != null ){
                f = find(extras.getString("friendId"));
            } else if ( extras.get("to_mkey") != null ){
                f = getFriendFromMkey(extras.getString("to_mkey"));
            } else if ( extras.get("from_mkey") != null ){
                f = getFriendFromMkey(extras.getString("from_mkey"));
            } else if ( extras.get("receiverId") != null ){
                f = find(extras.getString("receiverId"));
            } else if ( extras.get("from_id") != null ){
                f = find(extras.getString("from_id"));
            } else if ( extras.get("to_id") != null ){
                f = find(extras.getString("to_id"));
            } else if ( extras.get("id") != null ){
                f = find(extras.getString("id"));
            }
        }
        return f;
    }

    @Override
    public Class<Friend> getModelClass() {
        return Friend.class;
    }

    @Override
    public void destroyAll(Context context) {
        for (ActiveModel a : instances) {
            Friend f = (Friend) a;
            f.deleteAllIncoming();
            f.deleteThumb();
        }

        super.destroyAll(context);
    }

    @Override
    protected boolean checkAndNormalize() {
        boolean result = false;
        ArrayList<Friend> friends = all();
        for (Friend friend : friends) {
            if (!friend.validate()) {
                instances.remove(friend);
                result = true;
            }
        }
        return result;
    }

    //----------------------------
    // VideoStatusChange callbacks
    //----------------------------
    private Set<VideoStatusChangedCallback> videoStatusObservers = new HashSet<>();

    public synchronized void addVideoStatusObserver(VideoStatusChangedCallback observer){
        videoStatusObservers.add(observer);
        Log.i(TAG, "addVideoStatusObserver for " + observer.toString() + " num=" + videoStatusObservers.size());
    }

    public synchronized void removeOnVideoStatusChangedObserver(VideoStatusChangedCallback observer) {
        videoStatusObservers.remove(observer);
    }

    public synchronized void notifyStatusChanged(final Friend f){
        for (final VideoStatusChangedCallback observer : videoStatusObservers){
            observer.onVideoStatusChanged(f);
        }
    }

    public int getInviteeCount() {
        return allWhere(Friend.Attributes.CONNECTION_CREATOR, ActiveModel.FALSE).size();
    }

    public void deleteFriend(Friend friend) {
        if (friend != null) {
            friend.setDeleted(true);
            GridElement ge = GridElementFactory.getFactoryInstance().findWithFriendId(friend.getId());
            if (ge != null) {
                GridManager.getInstance().moveNextFriendTo(ge);
            }
        }
    }

    public ArrayList<Friend> allEnabled() {
        ArrayList<Friend> all = all();
        Iterator<Friend> it = all.iterator();
        while (it.hasNext()) {
            Friend friend = it.next();
            if (friend.isDeleted()) {
                it.remove();
            }
        }
        return all;
    }

    public int getNumberOfEverSentFriends() {
        List<Friend> friends = FriendFactory.getFactoryInstance().all();
        int number = 0;
        for (Friend friend : friends) {
            if (friend.everSent()) {
                number++;
            }
        }
        return number;
    }

    public static class ConnectionComparator implements Comparator<ServerFriend> {
        @Override
        public int compare(ServerFriend lhs, ServerFriend rhs) {
            if (lhs == null && rhs == null) {
                return 0;
            }
            if (lhs == null) {
                return -1;
            }
            if (rhs == null) {
                return 1;
            }
            Date ld = StringUtils.parseTime(lhs.connectionDate);
            Date rd = StringUtils.parseTime(rhs.connectionDate);
            if (ld == null && rd == null) {
                return 0;
            }
            if (ld == null) {
                return -1;
            }
            if (rd == null) {
                return 1;
            }
            return ld.compareTo(rd);
        }
    }
}
