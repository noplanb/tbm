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
import com.zazoapp.client.ContactsManager;
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

    public interface InviteDialogListener {
        void onShowInfoDialog(String title, String msg);
        void onShowActionInfoDialog(String title, String msg, String actionTitle, boolean isNeedCancel, boolean editable, int actionId);
        void onShowDoubleActionDialog(String title, String msg, String posText, String negText, int id, boolean editable);
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
        LinkedTreeMap<String, String> mobileNumber = contact.phoneObjects.get(phoneIndex);
        benchObject = BenchObject.benchObjectWithContact(contact, mobileNumber);
        Friend friend = friendMatchingContact(contact, mobileNumber);
        if (friend != null) {
            this.friend = friend;
            showAlreadyConnectedDialog();
            return;
        }
        checkHasApp();
    }

    public void inviteNewFriend() {
        if (benchObject != null) {
            getFriendFromServer();
        }
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

    public void nudge(Friend f) {
        friend = f;
        preNudgeDialog();
    }

    private Friend friendMatchingContact(Contact contact, LinkedTreeMap<String, String> mobileNumber) {
        for (Friend f : FriendFactory.getFactoryInstance().all()) {
            if (ContactsManager.isPhoneNumberMatch(f.get(Friend.Attributes.MOBILE_NUMBER),
                    mobileNumber.get(Contact.PhoneNumberKeys.E164))) {
                return f;
            }
        }
        return null;
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
        else {
            friend = null; // to clear previous nudged or invited friend
            preSmsDialog();
        }

	}

    //-----------------------
    // get friend from server
    //-----------------------
    private void getFriendFromServer() {
        Uri.Builder builder = new Uri.Builder();
        String uri = builder.appendPath("invitation").appendPath("invite").build().toString();
        LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
        params.put(FriendFactory.ServerParamKeys.MOBILE_NUMBER, benchObject.mobileNumber);
        params.put(FriendFactory.ServerParamKeys.FIRST_NAME, benchObject.firstName);
        params.put(FriendFactory.ServerParamKeys.LAST_NAME, benchObject.lastName);
        new InviteFriendRequest(uri, params);
    }

    private class InviteFriendRequest extends HttpRequest{

        public InviteFriendRequest(String uri, LinkedTreeMap<String, String> params) {
            super(uri, params, new Callbacks() {
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
            Logger.d(TAG, "Invitation: " + uri + " " + params);
            listener.onShowProgressDialog(context.getString(R.string.dialog_checking_title), null);
        }
    }

    @SuppressWarnings("unchecked")
    private void gotFriend(String response) {
        LinkedTreeMap<String, String> params = new LinkedTreeMap<>();
        Gson g = new Gson();
        params = g.fromJson(response, params.getClass());

        if (checkIsFailureAndShowDialog(params))
            return;

        friend = FriendFactory.getFactoryInstance().createWithServerParams(context, params);
        if (friend != null) {
            if (friend.hasApp()) {
                showConnectedDialog();
            } else {
                showSms();
            }
        } else { // if friend is already exist
            friend = FriendFactory.getFactoryInstance().getExistingFriend(params);
            showAlreadyConnectedDialog();
        }
    }

    //-----------------
    // Connected Dialog
    //-----------------
    public void showConnectedDialog() {
        String name = (friend != null) ? friend.getFirstName() : benchObject.firstName;
        String msg = context.getString(R.string.dialog_connected_message, name, Config.appName, name);
        String title = context.getString(R.string.dialog_connected_title);
        String action = context.getString(R.string.dialog_action_ok);
        listener.onShowActionInfoDialog(title, msg, action, false, false, MainActivity.CONNECTED_DIALOG);
    }

    private void showAlreadyConnectedDialog() {
        String msg = context.getString(R.string.dialog_already_connected_message, benchObject.firstName, Config.appName, benchObject.firstName);
        String title = context.getString(R.string.dialog_already_connected_title);
        String action = context.getString(R.string.dialog_action_ok);
        listener.onShowActionInfoDialog(title, msg, action, false, false, MainActivity.CONNECTED_DIALOG);
    }

    private void serverError() {
        String title = context.getString(R.string.dialog_server_error_title);
        String message = context.getString(R.string.dialog_server_error_message, Config.appName);
        listener.onShowInfoDialog(title, message);
    }

    //----------------------------
    // Send Sms with download link
    //----------------------------
    private void preNudgeDialog() {
        String name = friend.get(Friend.Attributes.FIRST_NAME);
        String msg = context.getString(R.string.dialog_nudge_friend_message, name, Config.appName);
        String title = context.getString(R.string.dialog_nudge_friend_title, name);
        String action = context.getString(R.string.dialog_action_ok);
        listener.onShowActionInfoDialog(title, msg, action, true, false, MainActivity.NUDGE_DIALOG);
    }

    private void preSmsDialog() {
        String name = benchObject.firstName;
        String msg = context.getString(R.string.dialog_invite_friend_message, name, Config.appName);
        String title = context.getString(R.string.dialog_invite_friend_title, name);
        String action = context.getString(R.string.dialog_action_ok);
        listener.onShowActionInfoDialog(title, msg, action, true, false, MainActivity.SMS_DIALOG);
    }

    public void failureNoSimDialog() {
        String friendName = (friend != null) ? friend.getFullName() : benchObject.displayName;
        String title = context.getString(R.string.dialog_send_sms_failure_title);
        String message = context.getString(R.string.dialog_send_sms_failure_message, friendName, Config.appName);
        String action = context.getString(R.string.dialog_action_ok);
        listener.onShowActionInfoDialog(title, message, action, false, false, MainActivity.NO_SIM_DIALOG);
    }

    public void showSms() {
        String title = context.getString(R.string.dialog_invite_sms_title);
        String posText = context.getString(R.string.dialog_invite_sms_action);
        String negText = context.getString(R.string.dialog_action_cancel);
        listener.onShowDoubleActionDialog(title, getDefaultInviteMessage(), posText, negText, MainActivity.SENDLINK_DIALOG, true);
    }

    public void sendInvite(String message) {
        if (canSendSms()) {
            sendSms(message);
            showConnectedDialog();
        } else {
            failureNoSimDialog();
        }
    }

    public String getDefaultInviteMessage() {
        String mkey = UserFactory.current_user().get(User.Attributes.MKEY);
        return context.getString(R.string.dialog_invite_sms_message, Config.appName, Config.landingPageUrl, mkey);
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

    public void moveFriendToGrid() {
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

    private boolean canSendSms() {
        return hasSim() || !DebugConfig.getInstance(context).shouldSendSms();
    }
}
