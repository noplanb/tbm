package com.noplanbees.tbm.bench;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.noplanbees.tbm.Config;
import com.noplanbees.tbm.GridManager;
import com.noplanbees.tbm.IntentHandler;
import com.noplanbees.tbm.dispatch.Dispatch;
import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.FriendFactory;
import com.noplanbees.tbm.network.HttpRequest;
import com.noplanbees.tbm.ui.MainActivity;

public class InviteManager{
	private static InviteManager inviteManager;

    private static class IntentActions{
        public static final String SMS_RESULT = "smsResult";
    }

    public static interface Callbacks{
        void onShowInfoDialog(String title, String msg);
        void onShowActionInfoDialog(String title, String msg, String actionTitle, boolean isNeedCancel, int actionId);
    }

	private final String TAG = getClass().getSimpleName();

	private Context context;
    private BenchObject benchObject;
    private Friend friend;
    private Callbacks callbacks;

	private InviteManager(){
    }

	public static InviteManager getInstance(Context context){
		if(inviteManager == null)
			inviteManager = new InviteManager();
        inviteManager.setContext(context);
		return inviteManager;
	}

    private void setContext(Context context){
        this.context = context;
        try {
            callbacks = (Callbacks) context;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }

    }

	public void invite(BenchObject bo){
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
		.appendQueryParameter(FriendFactory.ServerParamKeys.MOBILE_NUMBER, benchObject.mobileNumber);
		String url = builder.build().toString();
		new CheckHasAppRequest(url);
	}
    private ProgressDialog pd;

	private class CheckHasAppRequest extends HttpRequest {


        public CheckHasAppRequest(String uri) {
			super(uri, new Callbacks() {
                @Override
                public void success(String response) {
                    gotHasApp(response);
                    if(pd!=null)
                        pd.dismiss();
                }
                @Override
                public void error(String errorString) {
                    Dispatch.dispatch("Error: " + errorString);
                    serverError();
                    if(pd!=null)
                        pd.dismiss();
                }            });
            pd = ProgressDialog.show(context, "Checking", null);
		}

    }
    @SuppressWarnings("unchecked")
	public void gotHasApp(String response) {
		LinkedTreeMap <String, String> params = new LinkedTreeMap <String, String>();
		Gson g = new Gson();
		params = g.fromJson(response, params.getClass());
		if (checkIsFailureAndShowDialog(params))
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
		.appendQueryParameter(FriendFactory.ServerParamKeys.MOBILE_NUMBER, benchObject.mobileNumber)
		.appendQueryParameter(FriendFactory.ServerParamKeys.FIRST_NAME, benchObject.firstName)
		.appendQueryParameter(FriendFactory.ServerParamKeys.LAST_NAME, benchObject.lastName);
		String url = builder.build().toString();
		Log.i(TAG, url);
		new GetFriendRequest(url);
	}

	private class GetFriendRequest extends HttpRequest{

        public GetFriendRequest(String uri) {
			super(uri, new Callbacks() {
                @Override
                public void success(String response) {
                    Log.i(TAG, "Success: " + response);
                    gotFriend(response);
                    if(pd!=null)
                        pd.dismiss();
                }
                @Override
                public void error(String errorString) {
                    Dispatch.dispatch("Error: " + errorString);
                    serverError();
                    if(pd!=null)
                        pd.dismiss();
                }
            });
            pd = ProgressDialog.show(context, "Checking", null);
		}
    }
	@SuppressWarnings("unchecked")
	private void gotFriend(String response) {
		LinkedTreeMap<String, String>params = new LinkedTreeMap<String, String>();
		Gson g = new Gson();
		params = g.fromJson(response, params.getClass());

		if (checkIsFailureAndShowDialog(params))
			return;

		friend = FriendFactory.getFactoryInstance().createWithServerParams(context, params);
		connectedDialog();
	}

	//-----------------
	// Connected Dialog
	//-----------------
	private void connectedDialog(){
		String msg = "You and "+ benchObject.firstName +" are connected.\n\nRecord a welcome "
	+ Config.appName + " to " + benchObject.firstName + " now.";

        callbacks.onShowActionInfoDialog("You are Connected", msg, "Okay", false, MainActivity.CONNECTED_DIALOG);
	}

	private void serverError(){
		showErrorDialog("Can't reach " + Config.appName + ".\n\nCheck your connection and try again.");
	}

	private void showErrorDialog(String message){
        callbacks.onShowInfoDialog("No Connection", message);
	}

	//----------------------------
	// Send Sms with download link
	//----------------------------
	private void preNudgeDialog(){
        String msg = friend.get(Friend.Attributes.FIRST_NAME) + " still hasn't installed " + Config.appName + ". Send them the link again.";
        String title = "Nudge " + friend.get(Friend.Attributes.FIRST_NAME);
        callbacks.onShowActionInfoDialog(title, msg, "Send", false, MainActivity.NUDGE_DIALOG);
	}


	private void preSmsDialog(){
        String value = benchObject.firstName + " has not installed " + Config.appName + " yet.\n\nSend them a link!";
        callbacks.onShowActionInfoDialog("Invite", value, "Send", false, MainActivity.SMS_DIALOG);
	}

	public void showSms(){
		String smsMessage = "I sent you a message on " + Config.appName + ". Get the app - it is really great. http://www.zazoapp.com.";
        callbacks.onShowActionInfoDialog("Send Link", smsMessage, "Send", false, MainActivity.SENDLINK_DIALOG);
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
		GridManager.getInstance().moveFriendToGrid(context, friend);
	}

	// Not used as the intent coming back into home context is unnecessarily disruptive.
	private PendingIntent makeSmsResultPendingIntent(){
		Intent i = new Intent(context, MainActivity.class);
		i.setAction(IntentActions.SMS_RESULT);
		Uri uri = new Uri.Builder().appendPath(IntentHandler.IntentActions.SMS_RESULT).appendQueryParameter(IntentHandler.IntentParamKeys.FRIEND_ID, friend.getId()).build();
		i.setData(uri);
		return PendingIntent.getActivity(context, 0, i, 0);
	}

    public boolean checkIsFailureAndShowDialog(LinkedTreeMap<String, String>params){
        String status = params.get(HttpRequest.ParamKeys.RESPONSE_STATUS);
        if (HttpRequest.isFailure(status)){
            callbacks.onShowInfoDialog(params.get(HttpRequest.ParamKeys.ERROR_TITLE), params.get(HttpRequest.ParamKeys.ERROR_MSG));
            return true;
        } else {
            return false;
        }
    }

}
