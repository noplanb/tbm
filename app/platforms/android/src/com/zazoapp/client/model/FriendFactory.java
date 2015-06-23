package com.zazoapp.client.model;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.model.Friend.VideoStatusChangedCallback;

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
        
        public static final String HAS_APP_TRUE_VALUE = "true";
        public static final String HAS_APP_FALSE_VALUE = "false";
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
     * @param remoteHasApp true if on remote server it marks as has app
     * @return Returns a friend only one was found and changed. Otherwise returns null.
     */
    private Friend updateWithServerParams(Context context, Friend friend, boolean remoteHasApp){
        if (friend != null){
            if (friend.hasApp() ^ remoteHasApp){
                friend.setHasApp(remoteHasApp);
                GridManager.getInstance().rankingActionOccurred(friend);
                notifyStatusChanged(friend);
                return friend;
            }
        }
        return null;
    }

    /**
     * 
     * @param context
     * @param params (server params)
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
        GridManager.getInstance().rankingActionOccurred(f);
        notifyStatusChanged(f);
        return f;
    }

    public Friend getExistingFriend(LinkedTreeMap<String, String> params) {
        return find(params.get(ServerParamKeys.ID).toString());
    }

    public void reconcileFriends(Context context, final List<LinkedTreeMap<String, String>> remoteFriends) {
        notifyOnChanged(false);
        boolean needToNotify = false;
        for (LinkedTreeMap<String, String> friendParams : remoteFriends) {
            Friend f = getFriendFromMkey(friendParams.get(ServerParamKeys.MKEY));
            if (f != null) {
                f = updateWithServerParams(context, f, servHasApp(friendParams));
            } else {
                f = createWithServerParams(context, friendParams);
            }
            // if friend was updated or created then move him to grid
            if (f != null) {
                GridManager.getInstance().moveFriendToGrid(f);
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
    
}
