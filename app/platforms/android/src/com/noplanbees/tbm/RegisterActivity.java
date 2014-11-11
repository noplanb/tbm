package com.noplanbees.tbm;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.noplanbees.tbm.FriendGetter.FriendGetterCallback;

public class RegisterActivity extends Activity{
	private final String TAG = this.getClass().getSimpleName();
	private UserFactory userFactory;
	private User user;
	private ProgressDialog progress;

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
	private EditText verificationCodeTxt;


	//----------
	// LifeCycle
	//----------
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
		init();
		setContentView(R.layout.register);
		setupListeners();
		setupProgressDialog();
		AddShortcutIcon();
	}

	@Override
	protected void onResume(){
		super.onResume();
		setUpView();
		new VersionHandler(this);
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
		prefillTextFields();
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
		e164 = "+" + countryCode + mobileNumber;

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
		r.put(UserFactory.ServerParamKeys .DEVICE_PLATFORM, "android");
		r.put(UserFactory.ServerParamKeys.FIRST_NAME, firstName);
		r.put(UserFactory.ServerParamKeys.LAST_NAME, lastName);
		r.put(UserFactory.ServerParamKeys.MOBILE_NUMBER, e164);
		r.put(UserFactory.ServerParamKeys.AUTH, auth);
		r.put(UserFactory.ServerParamKeys.MKEY, mkey);
		r.put(UserFactory.ServerParamKeys.VERIFICATION_CODE, verificationCode);
		return r;
	}

	class Register extends Server{

		public Register(String uri, LinkedTreeMap<String, String> params) {
			super(uri, params);
			progress.show();
		}
		@Override
		public void success(String response) {
			progress.dismiss();
			didRegister(response);
		}
		@Override
		public void error(String errorString) {
			progress.dismiss();
			serverError();
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

		if ( Server.isSuccess(params.get(Server.ParamKeys.RESPONSE_STATUS)) ){
			auth = params.get(UserFactory.ServerParamKeys.AUTH);
			mkey = params.get(UserFactory.ServerParamKeys.MKEY);
			showVerificationDialog();
		} else {
			String title = params.get(Server.ParamKeys.ERROR_TITLE);
			String msg = params.get(Server.ParamKeys.ERROR_MSG);
			showErrorDialog(title, msg);
		}
	}

	private void showVerificationDialog() {
		LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

		LinearLayout ll = new LinearLayout(this);
		ll.setLayoutParams(lp);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setPadding(20, 20, 20, 20);

		TextView msgTxt = new TextView(this);
		msgTxt.setText("We sent a code via text message to\n\n" + phoneWithFormat(e164, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL) + ".");
		msgTxt.setLayoutParams(lp);
		msgTxt.setPadding(15, 20, 15, 50);
		msgTxt.setTextSize(17);
		msgTxt.setGravity(Gravity.CENTER);

		verificationCodeTxt = new EditText(this);
		verificationCodeTxt.setLayoutParams(lp);
		verificationCodeTxt.setHint("Enter code");
		msgTxt.setGravity(Gravity.CENTER_HORIZONTAL);
		verificationCodeTxt.setInputType(InputType.TYPE_CLASS_NUMBER);

		ll.addView(msgTxt);
		ll.addView(verificationCodeTxt);

		new AlertDialog.Builder(this)
		.setTitle("Enter Code")
		.setView(ll)
		.setPositiveButton("Enter", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				didEnterCode();
			}
		})
		.setNegativeButton("Cancel", null)
		.show();
	}

	protected void didEnterCode() {
		verificationCode = verificationCodeTxt.getText().toString().replaceAll("\\s+", "");
		Uri.Builder ub = new Uri.Builder();
		String uri = ub.appendPath("reg") .appendPath("verify_code").build().toString();
		new SendCode(uri, userParams());
	}


	private class SendCode extends Server{
		public SendCode(String uri, LinkedTreeMap<String, String> params) {
			super(uri, params);
			progress.show();
		}
		@Override
		public void success(String response) {	
			progress.dismiss();
			didReceiveCodeResponse(response);
		}
		@Override
		public void error(String errorString) {		
			progress.dismiss();
			serverError();
		}
	}

	@SuppressWarnings("unchecked")
	public void didReceiveCodeResponse(String r) {
		Log.i(TAG, "didReceiveCodeResponse: " + r);
		Gson g = new Gson();
		LinkedTreeMap<String,String> params = new LinkedTreeMap<String, String>();
		params = g.fromJson(r, params.getClass());
		if ( Server.isSuccess(params.get(Server.ParamKeys.RESPONSE_STATUS)) ){
			gotUser(params);
		} else {
			showErrorDialog("Bad Code", "The code you enterred is wrong. Please try again.");
		}
	}

	//---------------
	// Debug_get_user
	//---------------
	private void debugGetUser(){
		LinkedTreeMap<String, String>params = new LinkedTreeMap<String, String>();
		params.put(UserFactory.ServerParamKeys.MOBILE_NUMBER, countryCodeTxt.getText().toString() + mobileNumberTxt.getText().toString());
		Uri.Builder ub = new Uri.Builder();
		ub.appendPath("reg").appendPath("debug_get_user");
		new DebugGetUser(ub.build().toString(), params);
	}
	
	private class DebugGetUser extends Server{
		public DebugGetUser(String uri, LinkedTreeMap<String, String> params) {
			super(uri, params);
			progress.show();
		}
		@Override
		public void success(String response) {	
			progress.dismiss();
			didReceiveCodeResponse(response);
		}
		@Override
		public void error(String errorString) {	
			progress.dismiss();
			serverError();
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
		new FriendGetter(this, true, new FriendGetterCallback(){
			@Override
			public void gotFriends() {
				regComplete();
			}
		});
	}

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
	}

	private void setupProgressDialog(){
		progress = new ProgressDialog(this);
		progress.setTitle("Checking");
	}

	private void regComplete() {
		UserFactory.current_user().set(User.Attributes.REGISTERED, "true");
		ActiveModelsHandler.saveAll(this);
		Intent i = new Intent(this, HomeActivity.class);
		startActivity(i);
		finish();
	}

	// -------------
	// Error dialogs
	//--------------
	private void phoneError() {
		showErrorDialog("Bad Number", "Enter your country code and mobile number.");
	}

	private void lastNameError() {
		showErrorDialog("Last Name", "Enter your last name.");
	}

	private void firstNameError() {
		showErrorDialog("First Name", "Enter your first name.");
	}

	private void serverError(){
		showErrorDialog("No Connection", "Can't reach " + Config.appName + ".\n\nCheck your connection and try again.");
	}

	private void showErrorDialog(String title, String message){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title)
		.setMessage(message)
		.setPositiveButton("Ok", null)
		.create().show();
	}

	//-------------
	// Convenience
	//-------------
	private String phoneWithFormat(String phone, PhoneNumberFormat format){
		if (phone == null)
			return null;

		PhoneNumberUtil pu = PhoneNumberUtil.getInstance();
		try {
			PhoneNumber pn = pu.parse(phone, "US");
			return pu.format(pn, format);
		} catch (NumberParseException e) {
			return null;
		}
	}

	//-------------
	// Add shortcut
	//-------------
	private void AddShortcutIcon(){

		Intent shortcutIntent = new Intent(getApplicationContext(), HomeActivity.class);
		//	    shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		//	    shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);


		Intent addIntent = new Intent();
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		addIntent.putExtra("duplicate", false);
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, Config.appName);
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.drawable.ic_launcher));
		addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
		getApplicationContext().sendBroadcast(addIntent);
	}


}
