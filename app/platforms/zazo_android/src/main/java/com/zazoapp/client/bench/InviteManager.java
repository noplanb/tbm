package com.zazoapp.client.bench;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.network.DeleteFriendRequest;
import com.zazoapp.client.network.HttpRequest;
import com.zazoapp.client.ui.MainActivity;
import com.zazoapp.client.ui.MainFragment;
import com.zazoapp.client.ui.dialogs.InviteIntent;
import com.zazoapp.client.ui.helpers.ContactsManager;
import com.zazoapp.client.utilities.Logger;
import com.zazoapp.client.utilities.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Set;

public class InviteManager implements InviteHelper {

    public static final String SMS = "SMS";

    private static class IntentActions{
        public static final String SMS_RESULT = "smsResult";
    }

    public interface InviteDialogListener {
        void onShowInfoDialog(String title, String msg);
        void onShowActionInfoDialog(String title, String msg, String actionTitle, boolean isNeedCancel, boolean editable, int actionId);
        void onShowDoubleActionDialog(Bundle data, int id, boolean editable);
        void onShowSendLinkDialog(int id, String phone, String msg);
        void onShowProgressDialog(String title, String msg);
        void onShowSelectPhoneNumberDialog(Contact contact);
        void onDismissProgressDialog();
        void onFinishInvitation();
    }

    private static final String TAG = InviteManager.class.getSimpleName();

	private Context context;
    private BenchObject benchObject;
    private Friend friend;
    private Friend lastInvitedFriend;
    private InviteDialogListener listener;
    private boolean isInvitationInProcess;
    private boolean isNudging;
    private boolean friendWasAdded;

    public InviteManager(Context context) {
        this.context = context;
    }

    public void setListener(InviteDialogListener listener) {
        this.listener = listener;
    }

    @Override
    public void invite(BenchObject bo) {
        Logger.d(TAG, "invite: " + bo);
        benchObject = bo;
        inviteInner(bo);
    }

    @Override
    public void invite(Contact contact, int phoneIndex) {
        Logger.d(TAG, "invite: " + contact + " (" + phoneIndex + ")");
        LinkedTreeMap<String, String> mobileNumber = contact.phoneObjects.get(phoneIndex);
        benchObject = BenchObject.benchObjectWithContact(contact, mobileNumber);
        inviteInner(benchObject);
    }

