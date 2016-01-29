package com.zazoapp.client.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.appsflyer.AFInAppEventType;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.zazoapp.client.Config;
import com.zazoapp.client.R;
import com.zazoapp.client.core.FriendGetter;
import com.zazoapp.client.core.RemoteStorageHandler;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.debug.DebugSettingsActivity;
import com.zazoapp.client.dispatch.ZazoAnalytics;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.model.ActiveModelsHandler;
import com.zazoapp.client.model.Contact;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.network.HttpRequest;
import com.zazoapp.client.network.aws.S3CredentialsGetter;
import com.zazoapp.client.ui.dialogs.EnterCodeDialogFragment;
import com.zazoapp.client.ui.dialogs.ProgressDialogFragment;
import com.zazoapp.client.ui.helpers.ContactsManager;
import com.zazoapp.client.ui.view.CountryCodeAdapter;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.DialogShower;
import com.zazoapp.client.utilities.StringUtils;

import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 11/10/2015.
 */
public class RegisterFragment extends ZazoFragment implements EnterCodeDialogFragment.Callbacks {

    public static final String TAG = RegisterFragment.class.getSimpleName();

    @InjectView(R.id.first_name_txt) EditText firstNameTxt;
    @InjectView(R.id.last_name_txt) EditText lastNameTxt;
    @InjectView(R.id.country_code_txt) AutoCompleteTextView countryCodeTxt;
    @InjectView(R.id.mobile_number_text) EditText mobileNumberTxt;
    @InjectView(R.id.app_logo) ImageView appLogo;
    @InjectView(R.id.transparent_view) View additionalView;

    private User user;

    private String firstName;
    private String lastName;
    private String countryCode;
    private String mobileNumber;
    private String e164;
    private String verificationCode;
    private String auth;
    private String mkey;

    private DialogFragment pd;
    private DialogFragment enterCodeDialog;

    private static final int DEBUG_SCREEN_CODE = 293;

