package com.noplanbees.tbm;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.noplanbees.tbm.Friend.Attributes;

public class InviteManager {

	private final String TAG = getClass().getSimpleName();

	private Context context;
	private BenchObject benchObject;
	private Friend friend;
	private ProgressDialog progress;

	public InviteManager(Context c, BenchObject bo){
		Log.i(TAG, "InviteManager: " + bo.displayName +" "+ bo.mobileNumber);
		context = c;
		benchObject = bo;
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
		.appendQueryParameter(UserFactory.ServerParamKeys.AUTH, UserFactory.current_user().get(User.Attributes.AUTH))
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
			progress.hide();
			serverError();
		}	
	}

	private void gotFriend(String response) {
		addFriend(response);
		GridManager.moveFriendToGrid(friend);
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
		if (friend != null && !friend.hasApp()){
			Log.i(TAG, "Friend and no app sending sms");
			showSmsDialog();
		} else {
			Log.i(TAG, "Friend has app or doesnt exit. Not sending sms.");
		}
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


	//----------------------------
	// Send Sms with download link
	//----------------------------

	private void showSmsDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Invite")
		.setMessage(friend.get(Friend.Attributes.FIRST_NAME) + " has not installed " + Config.appName + " yet.\n\nSend them a link!")
		.setPositiveButton("Send", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				showSms();
			}
		}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
			}
	    });
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}
	
	private void showSms(){
		String mn = "+16502453537";
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setData(Uri.parse("smsto:" + mn)).
		putExtra("address", mn).
		putExtra("sms_body", "I sent you a message on " + Config.appName + ". Get it. Its great. http://asdf");

		try {
		    context.startActivity(Intent.createChooser(i, "Send sms..."));
		} catch (android.content.ActivityNotFoundException ex) {
		    Toast.makeText(context, "There are no sms clients installed.", Toast.LENGTH_SHORT).show();
		}

	}
}
