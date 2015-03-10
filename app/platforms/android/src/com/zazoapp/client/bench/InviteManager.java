package com.zazoapp.client.bench;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.Config;
import com.zazoapp.client.GridManager;
import com.zazoapp.client.IntentHandler;
import com.zazoapp.client.R;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.model.Contact;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.network.HttpRequest;
import com.zazoapp.client.ui.MainActivity;
import com.zazoapp.client.utilities.Logger;

public class InviteManager{
	private static InviteManager inviteManager;

    private static class IntentActions{
        public static final String SMS_RESULT = "smsResult";
    }

    public static interface InviteDialogListener {
        void onShowInfoDialog(String title, String msg);
        void onShowActionInfoDialog(String title, String msg, String actionTitle, boolean isNeedCancel, boolean editable, int actionId);
        void onShowProgressDialog(String title, String msg);
        void onShowSelectPhoneNumberDialog(Contact contact);
        void onDismissProgressDialog();
    }

    private static final String TAG = InviteManager.class.getSimpleName();

	private Context context;
    private BenchObject benchObject;
    private Friend friend;
    private InviteDialogListener listener;

    private InviteManager() {
    }

    public static InviteManager getInstance() {
        if (inviteManager == null)
            inviteManager = new InviteManager();
        return inviteManager;
    }

    public void init(Context context, InviteDialogListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void invite(BenchObject bo) {
        Logger.d(TAG, "invite: " + bo);
        benchObject = bo;
        checkHasApp();
    }

    public void invite(Contact contact, int phoneIndex) {
        Logger.d(TAG, "invite: " + contact + " (" + phoneIndex + ")");
        benchObject = BenchObject.benchObjectWithContact(contact, contact.phoneObjects.get(phoneIndex));
        checkHasApp();
    }

    public void invite(Contact contact) {
        int phonesNumber = contact.phoneObjects.size();
        if (phonesNumber == 0) {
            listener.onShowInfoDialog(context.getString(R.string.dialog_no_valid_phones_title),
                    context.getString(R.string.dialog_no_valid_phones_message, contact.getDisplayName(), contact.getFirstName()));
        } else if (phonesNumber == 1) {
            invite(contact, 0);
        } else {
            listener.onShowSelectPhoneNumberDialog(contact);
        }
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

	private class CheckHasAppRequest extends HttpRequest {


        public CheckHasAppRequest(String uri) {
			super(uri, new Callbacks() {
                @Override
                public void success(String response) {
                    gotHasApp(response);
                    listener.onDismissProgressDialog();
                }
                @Override
                public void error(String errorString) {
                    Dispatch.dispatch("Error: " + errorString);
                    serverError();
                    listener.onDismissProgressDialog();
                }
            });
            listener.onShowProgressDialog(context.getString(R.string.dialog_checking_title), null);
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
		else if (hasSim() || !DebugConfig.getInstance(context).shouldSendSms()) {
            friend = null; // to clear previous nudged or invited friend
            preSmsDialog();
        } else {
            failureNoSimDialog();
        }

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
		new InviteFriendRequest(url);
	}

	private class InviteFriendRequest extends HttpRequest{

        public InviteFriendRequest(String uri) {
			super(uri, new Callbacks() {
                @Override
                public void success(String response) {
                    Log.i(TAG, "Success: " + response);
                    gotFriend(response);
                    listener.onDismissProgressDialog();
                }
                @Override
                public void error(String errorString) {
                    Dispatch.dispatch("Error: " + errorString);
                    serverError();
                    listener.onDismissProgressDialog();
                }
            });
            listener.onShowProgressDialog(context.getString(R.string.dialog_checking_title), null);
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
        if (friend != null) {
            connectedDialog();
        } else { // if friend is already exist
            friend = FriendFactory.getFactoryInstance().getExistingFriend(params);
            alreadyConnectedDialog();
        }

	}

    //-----------------
    // Connected Dialog
    //-----------------
    private void connectedDialog() {
        String msg = context.getString(R.string.dialog_connected_message, benchObject.firstName, Config.appName, benchObject.firstName);
        String title = context.getString(R.string.dialog_connected_title);
        String action = context.getString(R.string.dialog_action_ok);
        listener.onShowActionInfoDialog(title, msg, action, false, false, MainActivity.CONNECTED_DIALOG);
    }

    private void alreadyConnectedDialog() {
        String msg = context.getString(R.string.dialog_already_connected_message, benchObject.firstName, Config.appName, benchObject.firstName);
        String title = context.getString(R.string.dialog_already_connected_title);
        String action = context.getString(R.string.dialog_action_ok);
        listener.onShowActionInfoDialog(title, msg, action, false, false, MainActivity.CONNECTED_DIALOG);
    }

	private void serverError(){
		showErrorDialog("Can't reach " + Config.appName + ".\n\nCheck your connection and try again.");
	}

	private void showErrorDialog(String message){
        listener.onShowInfoDialog("No Connection", message);
	}

	//----------------------------
	// Send Sms with download link
	//----------------------------
	private void preNudgeDialog(){
        String msg = friend.get(Friend.Attributes.FIRST_NAME) + " still hasn't installed " + Config.appName + ". Send them the link again.";
        String title = "Nudge " + friend.get(Friend.Attributes.FIRST_NAME);
        listener.onShowActionInfoDialog(title, msg, "Send", false, false, MainActivity.NUDGE_DIALOG);
	}


	private void preSmsDialog(){
        String value = benchObject.firstName + " has not installed " + Config.appName + " yet.\n\nSend them a link!";
        listener.onShowActionInfoDialog("Invite", value, "Send", true, false, MainActivity.SMS_DIALOG);
	}

    private void failureNoSimDialog() {
        listener.onShowInfoDialog(context.getString(R.string.dialog_send_sms_failure_title),
                context.getString(R.string.dialog_send_sms_failure_message, benchObject.displayName, Config.appName));
    }

	public void showSms(){
        listener.onShowActionInfoDialog("Send Link", getDefaultInviteMessage(), "Send", true, true, MainActivity.SENDLINK_DIALOG);
	}

	public void sendInvite(String message){
		sendSms(message);
		if (friend == null)
			getFriendFromServer();
	}

    public String getDefaultInviteMessage() {
        String mkey = UserFactory.current_user().get(User.Attributes.MKEY);
        return context.getString(R.string.dialog_invite_message, Config.appName, Config.landingPageUrl, mkey);
    }

	private void sendSms(String body){
		String addr = null;
		if (friend != null)
			addr = friend.get(Friend.Attributes.MOBILE_NUMBER);

		if (benchObject != null)
			addr = benchObject.mobileNumber;

        //addr = "+16502453537"; Only for Sani, it is not a development option

		Log.i(TAG, "sendSms: " + addr + ": " + body);
        if (DebugConfig.getInstance(context).shouldSendSms()) {
            SmsManager.getDefault().sendTextMessage(addr, null, body, null, null);
        } else {
            listener.onShowInfoDialog("Fake SMS invitation", "It is a fake SMS invitation to number " + addr
                    + " with text: \"" + body + "\"");
        }
	}

	public void moveFriendToGrid(){
		GridManager.getInstance().moveFriendToGrid(friend);
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
            listener.onShowInfoDialog(params.get(HttpRequest.ParamKeys.ERROR_TITLE), params.get(HttpRequest.ParamKeys.ERROR_MSG));
            return true;
        } else {
            return false;
        }
    }

    private boolean hasSim() {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getSimState() != TelephonyManager.SIM_STATE_ABSENT;
    }
}
