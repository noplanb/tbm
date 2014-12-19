package com.noplanbees.tbm;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.noplanbees.tbm.ui.MainActivity;
import com.noplanbees.tbm.ui.dialogs.ActionInfoDialogFragment;
import com.noplanbees.tbm.ui.dialogs.InfoDialogFragment;

public class InviteManager{
	private static InviteManager inviteManager;
	
	private static class IntentActions{
		public static final String SMS_RESULT = "smsResult";
	}

	private final String TAG = getClass().getSimpleName();

	private Activity activity;
	private BenchObject benchObject;
	private Friend friend;
	private ProgressDialog progress;

	private InviteManager(Activity a){
		activity = a;
	}
	
	public static InviteManager getInstance(Activity a){
		if(inviteManager == null)
			inviteManager = new InviteManager(a);
		return inviteManager;
	}

	public void invite(BenchObject bo){
		setupProgressDialog();
		benchObject = bo;
		Log.i(TAG, "invite: " + benchObject.displayName +" "+ benchObject.firstName +" "+ benchObject.lastName+" "+ benchObject.mobileNumber);
		checkHasApp();
	}
	
	public void nudge(Friend f){
		friend = f;
		preNudgeDialog();
	}
	
	//--------------
	// Check has app
	//--------------
	private void checkHasApp() {
		Uri.Builder builder = new Uri.Builder();
		builder.appendPath("invitation")
		.appendPath("has_app")
		.appendQueryParameter(UserFactory.ServerParamKeys.MKEY, UserFactory.current_user().get(User.Attributes.MKEY))
		.appendQueryParameter(UserFactory.ServerParamKeys.AUTH, UserFactory.current_user().get(User.Attributes.AUTH))
		.appendQueryParameter(FriendFactory.ServerParamKeys.MOBILE_NUMBER, benchObject.mobileNumber);
		String url = builder.build().toString();
		new checkHasApp(url);
	}
	
	private class checkHasApp extends Server{
		public checkHasApp(String uri) {
			super(uri);
			progress.show();
		}
		@Override
		public void success(String response) {	
			progress.hide();
			gotHasApp(response);
		}
		@Override
		public void error(String errorString) {		
			Log.e(TAG, "Error: " + errorString);
			progress.hide();
			serverError();
		}
	}
	@SuppressWarnings("unchecked")
	public void gotHasApp(String response) {	
		LinkedTreeMap <String, String> params = new LinkedTreeMap <String, String>();
		Gson g = new Gson();
		params = g.fromJson(response, params.getClass());
		if (Server.checkIsFailureAndShowDialog(activity, params))
			return;
		
		String hasAppStr = params.get(FriendFactory.ServerParamKeys.HAS_APP);
		boolean hasApp = hasAppStr != null && hasAppStr.equalsIgnoreCase("true");
		if (hasApp)
			getFriendFromServer();
		else
			preSmsDialog();
			
	}
	
