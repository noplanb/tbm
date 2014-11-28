package com.noplanbees.tbm;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SmsManager;
import android.text.InputType;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.noplanbees.tbm.ui.MainActivity;

public class InviteManager implements DialogInterface.OnClickListener{
	
	private static class IntentActions{
		public static final String SMS_RESULT = "smsResult";
	}

	private final String TAG = getClass().getSimpleName();

	private Context context;
	private BenchObject benchObject;
	private Friend friend;
	private ProgressDialog progress;
	private EditText smsBody;

	public InviteManager(Context c, BenchObject bo){
		context = c;
		benchObject = bo;
		Log.i(TAG, "InviteManager: " + benchObject.displayName +" "+ benchObject.firstName +" "+ benchObject.lastName+" "+ benchObject.mobileNumber);
		friend = (Friend) FriendFactory.getFactoryInstance().find(bo.friendId);
		setupProgressDialog();
		invite();
	}

	public void invite(){
		Log.i(TAG, "invite: friend=" + friend);
		if (friend == null)
			setFriendFromServer();
		else if (!friend.hasApp())
			showPreSmsDialog();
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
			showPreSmsDialog();
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
		showErrorDialog("Can't reach " + Config.appName + ".\n\nCheck your connection and try again.");
	}
	
	private void showErrorDialog(String message){
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("No Connection")
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
	
	private void showPreSmsDialog(){
		new AlertDialog.Builder(context)
		.setTitle("Invite")
		.setMessage(friend.get(Friend.Attributes.FIRST_NAME) + " has not installed " + Config.appName + " yet.\n\nSend them a link!")
		.setPositiveButton("Send", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				showSms();
			}
		}).setNegativeButton("Cancel", this)
		.show();
	}
	
	private void showSms(){
		smsBody = new EditText(context);
		LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		smsBody.setLayoutParams(lp);
		smsBody.setText("I sent you a message on " + Config.appName + ". Get the app - it is really great. http://www.zazoapp.com.");
		smsBody.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_CLASS_TEXT);
		
		new AlertDialog.Builder(context)
		.setTitle("Send Link")
		.setView(smsBody)
		.setPositiveButton("Send", this)
		.setNegativeButton("Cancel", this)
		.show();
	}
	
	private void showPostSms(){
		String msg = "You and "+ benchObject.firstName +" are connected.\n\nRecord a welcome " + Config.appName + " to " + benchObject.firstName + " now.";
		new AlertDialog.Builder(context)
		.setTitle("You are Connected")
		.setMessage(msg)
		.setPositiveButton("Ok", new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				GridManager.moveFriendToGrid(context, friend);
			}
		})
		.show();
	}


	@Override
	public void onClick(DialogInterface dialog, int which) {
		Log.i(TAG, "onClick:" + which);
		if (which == -1)
			sendSms(smsBody.getText().toString());
		showPostSms();
	}
	
	private void sendSms(String body){
		String addr = benchObject.mobileNumber;
		addr = "+16502453537";
		Log.i(TAG, "sendSms: " + addr + ": " + body);
		SmsManager.getDefault().sendTextMessage(addr, null, body, null, null);;
	}
	
	// Not used as the intent coming back into home activity is unnecessarily disruptive.
	private PendingIntent makeSmsResultPendingIntent(){
		Intent i = new Intent(context, MainActivity.class);
		i.setAction(IntentActions.SMS_RESULT);
		Uri uri = new Uri.Builder().appendPath(IntentHandler.IntentActions.SMS_RESULT).appendQueryParameter(IntentHandler.IntentParamKeys.FRIEND_ID, friend.getId()).build();
		i.setData(uri);
		return PendingIntent.getActivity(context, 0, i, 0);		
	}
}
