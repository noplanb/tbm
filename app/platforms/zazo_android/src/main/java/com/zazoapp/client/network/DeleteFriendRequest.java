package com.zazoapp.client.network;

import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.model.Friend;

/**
 * Created by skamenkovych@codeminders.com on 8/28/2015.
 */
public class DeleteFriendRequest extends HttpRequest {
    DeleteFriendRequest(LinkedTreeMap<String, String> params, Callbacks callbacks) {
        super("connection/set_visibility", params, HttpRequest.POST, callbacks);
    }

    public static void makeRequest(Friend friend, boolean delete, Callbacks callbacks) {
        LinkedTreeMap<String, String> params = new LinkedTreeMap<>();
        params.put("friend_mkey", friend.getMkey());
        params.put("visibility", delete ? VISIBILITY_HIDDEN : VISIBILITY_VISIBLE);
        new DeleteFriendRequest(params, callbacks);
    }

    public static void makeRequest(final Friend friend, final boolean delete) {
        LinkedTreeMap<String, String> params = new LinkedTreeMap<>();
        params.put("friend_mkey", friend.getMkey());
        params.put("visibility", delete ? VISIBILITY_HIDDEN : VISIBILITY_VISIBLE);
        new DeleteFriendRequest(params, new Callbacks() {
            @Override
            public void success(String response) {
                friend.setDeleted(delete);
            }

            @Override
            public void error(String errorString) {
            }
        });
    }

    private static final String VISIBILITY_HIDDEN = "hidden";
    private static final String VISIBILITY_VISIBLE = "visible";
}
