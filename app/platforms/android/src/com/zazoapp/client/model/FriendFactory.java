package com.zazoapp.client.model;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.model.Friend.VideoStatusChangedCallback;
import com.zazoapp.client.utilities.StringUtils;

import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FriendFactory extends ActiveModelFactory<Friend> {
    private static final String TAG = FriendFactory.class.getSimpleName();

    public static class ServerParamKeys{
        public static final String ID = "id";
        public static final String FIRST_NAME = "first_name";
        public static final String LAST_NAME = "last_name";
        public static final String MKEY = "mkey";
        public static final String CKEY = "ckey";
        public static final String MOBILE_NUMBER = "mobile_number";
        public static final String HAS_APP = "has_app";
        public static final String CONNECTION_CREATED_ON = "connection_created_on";
        public static final String CONNECTION_CREATOR_MKEY = "connection_creator_mkey";
        public static final String EMAILS = "emails";
        public static final String CONNECTION_STATUS = "connection_status";

        public static final String HAS_APP_TRUE_VALUE = "true";
        public static final String HAS_APP_FALSE_VALUE = "false";

        public static final String CONNECTION_VOIDED = "voided";
        public static final String CONNECTION_ESTABLISHED = "established";
        public static final String CONNECTION_HIDDEN_BY_CREATOR = "hidden_by_creator";
        public static final String CONNECTION_HIDDEN_BY_TARGET = "hidden_by_target";
        public static final String CONNECTION_HIDDEN_BY_BOTH = "hidden_by_both";
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
     * @param params server params
     * @return Returns a friend only one was found and changed. Otherwise returns null.
     */
    private Friend updateWithServerParams(Context context, Friend friend, LinkedTreeMap<String, String> params){
        if (friend != null) {
            Friend changedFriend = null;
            if (setConnectionParams(friend, params)) {
                changedFriend = friend;
            }
            boolean remoteHasApp = servHasApp(params);
            if (friend.hasApp() ^ remoteHasApp) {
                friend.setHasApp(remoteHasApp);
                friend.setLastActionTime();
                notifyStatusChanged(friend);
                changedFriend = friend;
            }
            return changedFriend;
        }
        return null;
    }

    private boolean setConnectionParams(Friend friend, LinkedTreeMap<String, String> params) {
        if (params.containsKey(ServerParamKeys.CONNECTION_CREATOR_MKEY)) {
            friend.setConnectionCreator(isFriendInviter(params));
            if (params.containsKey(ServerParamKeys.CONNECTION_STATUS)) {
                String status = params.get(ServerParamKeys.CONNECTION_STATUS);
                boolean newDeleteStatus = friend.isDeleted();
                switch (status) {
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

    private boolean isFriendInviter(LinkedTreeMap<String, String> params) {
        return params.get(ServerParamKeys.CONNECTION_CREATOR_MKEY).equals(params.get(ServerParamKeys.MKEY));
    }

    /**
     * 
     * @param context
     * @param params server params
     * @return Returns a friend only if a new one was created. It only creates a new friend if  
     * none was found with the same id as in params. Returns null if friend is already exist.
     */
    public Friend createWithServerParams(Context context, LinkedTreeMap<String, String> params){
        Log.i(TAG, "createFriendFromServerParams: " + params);
        if (existsWithId(params.get(ServerParamKeys.ID).toString())){
            Log.i(TAG, "ERROR: attempting to add friend with duplicate id. Ignoring.");
            return null;
        }
        Friend f = makeInstance(context);
        f.set(Friend.Attributes.FIRST_NAME, params.get(ServerParamKeys.FIRST_NAME));
        f.set(Friend.Attributes.LAST_NAME, params.get(ServerParamKeys.LAST_NAME));
        f.set(Friend.Attributes.ID, params.get(ServerParamKeys.ID).toString());
        f.set(Friend.Attributes.MKEY, params.get(ServerParamKeys.MKEY));
        f.set(Friend.Attributes.MOBILE_NUMBER, params.get(ServerParamKeys.MOBILE_NUMBER));
        f.set(Friend.Attributes.CKEY, params.get(ServerParamKeys.CKEY));
        f.setHasApp(servHasApp(params));
        f.setLastActionTime();
        setConnectionParams(f, params);
        notifyStatusChanged(f);
        return f;
    }

    public Friend getExistingFriend(LinkedTreeMap<String, String> params) {
        return find(params.get(ServerParamKeys.ID).toString());
    }

    public void reconcileFriends(Context context, final List<LinkedTreeMap<String, String>> remoteFriends) {
        notifyOnChanged(false);
        boolean needToNotify = false;
        if (remoteFriends.size() > 0) {
            LinkedTreeMap<String, String> firstFriend = remoteFriends.get(0);
            UserFactory.current_user().setInvitee(isFriendInviter(firstFriend));
        }

        for (LinkedTreeMap<String, String> friendParams : remoteFriends) {
            Friend f = getFriendFromMkey(friendParams.get(ServerParamKeys.MKEY));
            if (f != null) {
                f = updateWithServerParams(context, f, friendParams);
            } else {
                f = createWithServerParams(context, friendParams);
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

    public boolean servHasApp(LinkedTreeMap<String, String> servParams){
        return servParams.get(ServerParamKeys.HAS_APP).equalsIgnoreCase(ServerParamKeys.HAS_APP_TRUE_VALUE);
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
            f.deleteAllVideos();
            f.deleteThumb();
        }

        super.destroyAll(context);
    }

    //----------------------------
    // VideoStatusChange callbacks
    //----------------------------
    private Set<VideoStatusChangedCallback> videoStatusObservers = new HashSet<>();

    public void addVideoStatusObserver(VideoStatusChangedCallback observer){
        videoStatusObservers.add(observer);
        Log.i(TAG, "addVideoStatusObserver for " + observer.toString() + " num=" + videoStatusObservers.size());
    }

    public void removeOnVideoStatusChangedObserver(VideoStatusChangedCallback observer) {
        videoStatusObservers.remove(observer);
    }

    public void notifyStatusChanged(final Friend f){
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

    public static class ConnectionComparator implements Comparator<LinkedTreeMap<String, String>> {
        @Override
        public int compare(LinkedTreeMap<String, String> lhs, LinkedTreeMap<String, String> rhs) {
            Date ld = StringUtils.parseTime(lhs.get(ServerParamKeys.CONNECTION_CREATED_ON));
            Date rd = StringUtils.parseTime(rhs.get(ServerParamKeys.CONNECTION_CREATED_ON));
            return ld.compareTo(rd);
        }
    }
}