	//-----------------------
	// get friend from server
	//-----------------------
	private void getFriendFromServer() {
		Uri.Builder builder = new Uri.Builder();
		builder.appendPath("invitation")
		.appendPath("invite")
		.appendQueryParameter(UserFactory.ServerParamKeys.MKEY, UserFactory.current_user().get(User.Attributes.MKEY))
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

	@SuppressWarnings("unchecked")
	private void gotFriend(String response) {
		LinkedTreeMap<String, String>params = new LinkedTreeMap<String, String>();
		Gson g = new Gson();
		params = g.fromJson(response, params.getClass());
		
		if (Server.checkIsFailureAndShowDialog(activity, params))
			return;
		
		friend = FriendFactory.addFriendFromServerParams(activity, params);
		connectedDialog();
	}
	
	//-----------------
	// Connected Dialog
	//-----------------
	private void connectedDialog(){
		String msg = "You and "+ benchObject.firstName +" are connected.\n\nRecord a welcome " 
	+ Config.appName + " to " + benchObject.firstName + " now.";
		
		ActionInfoDialogFragment actionDialogFragment = new ActionInfoDialogFragment();
		Bundle args = new Bundle();
		args.putString(ActionInfoDialogFragment.TITLE, "You are Connected");
		args.putString(ActionInfoDialogFragment.MSG, msg);
		args.putString(ActionInfoDialogFragment.ACTION, "Okay");
		args.putInt(ActionInfoDialogFragment.ID, MainActivity.CONNECTED_DIALOG);
		args.putBoolean(ActionInfoDialogFragment.NEED_CANCEL, false);
		actionDialogFragment.setArguments(args );
		actionDialogFragment.show(activity.getFragmentManager(), null);
		
	}
	
	
	//---------------------------------
	// Progress dialog and error alerts
	//---------------------------------
	private void setupProgressDialog(){
		progress = new ProgressDialog(activity);
		progress.setTitle("Checking");
	}
	
	private void serverError(){
		showErrorDialog("Can't reach " + Config.appName + ".\n\nCheck your connection and try again.");
	}
	
	private void showErrorDialog(String message){
		InfoDialogFragment infoDialogFragment = new InfoDialogFragment();
		Bundle args = new Bundle();
		args.putString(InfoDialogFragment.TITLE, "No Connection");
		args.putString(InfoDialogFragment.MSG, message);
		infoDialogFragment.setArguments(args );
		infoDialogFragment.show(activity.getFragmentManager(), null);
	}
	

	//----------------------------
	// Send Sms with download link
	//----------------------------
	private void preNudgeDialog(){
		ActionInfoDialogFragment actionDialogFragment = new ActionInfoDialogFragment();
		Bundle args = new Bundle();
		args.putString(ActionInfoDialogFragment.TITLE, "Nudge " + friend.get(Friend.Attributes.FIRST_NAME));
		args.putString(ActionInfoDialogFragment.MSG, friend.get(Friend.Attributes.FIRST_NAME) + " still hasn't installed " + Config.appName + ". Send them the link again.");
		args.putString(ActionInfoDialogFragment.ACTION, "Send");
		args.putInt(ActionInfoDialogFragment.ID, MainActivity.NUDGE_DIALOG);
		actionDialogFragment.setArguments(args );
		actionDialogFragment.show(activity.getFragmentManager(), null);
	}
	
	private void preSmsDialog(){
		ActionInfoDialogFragment actionDialogFragment = new ActionInfoDialogFragment();
		Bundle args = new Bundle();
		args.putString(ActionInfoDialogFragment.TITLE, "Invite");
		args.putString(ActionInfoDialogFragment.MSG, benchObject.firstName + " has not installed " + Config.appName + " yet.\n\nSend them a link!");
		args.putString(ActionInfoDialogFragment.ACTION, "Send");
		args.putInt(ActionInfoDialogFragment.ID, MainActivity.SMS_DIALOG);
		actionDialogFragment.setArguments(args );
		actionDialogFragment.show(activity.getFragmentManager(), null);
	}
	
	public void showSms(){
		String smsMessage = "I sent you a message on " + Config.appName + ". Get the app - it is really great. http://www.zazoapp.com.";
		ActionInfoDialogFragment actionDialogFragment = new ActionInfoDialogFragment();
		Bundle args = new Bundle();
		args.putString(ActionInfoDialogFragment.TITLE, "Send Link");
		args.putString(ActionInfoDialogFragment.MSG, smsMessage);
		args.putString(ActionInfoDialogFragment.ACTION, "Send");
		args.putInt(ActionInfoDialogFragment.ID, MainActivity.SENDLINK_DIALOG);
		actionDialogFragment.setArguments(args );
		actionDialogFragment.show(activity.getFragmentManager(), null);
	}
	
	public void sendLink(){
		String smsMessage = "I sent you a message on " + Config.appName + ". Get the app - it is really great. http://www.zazoapp.com.";
		sendSms(smsMessage);
		if (friend == null)
			getFriendFromServer();
	}
	
	private void sendSms(String body){
		String addr;
		if (friend != null)
			addr = friend.get(Friend.Attributes.MOBILE_NUMBER);
			
		if (benchObject != null)
			addr = benchObject.mobileNumber;
		
		addr = "+16502453537";
		Log.i(TAG, "sendSms: " + addr + ": " + body);
//		SmsManager.getDefault().sendTextMessage(addr, null, body, null, null);
	}
	
	public void moveFriendToGrid(){
		GridManager.moveFriendToGrid(activity, friend);
	}
	
	// Not used as the intent coming back into home activity is unnecessarily disruptive.
	private PendingIntent makeSmsResultPendingIntent(){
		Intent i = new Intent(activity, MainActivity.class);
		i.setAction(IntentActions.SMS_RESULT);
		Uri uri = new Uri.Builder().appendPath(IntentHandler.IntentActions.SMS_RESULT).appendQueryParameter(IntentHandler.IntentParamKeys.FRIEND_ID, friend.getId()).build();
		i.setData(uri);
		return PendingIntent.getActivity(activity, 0, i, 0);		
	}

}
