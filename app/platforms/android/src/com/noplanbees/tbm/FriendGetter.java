package com.noplanbees.tbm;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;

public class FriendGetter {
	
	private final String TAG = getClass().getSimpleName();
	
	private ProgressDialog progress;
	private Context context;
	private boolean destroyAll;
	private FriendGetterCallback delegate;
	ArrayList<LinkedTreeMap<String, String>> friendList = new ArrayList<LinkedTreeMap<String, String>>();
	
	public interface FriendGetterCallback{
		public void gotFriends();
	}
	
	public FriendGetter(Context c, boolean destroyAll, FriendGetterCallback delegate){
		context = c;
		this.destroyAll = destroyAll;
		this.delegate = delegate;
		progress = new ProgressDialog(context);
		progress.setTitle("Checking");	
		getFriends();
	}
	
	private void getFriends(){
		LinkedTreeMap<String, String>params = new LinkedTreeMap<String, String>();
//		params.put(UserFactory.ServerParamKeys.AUTH, UserFactory.current_user().get(User.Attributes.AUTH));
//		params.put(UserFactory.ServerParamKeys.MKEY, UserFactory.current_user().get(User.Attributes.MKEY));
		String uri = new Uri.Builder().appendPath("reg").appendPath("get_friends").build().toString();
		new GetFriends(uri, params);
	}

	class GetFriends extends Server{
		public GetFriends(String uri, LinkedTreeMap<String, String>params){
			super(uri, params);
			//progress.show();
		}
		@Override
		public void success(String response) {	
			//progress.dismiss();
			gotFriends(context, response);
		}
		@Override
		public void error(String errorString) {
			//progress.dismiss();
			serverError();
		}
	}

	@SuppressWarnings("unchecked")
	public void gotFriends(Context context, String r) {
        Log.i(TAG, "gotRegResponse: " + r);
		Gson g = new Gson();
		friendList = g.fromJson(r, friendList.getClass());
		Log.i(TAG, "gotRegResponse: " + friendList.toString());
		
		if (destroyAll)
			FriendFactory.getFactoryInstance().destroyAll(context);
		
		for (LinkedTreeMap<String, String> fm : friendList){
			FriendFactory.addFriendFromServerParams(context, fm);
		}
		delegate.gotFriends();
	}
	
	private void serverError(){
		showErrorDialog("No Connection", "Can't reach " + Config.appName + ".\n\nCheck your connection and try again.");
	}

	private void showErrorDialog(String title, String message){
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title)
		.setMessage(message)
		.setPositiveButton("Ok", null)
		.create().show();
	}

}
