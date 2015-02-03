package com.noplanbees.tbm.model;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.google.gson.internal.LinkedTreeMap;
import com.noplanbees.tbm.model.Friend.VideoStatusChangedCallback;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class FriendFactory extends ActiveModelFactory{
    private final String TAG = getClass().getSimpleName();
    private static final String STAG = FriendFactory.class.getSimpleName();

    public static class ServerParamKeys{
        public static final String ID = "id";
        public static final String FIRST_NAME = "first_name";
        public static final String LAST_NAME = "last_name";
        public static final String MKEY = "mkey";
        public static final String CKEY = "ckey";
        public static final String MOBILE_NUMBER = "mobile_number";
        public static final String HAS_APP = "has_app";
    }

    private static FriendFactory instance = null;

    public static FriendFactory getFactoryInstance(){
        if ( instance == null )
            instance = new FriendFactory();
        return instance;
    }

    @Override
    protected Friend makeInstance(Context context) {
        Friend i = new Friend();
        i.init(context);
        instances.add(i);
        return i;
    }

    public static Friend addFriendFromServerParams(Context context, LinkedTreeMap<String,String>params){
        Log.i(STAG, "addFriendFromServerParams: " + params);
        if (getFactoryInstance().existsWithId(params.get(ServerParamKeys.ID).toString())){
            Log.e(STAG, "ERROR: attempting to add friend with duplicate id. Ignoring.");
            return null;
        }
        Friend f = getFactoryInstance().makeInstance(context);
        f.set(Friend.Attributes.FIRST_NAME, params.get(ServerParamKeys.FIRST_NAME));
        f.set(Friend.Attributes.LAST_NAME, params.get(ServerParamKeys.LAST_NAME));
        f.set(Friend.Attributes.ID, params.get(ServerParamKeys.ID).toString());
        f.set(Friend.Attributes.MKEY, params.get(ServerParamKeys.MKEY));
        f.set(Friend.Attributes.MOBILE_NUMBER, params.get(ServerParamKeys.MOBILE_NUMBER));
        f.set(Friend.Attributes.HAS_APP, params.get(ServerParamKeys.HAS_APP));
        f.set(Friend.Attributes.CKEY, params.get(ServerParamKeys.CKEY));
        return f;
    }

    public static Friend getFriendFromMkey(String mkey){
        return (Friend) getFactoryInstance().findWhere(Friend.Attributes.MKEY, mkey);
    }

    public Friend getFriendFromIntent(Intent intent) {
        Friend f = null;
        Bundle extras = intent.getExtras();
        if (extras != null){
            if ( extras.get("friendId") != null ){
                f = (Friend) find(extras.getString("friendId"));
            } else if ( extras.get("to_mkey") != null ){
                f = getFriendFromMkey(extras.getString("to_mkey"));
            } else if ( extras.get("from_mkey") != null ){
                f = getFriendFromMkey(extras.getString("from_mkey"));
            } else if ( extras.get("receiverId") != null ){
                f = (Friend) find(extras.getString("receiverId"));
            } else if ( extras.get("from_id") != null ){
                f = (Friend) find(extras.getString("from_id"));
            } else if ( extras.get("to_id") != null ){
                f = (Friend) find(extras.getString("to_id"));
            } else if ( extras.get("id") != null ){
                f = (Friend) find(extras.getString("id"));
            }
        }
        return f;
    }

    public ArrayList<Friend> all(){
        ArrayList<Friend> r = new ArrayList<Friend>();
        for (ActiveModel a : instances){
            r.add((Friend) a);
        }
        return r;
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

    public void notifyStatusChanged(Friend f){
        for (VideoStatusChangedCallback observer : videoStatusObservers){
            observer.onVideoStatusChanged(f);
        }
    }


}