    private static final String KEY_FIRST_NAME = "rf_first_name";
    private static final String KEY_LAST_NAME = "rf_last_name";
    private static final String KEY_COUNTRY_CODE = "rf_country_code";
    private static final String KEY_MOBILE_NUMBER = "rf_mobile_number";
    private static final String KEY_E164 = "rf_e164";
    private static final String KEY_AUTH = "rf_auth";
    private static final String KEY_MKEY = "rf_mkey";
    private Bundle registerData;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.register, null);
        ButterKnife.inject(this, v);
        setupListeners();

        setAdditionalViewHeight();
        setUpView(savedInstanceState);
        initUser();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        dismissProgressDialog();
    }

    private void setAdditionalViewHeight() {
        ViewGroup.LayoutParams lp = additionalView.getLayoutParams();
        lp.height = (int) (Convenience.getScreenDimensions(getContext()).y * 0.6);
        additionalView.setLayoutParams(lp);
    }

    //-----
    // Init
    //-----
    private void initUser() {
        UserFactory userFactory = UserFactory.getFactoryInstance();
        userFactory.destroyAll(getContext());
        user = userFactory.makeInstance(getContext());
    }

    //----------
    // SetupView
    //----------
    private void setUpView(@Nullable Bundle args) {
        //prefillTextFields(); issue 250
        if (args != null) {
            if (args.containsKey(KEY_FIRST_NAME)) {
                firstNameTxt.setText(args.getString(KEY_FIRST_NAME));
                firstName = args.getString(KEY_FIRST_NAME);
            }
            if (args.containsKey(KEY_LAST_NAME)) {
                lastNameTxt.setText(args.getString(KEY_LAST_NAME));
                lastName = args.getString(KEY_LAST_NAME);
            }
            if (args.containsKey(KEY_COUNTRY_CODE)) {
                countryCodeTxt.setText(args.getString(KEY_COUNTRY_CODE));
                countryCode = args.getString(KEY_COUNTRY_CODE);
            }
            if (args.containsKey(KEY_MOBILE_NUMBER)) {
                mobileNumberTxt.setText(args.getString(KEY_MOBILE_NUMBER));
                mobileNumber = args.getString(KEY_MOBILE_NUMBER);
            }
            e164 = args.getString(KEY_E164);
            auth = args.getString(KEY_AUTH);
            mkey = args.getString(KEY_MKEY);
        }
    }

    private void prefillTextFields() {
        Contact contact = new ContactsManager(getContext()).userProfile(getContext());
        Log.i(TAG, "profile: " + contact);

        if(contact == null)
            return;

        if (contact.getFirstName() != null)
            firstNameTxt.setText(contact.getFirstName());

        if (contact.getLastName() != null)
            lastNameTxt.setText(contact.getLastName());

        LinkedTreeMap<String, String> po = contact.firstPhoneMarkedMobileForContact();
        if (po != null){
            countryCodeTxt.setText(po.get(Contact.PhoneNumberKeys.COUNTRY_CODE));
            mobileNumberTxt.setText(po.get(Contact.PhoneNumberKeys.NATIONAL));
        } else {
            countryCodeTxt.setText("1");
        }
    }

    //----------------------------
    // Handle register form submit
    //----------------------------
    @OnClick(R.id.enter_btn)
    public void registerUser() {
        firstName = cleanName(firstNameTxt.getText().toString());
        firstNameTxt.setText(firstName);
        lastName = cleanName(lastNameTxt.getText().toString());
        lastNameTxt.setText(lastName);
        countryCode = cleanNumber(countryCodeTxt.getText().toString());
        countryCodeTxt.setText(countryCode);
        mobileNumber = cleanNumber(mobileNumberTxt.getText().toString());
        mobileNumberTxt.setText(mobileNumber);
        e164 = "+" + countryCode + mobileNumber;

        if (!isValidName(firstName)) {
            firstNameError();
            return;
        }
        if (!isValidName(lastName)) {
            lastNameError();
            return;
        }
        if (!isValidPhone(e164)) {
            phoneError();
            return;
        }
        saveParams();
        register();
    }

    private void saveParams() {
        registerData = new Bundle();
        registerData.putString(KEY_FIRST_NAME, firstName);
        registerData.putString(KEY_LAST_NAME, lastName);
        registerData.putString(KEY_COUNTRY_CODE, countryCode);
        registerData.putString(KEY_MOBILE_NUMBER, mobileNumber);
        registerData.putString(KEY_E164, e164);
        registerData.putString(KEY_AUTH, auth);
        registerData.putString(KEY_MKEY, mkey);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (registerData != null) {
            for (String key : registerData.keySet()) {
                outState.putString(key, registerData.getString(key));
            }
        }
        super.onSaveInstanceState(outState);
    }

    private boolean isValidPhone(String p) {
        PhoneNumberUtil pu = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber pn;
        try {
            pn = pu.parse(p, "US");
        } catch (NumberParseException e) {
            return false;
        }

        if (pu.isValidNumber(pn))
            return true;
        else
            return false;
    }

    private boolean isValidName(String name) {
        if (name != null && !name.isEmpty())
            return true;
        else
            return false;
    }

    private String cleanNumber(String num) {
        return num.replaceAll("\\D", "");
    }

    private String cleanName(String name) {
        String r = name.replaceAll("\\W", "");
        return r.replaceAll("\\d", "");
    }

    private void register() {
        Uri.Builder ub = new Uri.Builder();
        ub.appendPath("reg").appendPath("reg");
        LinkedTreeMap<String, String> r = userParams();
        r.put(UserFactory.ServerParamKeys.VERIFICATION_VIA, UserFactory.VerificationCodeVia.SMS);
        DebugConfig config = DebugConfig.getInstance(getContext());
        if (config.shouldForceConfirmationSms()) {
            r.put(UserFactory.ServerParamKeys.VERIFICATION_FORCE_SMS, "true");
        } else if (config.shouldForceConfirmationCall()) {
            r.put(UserFactory.ServerParamKeys.VERIFICATION_FORCE_CALL, "true");
        }
        new Register(ub.build().toString(), r);
    }

    private LinkedTreeMap<String, String> userParams() {
        LinkedTreeMap<String, String> r = new LinkedTreeMap<>();
        r.put(UserFactory.ServerParamKeys.DEVICE_PLATFORM, "android");
        r.put(UserFactory.ServerParamKeys.FIRST_NAME, firstName);
        r.put(UserFactory.ServerParamKeys.LAST_NAME, lastName);
        r.put(UserFactory.ServerParamKeys.MOBILE_NUMBER, e164);
        r.put(UserFactory.ServerParamKeys.VERIFICATION_CODE, verificationCode);
        return r;
    }

    class Register extends HttpRequest {

        public Register(String uri, LinkedTreeMap<String, String> params) {
            super(uri, params, new Callbacks() {
                @Override
                public void success(String response) {
                    dismissProgressDialog();
                    didRegister(response);
                }
                @Override
                public void error(String errorString) {
                    dismissProgressDialog();
                    serverError();
                }
            });
            showProgressDialog(R.string.dialog_checking_title);
        }
    }

    class RequestCall extends HttpRequest {

        public RequestCall(String uri, LinkedTreeMap<String, String> params) {
            super(uri, params, new Callbacks() {
                @Override
                public void success(String response) {
                    if (enterCodeDialog != null) {
                        ((EnterCodeDialogFragment) enterCodeDialog).setCalling();
                    }
                }
                @Override
                public void error(String errorString) {
                    serverError();
                }
            });
        }
    }

    //-------------------------
    // Handle verification code
    //-------------------------
    public void didRegister(String r) {
        LinkedTreeMap<String, String> params = StringUtils.linkedTreeMapWithJson(r);
        if (params == null) {
            serverError();
            return;
        }
        Log.i(TAG, "didRegister: " + params.toString());

        if ( HttpRequest.isSuccess(params.get(HttpRequest.ParamKeys.RESPONSE_STATUS)) ){
            auth = params.get(UserFactory.ServerParamKeys.AUTH);
            mkey = params.get(UserFactory.ServerParamKeys.MKEY);
            saveParams();
            showVerificationDialog();
        } else {
            String title = params.get(HttpRequest.ParamKeys.ERROR_TITLE);
            String msg = params.get(HttpRequest.ParamKeys.ERROR_MSG);
            showErrorDialog(title, msg);
        }
    }

    private void showVerificationDialog() {
        enterCodeDialog = (DialogFragment) getChildFragmentManager().findFragmentByTag("enterCdDlg");
        if (enterCodeDialog != null) {
            enterCodeDialog.dismissAllowingStateLoss();
        }
        enterCodeDialog = EnterCodeDialogFragment.getInstance(e164, this); // is need to update dialog
        enterCodeDialog.show(getChildFragmentManager(), "enterCdDlg");
    }

    @Override
    public void didEnterCode(String code) {
        verificationCode = code;
        Uri.Builder ub = new Uri.Builder();
        String uri = ub.appendPath("reg") .appendPath("verify_code").build().toString();
        new SendCode(uri, userParams(), mkey, auth);
    }

    @Override
    public void requestCall() {
        Uri.Builder ub = new Uri.Builder();
        ub.appendPath("reg").appendPath("reg");
        LinkedTreeMap<String, String> r = userParams();
        r.put(UserFactory.ServerParamKeys.VERIFICATION_VIA, UserFactory.VerificationCodeVia.CALL);
        new RequestCall(ub.build().toString(), r);
    }

    private class SendCode extends HttpRequest{
        public SendCode(String uri, LinkedTreeMap<String, String> params, String mkey, String auth) {
            super(uri, params, mkey, auth, new HttpRequest.Callbacks() {
                @Override
                public void success(String response) {
                    dismissProgressDialog();
                    didReceiveCodeResponse(response);
                }

                @Override
                public void error(String errorString) {
                    dismissProgressDialog();
                    serverError();
                }

            });
            showProgressDialog(R.string.dialog_verifying_title);
        }
    }

    public void didReceiveCodeResponse(String r) {
        Log.i(TAG, "didReceiveCodeResponse: " + r);
        Gson g = new Gson();
        LinkedTreeMap<String,String> params;
        try {
            params = g.fromJson(r, LinkedTreeMap.class);
        } catch (JsonSyntaxException e) {
            serverError();
            return;
        }
        if ( HttpRequest.isSuccess(params.get(HttpRequest.ParamKeys.RESPONSE_STATUS)) ){
            gotUser(params);
        } else {
            showErrorDialog(getString(R.string.dialog_register_bad_code_title), getString(R.string.dialog_register_bad_code_message));
        }
    }

    //----------------
    // Click listeners
    //----------------
    private void setupListeners() {
        countryCodeTxt.setAdapter(new CountryCodeAdapter(getContext()));
        countryCodeTxt.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mobileNumberTxt.requestFocus();
            }
        });
        countryCodeTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && s.length() >= 4) {
                    mobileNumberTxt.requestFocus();
                }
            }
        });
        mobileNumberTxt.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    if (mobileNumberTxt.length() == 0) {
                        countryCodeTxt.requestFocus();
                    }
                }
                return false;
            }
        });
        countryCodeTxt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    int maxWidth = Convenience.dpToPx(getContext(), 350);
                    int width = lastNameTxt.getWidth();
                    countryCodeTxt.setDropDownWidth(Math.min(width, maxWidth));
                }
            }
        });
        appLogo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if ("000".equals(countryCodeTxt.getText().toString())) {
                    debugPage();
                }
                return true;
            }
        });
    }

    // -------------
    // Error dialogs
    //--------------
    private void phoneError() {
        showErrorDialog(getString(R.string.dialog_register_phone_error_title), getString(R.string.dialog_register_phone_error_message));
    }

    private void lastNameError() {
        showErrorDialog(getString(R.string.dialog_register_last_name_error_title), getString(R.string.dialog_register_last_name_error_message));
    }

    private void firstNameError() {
        showErrorDialog(getString(R.string.dialog_register_first_name_title), getString(R.string.dialog_register_first_name_message));
    }

    private void serverError(){
        showErrorDialog(getString(R.string.dialog_server_error_title),
                getString(R.string.dialog_server_error_message, Config.appName));
    }

    private void showErrorDialog(String title, String message) {
        DialogShower.showInfoDialog(getActivity(), title, message);
    }

    //-------------
    // Add shortcut
    //-------------
    private void addShortcutIcon(){

        Intent shortcutIntent = new Intent(getContext(), MainActivity.class);

        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra("duplicate", false);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, Config.appName);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getContext(), R.drawable.ic_launcher));
        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        getContext().sendBroadcast(addIntent);
    }

    private void showProgressDialog(@StringRes int message) {
        dismissProgressDialog();
        pd = ProgressDialogFragment.getInstance(null, getString(message));
        pd.show(getChildFragmentManager(), null);
    }

    private void dismissProgressDialog() {
        if (pd != null) {
            pd.dismissAllowingStateLoss();
            pd = null;
        }
    }

    private class RegSyncWelcomedFriends extends RemoteStorageHandler.GetWelcomedFriends {

        @Override
        protected void gotWelcomedFriends(List<String> mkeys) {
            if (mkeys != null && !mkeys.isEmpty()) {
                List<Friend> friends = FriendFactory.getFactoryInstance().all();
                for (Friend friend : friends) {
                    String mkey = friend.getMkey();
                    if (mkeys.contains(mkey)) {
                        friend.setEverSent(true);
                        mkeys.remove(mkey);
                    } else {
                        friend.setEverSent(false);
                    }
                }
            }
            RemoteStorageHandler.setWelcomedFriends();
            Features features = new Features(getActivity());
            features.checkAndUnlock();
            getAWSCredentials();
        }

        @Override
        protected void failure() {
            dismissProgressDialog();
            serverError();
        }
    }

    //---------------------
    // Add user and friends
    //---------------------
    private void gotUser(LinkedTreeMap<String, String> params) {
        user.set(User.Attributes.FIRST_NAME, params.get(UserFactory.ServerParamKeys.FIRST_NAME));
        user.set(User.Attributes.LAST_NAME, params.get(UserFactory.ServerParamKeys.LAST_NAME));
        user.set(User.Attributes.MOBILE_NUMBER, params.get(UserFactory.ServerParamKeys.MOBILE_NUMBER));
        user.set(User.Attributes.ID, params.get(UserFactory.ServerParamKeys.ID));
        user.set(User.Attributes.MKEY, params.get(UserFactory.ServerParamKeys.MKEY));
        user.set(User.Attributes.AUTH, params.get(UserFactory.ServerParamKeys.AUTH));
        new RegFriendGetter(getContext(), true).getFriends();
    }

    private class RegFriendGetter extends FriendGetter {
        public RegFriendGetter(Context c, boolean destroyAll) {
            super(c, destroyAll);
            showProgressDialog(R.string.dialog_syncing_title);
        }

        @Override
        protected void success() {
            new RegSyncWelcomedFriends();
        }

        @Override
        protected void failure() {
            dismissProgressDialog();
            serverError();
        }
    }

    //-------------------
    // Get S3 credentials
    //-------------------
    private void getAWSCredentials(){
        new RegS3CredentialsGetter(getContext());
    }

    private class RegS3CredentialsGetter extends S3CredentialsGetter {
        public RegS3CredentialsGetter(Context c) {
            super(c);
        }
        @Override
        public void success() {
            regComplete();
        }
        @Override
        public void failure() {
            dismissProgressDialog();
            serverError();
        }
    }

    private void regComplete() {
        user.set(User.Attributes.REGISTERED, "true");
        ActiveModelsHandler.getInstance(getContext()).saveAll();
        dismissProgressDialog();
        ZazoAnalytics.trackEvent(AFInAppEventType.COMPLETE_REGISTRATION);
        publishResult(0, null);
    }

    private void debugPage() {
        Intent intent = new Intent(getActivity(), DebugSettingsActivity.class);
        intent.putExtra(DebugSettingsActivity.EXTRA_FROM_REGISTER_SCREEN, true);
        startActivityForResult(intent, DEBUG_SCREEN_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DEBUG_SCREEN_CODE) {
            countryCodeTxt.setText("");
            user = UserFactory.current_user();
            if (User.isRegistered(getContext())) {
                getAWSCredentials();
            }
        }
    }
}
