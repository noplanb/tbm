package com.noplanbees.tbm;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

public class InviteManager {

	private final String TAG = getClass().getSimpleName();

	private Context context;
	private BenchObject benchObject;
	private Friend friend;
	private ProgressDialog progress;

	public InviteManager(Context c, BenchObject b){
		context = c;
		benchObject = b;
		setupProgressDialog();
		invite();
	}

	public void invite(){
		if (benchObject.friendId != null)
			Log.e(TAG, "invite: ERROR: cant invite bench object that already has a friendId");
		setFriendFromServer();
	}

	private void setFriendFromServer() {
		Uri.Builder builder = new Uri.Builder();
		builder.appendPath("invitation")
		.appendPath("invite")
		.appendQueryParameter(FriendFactory.ServerParamKeys.MKEY, UserFactory.current_user().get(User.Attributes.MKEY))
		.appendQueryParameter(FriendFactory.ServerParamKeys.MOBILE_NUMBER, benchObject.mobileNumber)
		.appendQueryParameter(FriendFactory.ServerParamKeys.FIRST_NAME, benchObject.firstName)
		.appendQueryParameter(FriendFactory.ServerParamKeys.LAST_NAME, benchObject.lastName);
		String url = builder.build().toString();
		Log.i(TAG, url);
		new getFriend(url);
	}

	private class getFriend extends Server{
		public getFriend(String uri) {
			super(uri);
			progress.show();
		}
		@Override
		public void success(String response) {
			Log.i(TAG, "Success: " + response);
			progress.hide();
			gotFriend(response);
		}
		@Override
		public void error(String errorString) {
			Log.e(TAG, "Error: " + errorString);
			serverError();
		}	
	}

	private void gotFriend(String response) {
		addFriend(response);
		sendSmsIfNecessary();
	}
	
	@SuppressWarnings("unchecked")
	private void addFriend(String response) {
		LinkedTreeMap<String, String>params = new LinkedTreeMap<String, String>();
		Gson g = new Gson();
		params = g.fromJson(response, params.getClass());
		friend = FriendFactory.addFriendFromServerParams(context, params);
	}
	
	private void sendSmsIfNecessary() {
		if (friend != null && !friend.hasApp())
			Log.i(TAG, "User sending sms");
		else
			Log.i(TAG, "Friend has app or doesnt exit. Not sending sms.");
	}
	


	//---------------------------------
	// Progress dialog and error alerts
	//---------------------------------
	private void setupProgressDialog(){
		progress = new ProgressDialog(context);
		progress.setTitle("Checking");
	}
	
	private void serverError(){
		showErrorDialog("Can't reach ThreeByMe.\n\nCheck your connectivity and try again.");
	}
	
	private void showErrorDialog(String message){
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Error")
		.setMessage(message)
		.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
			}
		})
		.create().show();
	}



}
