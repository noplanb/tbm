package com.zazoapp.client.ui;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.zazoapp.client.Config;
import com.zazoapp.client.ContactsManager;
import com.zazoapp.client.DataHolderService;
import com.zazoapp.client.DataHolderService.LocalBinder;
import com.zazoapp.client.FriendGetter;
import com.zazoapp.client.R;
import com.zazoapp.client.debug.DebugSettingsActivity;
import com.zazoapp.client.model.ActiveModelsHandler;
import com.zazoapp.client.model.Contact;
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.network.HttpRequest;
import com.zazoapp.client.network.aws.S3CredentialsGetter;
import com.zazoapp.client.ui.dialogs.EnterCodeDialogFragment;
import com.zazoapp.client.ui.dialogs.ProgressDialogFragment;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.DialogShower;

public class RegisterActivity extends Activity implements EnterCodeDialogFragment.Callbacks{
    private static final int DEBUG_SCREEN_CODE = 293;
	private static final String TAG = RegisterActivity.class.getSimpleName();
	private UserFactory userFactory;
	private User user;

	private String firstName;
	private String lastName;
	private String countryCode;
	private String mobileNumber;
	private String e164;
	private String verificationCode;
	private String auth;
	private String mkey;

	private EditText firstNameTxt;
	private EditText lastNameTxt;
	private EditText countryCodeTxt;
	private EditText mobileNumberTxt;

    private DialogFragment pd;

	protected ActiveModelsHandler activeModelsHandler;
	private ServiceConnection conn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			activeModelsHandler = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			activeModelsHandler = ((LocalBinder) service).getActiveModelsHandler();

