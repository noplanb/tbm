package com.noplanbees.tbm;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

public class RegisterActivity extends Activity{
	private final String TAG = this.getClass().getSimpleName();
	private UserFactory userFactory;
	private User user;
	private FriendFactory friendFactory;
	private ProgressDialog progress;

	private ArrayList<LinkedTreeMap<String, String>> friendList = new ArrayList<LinkedTreeMap<String,String>>();
	private LinkedTreeMap<String, String> userParams = new LinkedTreeMap<String, String>();
	
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

	private void init(){
		userFactory = UserFactory.getFactoryInstance();
		userFactory.destroyAll(this);
		user = userFactory.makeInstance(this);
		friendFactory = FriendFactory.getFactoryInstance();
	}

	private void getUser() {	
		EditText phoneView = (EditText) findViewById(R.id.phoneNumber);
		String phoneNumber = phoneView.getText().toString().replaceAll(" ", "");
		new GetUser("/reg/get_user?mobile_number=" + Uri.encode(phoneNumber));
	}

	class GetUser extends Server{
		public GetUser(String uri) {
			super(uri);
			progress.show();
		}
		@Override
		public void success(String response) {	
			progress.dismiss();
			gotUser(response);
		}
		@Override
		public void error(String errorString) {
			progress.dismiss();
			serverError();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void gotUser(String r){
		Log.i(TAG, "gotUser: " + r);
		Gson g = new Gson();
		userParams = g.fromJson(r, userParams.getClass());
		Log.i(TAG, "gotUser: " + userParams.toString());
		if (userParams.isEmpty()){
			badPhoneError();
			return;
		}
		user.set(User.Attributes.FIRST_NAME, userParams.get("first_name"));
		user.set(User.Attributes.LAST_NAME, userParams.get("last_name"));
		user.set(User.Attributes.MOBILE_NUMBER, userParams.get("mobile_number"));
		user.set(User.Attributes.ID, userParams.get("id").toString());
		user.set(User.Attributes.MKEY, userParams.get("mkey"));
		user.set(User.Attributes.AUTH, userParams.get("auth"));
		new GetFriends("/reg/get_friends?mkey=" + user.get(User.Attributes.MKEY));
	}
	
	class GetFriends extends Server{

		public GetFriends(String uri) {
			super(uri);
			progress.show();
		}

		@Override
		public void success(String response) {	
			progress.dismiss();
			gotFriends(response);
		}

		@Override
		public void error(String errorString) {
			progress.dismiss();
			serverError();
		}
	}

	@SuppressWarnings("unchecked")
	public void gotFriends(String r) {
		Gson g = new Gson();
		friendList = g.fromJson(r, friendList.getClass());
		Log.i(TAG, "gotRegResponse: " + friendList.toString());
		friendFactory.destroyAll(this);
		Integer i = 0;
		
		for (LinkedTreeMap<String, String> fm : friendList){
			FriendFactory.addFriendFromServerParams(this, fm);
			i ++;
		}
		user.set(User.Attributes.REGISTERED, "true");
		regComplete();
	}
	
	private void setupListeners(){
		Button enterBtn = (Button) findViewById(R.id.btnEnter);
		enterBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				getUser();
			}
		});
	}

	private void setupProgressDialog(){
		progress = new ProgressDialog(this);
		progress.setTitle("Checking");
	}
	
	private void regComplete() {
		ActiveModelsHandler.saveAll(this);
		Intent i = new Intent(this, HomeActivity.class);
		startActivity(i);
		finish();
	}
	
	// -------------
	// Error dialogs
	//--------------
	private void badPhoneError(){
		showErrorDialog("No user found with that phone number. Check the number or contact Sani for assistance.\n\n   Sani Elfishawy\n   ph: 650-245-3537\n   e: sani@sbcglobal.net");
	}
	
	private void serverError(){
		showErrorDialog("Can't reach ThreeByMe.\n\nCheck your connectivity and try again.");
	}
	
	private void showErrorDialog(String message){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Error")
		.setMessage(message)
		.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
			}
		})
		.create().show();
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
