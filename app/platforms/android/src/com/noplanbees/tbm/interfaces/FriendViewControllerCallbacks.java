package com.noplanbees.tbm.interfaces;

import com.noplanbees.tbm.model.Friend;

/**
 * Created by User on 1/30/2015.
 */
public interface FriendViewControllerCallbacks {
    void onBenchRequest(int pos);
    void onNudgeFriend(Friend f);
    void onRecordDialogRequested();
}
