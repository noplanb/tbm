package com.zazoapp.client;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.network.HttpRequest;

import java.util.ArrayList;
import java.util.List;

public class FriendGetter {
	
	private static final String TAG = FriendGetter.class.getSimpleName();
	
	private Context context;
	private boolean destroyAll;
	private List<LinkedTreeMap<String, String>> friendList = new ArrayList<LinkedTreeMap<String, String>>();
	
	public FriendGetter(Context c, boolean destroyAll){
		context = c;
		this.destroyAll = destroyAll;
	}
	
	// I do not use an interface here because I want to be able to call failure from the 
	// instantiation method of httpRequest.
	protected void success(){};
    protected void failure(){};

	public void getFriends(){
		LinkedTreeMap<String, String>params = new LinkedTreeMap<String, String>();
		String uri = new Uri.Builder().appendPath("reg").appendPath("get_friends").build().toString();
		new HttpRequest(uri, params, new HttpRequest.Callbacks() {
            @Override
            public void success(String response) {
                gotFriends(context, response);
            }
            @Override
            public void error(String errorString) {
                failure();   
            }
        });
	}

	@SuppressWarnings("unchecked")
	private void gotFriends(Context context, String r) {
        Log.i(TAG, "gotFriends: " + r);
		Gson g = new Gson();
		friendList = g.fromJson(r, friendList.getClass());
		
		if (destroyAll)
			FriendFactory.getFactoryInstance().destroyAll(context);

        FriendFactory.getFactoryInstance().reconcileFriends(context, friendList);
        success();
	}
}
