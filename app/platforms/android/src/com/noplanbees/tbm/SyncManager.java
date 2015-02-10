package com.noplanbees.tbm;

import android.content.Context;
import android.util.Log;

public class SyncManager {
    private static String TAG = SyncManager.class.getSimpleName();
    private Context context;
    
    public SyncManager(Context c){
        context = c;
    }
    
    public void getAndPollAllFriends(){
        Log.d(TAG, "getAndPollAllFriends");
        new SyncFriendGetter(context, false).getFriends();
    }
    
    private class SyncFriendGetter extends FriendGetter{
        public SyncFriendGetter(Context c, boolean destroyAll) {
            super(c, destroyAll);
        }
        
        @Override
        protected void success() {
            new Poller(context).pollAll();
        }

        @Override
        protected void failure() {
            new Poller(context).pollAll();
        }

    }

}
