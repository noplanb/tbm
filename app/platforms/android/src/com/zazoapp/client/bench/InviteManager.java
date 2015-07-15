package com.zazoapp.client.bench;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.Config;
import com.zazoapp.client.R;
import com.zazoapp.client.core.IntentHandlerService;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.model.Contact;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.GridManager;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.network.HttpRequest;
import com.zazoapp.client.ui.MainActivity;
import com.zazoapp.client.ui.dialogs.InviteIntent;
import com.zazoapp.client.ui.helpers.ContactsManager;
import com.zazoapp.client.utilities.Logger;
import com.zazoapp.client.utilities.StringUtils;

public class InviteManager implements InviteHelper {

    public static final String SMS = "SMS";

    private static class IntentActions{
        public static final String SMS_RESULT = "smsResult";
    }

    public interface InviteDialogListener {
        void onShowInfoDialog(String title, String msg);
        void onShowActionInfoDialog(String title, String msg, String actionTitle, boolean isNeedCancel, boolean editable, int actionId);
        void onShowDoubleActionDialog(String title, String msg, String posText, String negText, int id, boolean editable);
        void onShowSendLinkDialog(int id, String phone, String msg);
        void onShowProgressDialog(String title, String msg);
        void onShowSelectPhoneNumberDialog(Contact contact);
        void onDismissProgressDialog();
    }

    private static final String TAG = InviteManager.class.getSimpleName();

	private Context context;
    private BenchObject benchObject;
    private Friend friend;
    private Friend lastInvitedFriend;
    private InviteDialogListener listener;

    public InviteManager(Context context, InviteDialogListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    public void invite(BenchObject bo) {
        Logger.d(TAG, "invite: " + bo);
        benchObject = bo;
        checkHasApp();
    }

    @Override
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

    @Override
    public void inviteNewFriend() {
        if (benchObject != null) {
            getFriendFromServer();
        }
    }

    @Override
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

    @Override
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
	private void gotHasApp(String response) {
        LinkedTreeMap<String, String> params = StringUtils.linkedTreeMapWithJson(response);
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
        LinkedTreeMap<String, String> params = StringUtils.linkedTreeMapWithJson(response);

        if (checkIsFailureAndShowDialog(params))
            return;
        friend = FriendFactory.getFactoryInstance().createWithServerParams(context, params);
        if (friend != null) {
            if (friend.hasApp()) {
                finishInvitation();
            } else {
                showSmsDialog();
            }
        } else { // if friend is already exist TODO legacy code
            friend = FriendFactory.getFactoryInstance().getExistingFriend(params);
            showAlreadyConnectedDialog();
        }
    }

    @Override
    public void finishInvitation() {
        if (friend == null) {
            Log.e(TAG, "Friend is null on this step. This should never happen");
            Dispatch.dispatch(new NullPointerException(), "Friend is null");
            return;
        }
        lastInvitedFriend = friend;
        moveFriendToGrid();
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

    @Override
    public void failureNoSimDialog() {
        if (friend == null) {
            Log.e(TAG, "Friend is null on this step. This should never happen");
            Dispatch.dispatch(new NullPointerException(), "Friend is null");
            return;
        }
        String friendName = friend.getFullName();
        String title = context.getString(R.string.dialog_send_sms_failure_title);
        String message = context.getString(R.string.dialog_send_sms_failure_message, friendName, Config.appName);
        String action = context.getString(R.string.dialog_action_ok);
        listener.onShowActionInfoDialog(title, message, action, false, false, MainActivity.NO_SIM_DIALOG);
    }

    @Override
    public void showSmsDialog() {
        listener.onShowSendLinkDialog(MainActivity.SENDLINK_DIALOG, getMobileNumber(), getDefaultInviteMessage());
    }

    @Override
    public void sendInvite(String message, Activity activity) {
        if (canSendSms()) {
            boolean sentInstantly = sendSms(message, activity);
            notifyInviteVector(SMS, true);
            if (sentInstantly) {
                finishInvitation();
            }
        } else {
            notifyInviteVector(SMS, false);
            failureNoSimDialog();
        }
    }

    @Override
    public Friend getLastInvitedFriend() {
        return lastInvitedFriend;
    }

    @Override
    public void dropLastInvitedFriend() {
        lastInvitedFriend = null;
    }

    @Override
    public void notifyInviteVector(String name, boolean success) {
        if (friend == null || TextUtils.isEmpty(name)) {
            return;
        }
        LinkedTreeMap<String, String> params = new LinkedTreeMap<>();
        params.put("mkey", friend.get(Friend.Attributes.MKEY));
        params.put("messaging_platform", name);
        params.put("message_status", success ? HttpRequest.StatusValues.STATUS_SUCCESS :
                HttpRequest.StatusValues.STATUS_FAILURE);
        new HttpRequest("invitation/direct_invite_message", params, "POST");
    }

    private String getDefaultInviteMessage() {
        String id = UserFactory.current_user().getId();
        return context.getString(R.string.dialog_invite_sms_message, Config.appName, Config.landingPageUrl, id);
    }

    private boolean sendSms(String body, Activity activity) {
        String addr = getMobileNumber();

        Log.i(TAG, "sendSms: " + addr + ": " + body);
        if (DebugConfig.getInstance(context).shouldSendSms()) {
            try {
                SmsManager.getDefault().sendTextMessage(addr, null, body, null, null);
            } catch (RuntimeException e) {
                Dispatch.dispatch(e.getMessage());
                Bundle data = new Bundle();
                data.putString(InviteIntent.PHONE_NUMBER_KEY, addr);
                data.putString(InviteIntent.MESSAGE_KEY, body);
                activity.startActivityForResult(InviteIntent.SMS.getIntent(data), InviteIntent.INVITATION_REQUEST_ID);
                return false;
            }

        } else {
            listener.onShowInfoDialog("Fake SMS invitation", "It is a fake SMS invitation to number " + addr
                    + " with text: \"" + body + "\"");
        }
        return true;
    }

    private String getMobileNumber() {
        String addr = null;
        if (friend != null)
            addr = friend.get(Friend.Attributes.MOBILE_NUMBER);

        if (benchObject != null)
            addr = benchObject.mobileNumber;
        return addr;
    }

    @Override
    public void moveFriendToGrid() {
        GridManager.getInstance().moveFriendToGrid(friend);
    }

	// Not used as the intent coming back into home context is unnecessarily disruptive.
	private PendingIntent makeSmsResultPendingIntent(){
		Intent i = new Intent(context, MainActivity.class);
		i.setAction(IntentActions.SMS_RESULT);
		Uri uri = new Uri.Builder().appendPath(IntentHandlerService.IntentActions.SMS_RESULT).appendQueryParameter(IntentHandlerService.IntentParamKeys.FRIEND_ID, friend.getId()).build();
		i.setData(uri);
		return PendingIntent.getActivity(context, 0, i, 0);
	}

    private boolean checkIsFailureAndShowDialog(LinkedTreeMap<String, String> params) {
        if (params == null) {
            serverError();
            return true;
        }
        String status = params.get(HttpRequest.ParamKeys.RESPONSE_STATUS);
        if (HttpRequest.isFailure(status)) {
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