			onLoadComplete();
		}
	};
	private DialogFragment enterCodeDialog;

	//----------
	// LifeCycle
	//----------
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getActionBar().hide();
		setContentView(R.layout.register);
		setupListeners();

        setAdditionalViewHeight();
        setUpView();
	}

    private void setAdditionalViewHeight() {
        View additionalView = findViewById(R.id.transparent_view);
        ViewGroup.LayoutParams lp = additionalView.getLayoutParams();
        lp.height = (int) (Convenience.getScreenDimensions(this).y * 0.6);
        additionalView.setLayoutParams(lp);
    }

    @Override
	protected void onStart() {
		super.onStart();
		pd = ProgressDialogFragment.getInstance("Data", "retrieving data...");
        pd.show(getFragmentManager(), null);
		bindService(new Intent(this, DataHolderService.class), conn, Service.BIND_IMPORTANT);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if(pd != null)
			pd.dismiss();
		unbindService(conn);
	}

	private void onLoadComplete(){
		init();
		pd.dismiss();
	}

	//-----
	// Init
	//-----
	private void init(){
		userFactory = UserFactory.getFactoryInstance();
		userFactory.destroyAll(this);
		user = userFactory.makeInstance(this);
	}

	//----------
	// SetupView 
	//----------
	private void setUpView(){
		initTxtFields();
		//prefillTextFields(); issue 250
	}

	private void initTxtFields() {
		firstNameTxt = (EditText) findViewById(R.id.first_name_txt);
		lastNameTxt = (EditText) findViewById(R.id.last_name_txt);
		countryCodeTxt = (EditText) findViewById(R.id.country_code_txt);
		mobileNumberTxt = (EditText) findViewById(R.id.mobile_number_text);		
	}

	private void prefillTextFields() {
		Contact contact = new ContactsManager(this).userProfile(this);
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
	private void registerUser() {	
		firstName = cleanName(firstNameTxt.getText().toString());
		firstNameTxt.setText(firstName);
		lastName = cleanName(lastNameTxt.getText().toString());
		lastNameTxt.setText(lastName);
		countryCode = cleanNumber(countryCodeTxt.getText().toString());
		countryCodeTxt.setText(countryCode);
		mobileNumber = cleanNumber(mobileNumberTxt.getText().toString());
		mobileNumberTxt.setText(mobileNumber);
        String newE164 = "+" + countryCode + mobileNumber;
        if (!newE164.equals(e164)) {
            enterCodeDialog = null; // is need to update dialog
        }
        e164 = newE164;

		if (!isValidName(firstName)){
			firstNameError();
			return;
		}
		if (!isValidName(lastName)){
			lastNameError();
			return;
		}
		if (!isValidPhone(e164)){
			phoneError();
			return;
		}
		register();
	}


	private boolean isValidPhone(String p) {
		PhoneNumberUtil pu = PhoneNumberUtil.getInstance();
		PhoneNumber pn;
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

	private void register(){
		Uri.Builder ub = new Uri.Builder();
		ub.appendPath("reg")
		.appendPath("reg");
		new Register(ub.build().toString(), userParams());
	}

	private LinkedTreeMap<String, String> userParams(){
		LinkedTreeMap<String, String> r = new LinkedTreeMap<String, String>();
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
                    pd.dismiss();
                    didRegister(response);
                }
                @Override
                public void error(String errorString) {
                    pd.dismiss();
                    serverError();
                }
            });
            pd = ProgressDialogFragment.getInstance(getString(R.string.dialog_checking_title), null);
			pd.show(getFragmentManager(), null);
		}
	}

	//-------------------------
	// Handle verification code 
	//-------------------------
	@SuppressWarnings("unchecked")
	public void didRegister(String r) {
		Gson g = new Gson();
		LinkedTreeMap<String, String> params = new LinkedTreeMap<String,String>();
		params = g.fromJson(r, params.getClass());
		Log.i(TAG, "didRegister: " + params.toString());

		if ( HttpRequest.isSuccess(params.get(HttpRequest.ParamKeys.RESPONSE_STATUS)) ){
			auth = params.get(UserFactory.ServerParamKeys.AUTH);
			mkey = params.get(UserFactory.ServerParamKeys.MKEY);
			showVerificationDialog();
		} else {
			String title = params.get(HttpRequest.ParamKeys.ERROR_TITLE);
			String msg = params.get(HttpRequest.ParamKeys.ERROR_MSG);
			showErrorDialog(title, msg);
		}
	}

	private void showVerificationDialog() {
		if(enterCodeDialog == null)
			enterCodeDialog = EnterCodeDialogFragment.getInstance(e164, this);
		enterCodeDialog.show(getFragmentManager(), "enterCdDlg");
	}

	@Override
	public void didEnterCode(String code) {
		verificationCode = code;
		Uri.Builder ub = new Uri.Builder();
		String uri = ub.appendPath("reg") .appendPath("verify_code").build().toString();
		new SendCode(uri, userParams(), mkey, auth);
	}


	private class SendCode extends HttpRequest{
		public SendCode(String uri, LinkedTreeMap<String, String> params, String mkey, String auth) {
			super(uri, params, mkey, auth, new HttpRequest.Callbacks() {
                @Override
                public void success(String response) {
                    pd.dismiss();
                    didReceiveCodeResponse(response);
                }

                @Override
                public void error(String errorString) {
                    pd.dismiss();
                    serverError();
                }

            });
            pd = ProgressDialogFragment.getInstance(getString(R.string.dialog_checking_title), null);
			pd.show(getFragmentManager(), null);
		}
	}

	@SuppressWarnings("unchecked")
	public void didReceiveCodeResponse(String r) {
		Log.i(TAG, "didReceiveCodeResponse: " + r);
		Gson g = new Gson();
		LinkedTreeMap<String,String> params = new LinkedTreeMap<String, String>();
		params = g.fromJson(r, params.getClass());
		if ( HttpRequest.isSuccess(params.get(HttpRequest.ParamKeys.RESPONSE_STATUS)) ){
			gotUser(params);
		} else {
			showErrorDialog(getString(R.string.dialog_register_bad_code_title), getString(R.string.dialog_register_bad_code_message));
		}
	}

	//---------------
	// Debug_get_user
	//---------------
	private void debugGetUser(){
		LinkedTreeMap<String, String>params = new LinkedTreeMap<String, String>();
		params.put(UserFactory.ServerParamKeys.MOBILE_NUMBER, cleanNumber(mobileNumberTxt.getText().toString()));
		params.put("country_code", cleanNumber(countryCodeTxt.getText().toString()));
		Uri.Builder ub = new Uri.Builder();
		ub.appendPath("reg").appendPath("debug_get_user");
		new DebugGetUser(ub.build().toString(), params);
	}
	
	private class DebugGetUser extends HttpRequest{
		public DebugGetUser(String uri, LinkedTreeMap<String, String> params) {
			super(uri, params, new Callbacks() {
                @Override
                public void success(String response) {
                    pd.dismiss();
                    didReceiveCodeResponse(response);
                }
                @Override
                public void error(String errorString) {
                    pd.dismiss();
                    serverError();
                }
            });
            pd = ProgressDialogFragment.getInstance(getString(R.string.dialog_checking_title), null);
            pd.show(getFragmentManager(), null);
		}
	}

    private void debugPage() {
        Intent intent = new Intent(this, DebugSettingsActivity.class);
        intent.putExtra(DebugSettingsActivity.EXTRA_SERVER_OPTION, true);
        startActivityForResult(intent, DEBUG_SCREEN_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DEBUG_SCREEN_CODE) {
            firstNameTxt.setText("");
            lastNameTxt.setText("");
            countryCodeTxt.setText("");
            mobileNumberTxt.setText("");
        }
    }

    //---------------------
	// Add user and friends
	//---------------------
	private void gotUser(LinkedTreeMap<String, String> params){
		user.set(User.Attributes.FIRST_NAME, params.get(UserFactory.ServerParamKeys.FIRST_NAME));
		user.set(User.Attributes.LAST_NAME, params.get(UserFactory.ServerParamKeys.LAST_NAME));
		user.set(User.Attributes.MOBILE_NUMBER, params.get(UserFactory.ServerParamKeys.MOBILE_NUMBER));
		user.set(User.Attributes.ID, params.get(UserFactory.ServerParamKeys.ID)).toString();
		user.set(User.Attributes.MKEY, params.get(UserFactory.ServerParamKeys.MKEY));
		user.set(User.Attributes.AUTH, params.get(UserFactory.ServerParamKeys.AUTH));
		user.set(User.Attributes.REGISTERED, "true");
		new RegFriendGetter(this, true).getFriends();
	}
	
	private class RegFriendGetter extends FriendGetter{
        public RegFriendGetter(Context c, boolean destroyAll) {
            super(c, destroyAll);
            pd = ProgressDialogFragment.getInstance(getString(R.string.dialog_checking_title), null);
            pd.show(getFragmentManager(), null);
        }
        
        @Override
        protected void success() {
            pd.dismiss();
            getAWSCredentials();
        }

        @Override
        protected void failure() {
            pd.dismiss();
            serverError();
        }
	}

	//-------------------
	// Get S3 credentials
	//-------------------
    private void getAWSCredentials(){
        new RegS3CredentialsGetter(this);
    }
    
    private class RegS3CredentialsGetter extends S3CredentialsGetter{
        public RegS3CredentialsGetter(Context c) {
            super(c);
        }
        @Override
        public void success() {
            regComplete();
        }
        @Override
        public void failure() {
            serverError();            
        }
    }

	private void regComplete() {
		UserFactory.current_user().set(User.Attributes.REGISTERED, "true");
		activeModelsHandler.saveAll();
		Intent i = new Intent(this, MainActivity.class);
		startActivity(i);
		finish();
	}

    
    //----------------
    // Click listeners
    //----------------
	private void setupListeners(){
		Button enterBtn = (Button) findViewById(R.id.enter_btn);
		enterBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				registerUser();
			}
		});
		
		Button debugBtn = (Button) findViewById(R.id.debug_btn);
		debugBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {	
				debugGetUser();
			}
		});
        debugBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                debugPage();
                return true;
            }
        });

        findViewById(R.id.app_logo).setOnLongClickListener(new View.OnLongClickListener() {
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
        showErrorDialog(getString(R.string.dialog_register_server_error_title),
                getString(R.string.dialog_register_server_error_message, Config.appName));
	}

    private void showErrorDialog(String title, String message) {
        DialogShower.showInfoDialog(this, title, message);
    }

	//-------------
	// Add shortcut
	//-------------
	private void addShortcutIcon(){

		Intent shortcutIntent = new Intent(getApplicationContext(), MainActivity.class);

		Intent addIntent = new Intent();
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		addIntent.putExtra("duplicate", false);
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, Config.appName);
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.drawable.ic_launcher));
		addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
		getApplicationContext().sendBroadcast(addIntent);
	}


}
