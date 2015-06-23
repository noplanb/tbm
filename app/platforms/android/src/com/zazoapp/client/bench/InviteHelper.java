package com.zazoapp.client.bench;

import com.zazoapp.client.model.Contact;
import com.zazoapp.client.model.Friend;

/**
 * Created by skamenkovych@codeminders.com on 4/21/2015.
 */
public interface InviteHelper {
    void invite(BenchObject bo);
    void invite(Contact contact, int phoneIndex);
    void inviteNewFriend();
    void invite(Contact contact);
    void nudge(Friend f);
    void moveFriendToGrid();
    void finishInvitation();
    void failureNoSimDialog();
    void showSmsDialog();
    void sendInvite(String message);
    Friend getLastInvitedFriend();
    void dropLastInvitedFriend();
}