    private void inviteInner(BenchObject bo) {
        isInvitationInProcess = true;
        isNudging = false;
        lastInvitedFriend = null;
        friendWasAdded = false;
        final Friend friend = friendMatchingContact(bo.mobileNumber);
        if (friend != null) {
            this.friend = friend;
            if (friend.isDeleted()) {
                DeleteFriendRequest.makeRequest(friend, false, new HttpRequest.Callbacks() {
                    @Override
                    public void success(String response) {
                        friend.setDeleted(false);
                        GridManager.onFriendDeleteStatusChanged(friend);
                        listener.onDismissProgressDialog();
                        finishInvitation();
                    }

                    @Override
                    public void error(String errorString) {
                        listener.onDismissProgressDialog();
                        serverError();
                    }
                });
                listener.onShowProgressDialog(null, context.getString(R.string.dialog_checking_title));
            } else {
                // TODO swap friends
                moveFriendToGrid();
            }
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
        isNudging = true;
        lastInvitedFriend = null;
        friend = f;
        preNudgeDialog();
    }

    private Friend friendMatchingContact(String mobileNumber) {
        for (Friend f : FriendFactory.getFactoryInstance().all()) {
            if (ContactsManager.isPhoneNumberMatch(f.get(Friend.Attributes.MOBILE_NUMBER), mobileNumber)) {
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
            listener.onShowProgressDialog(null, context.getString(R.string.dialog_checking_title));
		}

    }
    @SuppressWarnings("unchecked")
	private void gotHasApp(String response) {
        LinkedTreeMap<String, String> params = StringUtils.linkedTreeMapWithJson(response);
        if (checkIsFailureAndShowDialog(StringUtils.fromJson(response, HttpRequest.ServerResponse.class)))
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
                    if (friend != null) {
                        UpdateInviteeInfoRequest.updateInviteeInfo(context, friend, benchObject.contactId);
                    }
                }
                @Override
                public void error(String errorString) {
                    Dispatch.dispatch("Error: " + errorString);
                    serverError();
                    listener.onDismissProgressDialog();
                }
            });
            Logger.d(TAG, "Invitation: " + uri + " " + params);
            listener.onShowProgressDialog(null, context.getString(R.string.dialog_syncing_title));
        }
    }

    private static class UpdateInviteeInfoRequest extends HttpRequest {

        public UpdateInviteeInfoRequest(JSONObject json) {
            super("invitation/update_friend", json, HttpRequest.POST, null);
        }

        static void updateInviteeInfo(Context context, Friend invitee, @Nullable String contactId) {
            JSONObject json = new JSONObject();
            try {
                json.put(FriendFactory.ServerParamKeys.MKEY, invitee.getMkey());
                Set<String> emails = ContactsManager.getEmailsForPhone(context, invitee.get(Friend.Attributes.MOBILE_NUMBER), contactId);
                JSONArray jsonEmails = new JSONArray(emails);
                json.put(FriendFactory.ServerParamKeys.EMAILS, jsonEmails);
                new UpdateInviteeInfoRequest(json);
            } catch (JSONException e) {
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void gotFriend(String response) {
        HttpRequest.ServerResponse serverResponse = StringUtils.fromJson(response, HttpRequest.ServerResponse.class);

        if (checkIsFailureAndShowDialog(serverResponse))
            return;
        FriendFactory.ServerFriend serverFriend = StringUtils.fromJson(response, FriendFactory.ServerFriend.class);
        friend = FriendFactory.getFactoryInstance().createWithServerParams(context, serverFriend, false);
        if (friend != null) {
            friendWasAdded = true;
            if (friend.hasApp()) {
                finishInvitation();
            } else {
                lastInvitedFriend = friend;
                showSmsDialog();
            }
        } else { // if friend is already exist TODO legacy code
            friend = FriendFactory.getFactoryInstance().getExistingFriend(serverFriend);
            moveFriendToGrid();
        }
    }

    @Override
    public void finishInvitation() {
        if (friend == null) {
            Log.e(TAG, "Friend is null on this step. This should never happen");
            Dispatch.dispatch(new NullPointerException(), "Friend is null");
            return;
        }
        if (isNudging) {
            isNudging = false;
        } else {
            lastInvitedFriend = friend;
        }
        moveFriendToGrid();
    }

    private void showAlreadyConnectedDialog() {
        cancelInvitation();
        String msg = context.getString(R.string.dialog_already_connected_message, benchObject.firstName, Config.appName, benchObject.firstName);
        String title = context.getString(R.string.dialog_already_connected_title);
        String action = context.getString(R.string.dialog_action_ok);
        listener.onShowActionInfoDialog(title, msg, action, false, false, MainFragment.CONNECTED_DIALOG);
    }

    private void serverError() {
        cancelInvitation();
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
        String action = context.getString(R.string.dialog_action_invite);
        listener.onShowActionInfoDialog(title, msg, action, true, false, MainFragment.NUDGE_DIALOG);
    }

    private void preSmsDialog() {
        String name = benchObject.firstName;
        String msg = context.getString(R.string.dialog_invite_friend_message, name, Config.appName);
        String title = context.getString(R.string.dialog_invite_friend_title, name);
        String action = context.getString(R.string.dialog_action_invite);
        listener.onShowActionInfoDialog(title, msg, action, true, false, MainFragment.SMS_DIALOG);
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
        listener.onShowActionInfoDialog(title, message, action, false, false, MainFragment.NO_SIM_DIALOG);
    }

    @Override
    public void showSmsDialog() {
        listener.onShowSendLinkDialog(MainFragment.SENDLINK_DIALOG, getMobileNumber(), getDefaultInviteMessage());
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
        params.put("mkey", friend.getMkey());
        params.put("messaging_platform", name);
        params.put("message_status", success ? HttpRequest.StatusValues.STATUS_SUCCESS :
                HttpRequest.StatusValues.STATUS_FAILURE);
        new HttpRequest("invitation/direct_invite_message", params, HttpRequest.POST);
    }

    @Override
    public boolean isInvitationInProcess() {
        return isInvitationInProcess;
    }

    @Override
    public void cancelInvitation() {
        isInvitationInProcess = false;
        isNudging = false;
        friendWasAdded = false;
    }

    private String getDefaultInviteMessage() {
        User user = UserFactory.current_user();
        if (user == null || friend == null) {
            return context.getString(R.string.dialog_invite_sms_no_user_message, Config.APP_DOMAIN, Config.appName);
        }
        String url;
        String id;
        if (!TextUtils.isEmpty(friend.getCid())) {
            url = Config.landingPageUrl;
            id = friend.getCid();
        } else {
            url = Config.legacyLandingPageUrl;
            id = user.getId();
        }
        return context.getString(R.string.dialog_invite_sms_message, friend.getFirstName(), Config.appName, url, id, user.getFirstName());
    }

    private boolean sendSms(String body, Activity activity) {
        String addr = getMobileNumber();

        Log.i(TAG, "sendSms: " + addr + ": " + body);
        if (DebugConfig.Bool.SEND_SMS.get()) {
            try {
                SmsManager sms = SmsManager.getDefault();
                ArrayList<String> parts = sms.divideMessage(body);
                sms.sendMultipartTextMessage(addr, null, parts, null, null);
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
        isInvitationInProcess = false;
        if (listener != null) {
            listener.onFinishInvitation();
        }
        GridManager.getInstance().moveFriendToGrid(friend);
        friendWasAdded = false;
    }

	// Not used as the intent coming back into home context is unnecessarily disruptive.
	private PendingIntent makeSmsResultPendingIntent(){
		Intent i = new Intent(context, MainActivity.class);
		i.setAction(IntentActions.SMS_RESULT);
		Uri uri = new Uri.Builder().appendPath(IntentHandlerService.IntentActions.SMS_RESULT).appendQueryParameter(IntentHandlerService.IntentParamKeys.FRIEND_ID, friend.getId()).build();
		i.setData(uri);
		return PendingIntent.getActivity(context, 0, i, 0);
	}

    private boolean checkIsFailureAndShowDialog(HttpRequest.ServerResponse response) {
        if (response == null) {
            serverError();
            return true;
        }
        if (HttpRequest.isFailure(response.status)) {
            cancelInvitation();
            listener.onShowInfoDialog(response.title, response.message);
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
        return hasSim() || !DebugConfig.Bool.SEND_SMS.get();
    }

    public boolean isFriendWasAdded() {
        return friendWasAdded;
    }
}
