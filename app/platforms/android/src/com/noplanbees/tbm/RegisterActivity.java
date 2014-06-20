package com.noplanbees.tbm;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

public class RegisterActivity extends Activity{
	private final String TAG = this.getClass().getSimpleName();
	private UserFactory userFactory;
	private User user;
	private FriendFactory friendFactory;

	private ArrayList<LinkedTreeMap<String, String>> userList = new ArrayList<LinkedTreeMap<String,String>>();
	private ArrayList<LinkedTreeMap<String, String>> friendList = new ArrayList<LinkedTreeMap<String,String>>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
		init();
		setContentView(R.layout.register);
		new GetUserList("reg/user_list");
	}

	private void init(){
		userFactory = UserFactory.getFactoryInstance();
		userFactory.destroyAll();
		user = userFactory.makeInstance();
		friendFactory = FriendFactory.getFactoryInstance();
	}

	class GetUserList extends Server{

		public GetUserList(String uri) {
			super(uri);
		}

		@Override
		public void callback(String response) {
			gotUserList(response);
		}
	}

	private void gotUserList(String ulj){
		Log.i(TAG, "gotUserList: " + ulj); 
		Gson g = new Gson();
		userList =  g.fromJson(ulj, userList.getClass());
		Log.i(TAG, "userlist = " + userList.toString());
		render();
	}

	private void render(){
		ViewGroup r = (ViewGroup) findViewById(R.id.register);
		int index = 0;
		for(LinkedTreeMap<String, String> u : userList){
			Log.d(TAG, "Adding button for " + u.get("first_name"));
			Button b = new Button(this);
			b.setText(u.get("first_name") + " " + u.get("last_name"));			
			b.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			b.setId(index);
			b.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {	
					userSelected(v);
				}
			});
			r.addView(b);
			index ++;
		}
	}

	protected void userSelected(View v) {	
		int index = v.getId();
		LinkedTreeMap<String, String> u = userList.get(index);	
		Log.i(TAG, "userSelected: " + u.toString());
		user.set(User.Attributes.FIRST_NAME, u.get("first_name"));
		user.set(User.Attributes.LAST_NAME, u.get("last_name"));
		user.set(User.Attributes.ID, u.get("id"));
		Log.i(TAG, "User set to: " + user.attributes.toString());
		new RegisterUser("/reg/register/" + user.getId());
	}

	class RegisterUser extends Server{

		public RegisterUser(String uri) {
			super(uri);
		}

		@Override
		public void callback(String response) {	
			gotRegResponse(response);
		}
	}

	public void gotRegResponse(String r) {
		Gson g = new Gson();
		friendList = g.fromJson(r, friendList.getClass());
		Log.i(TAG, "gotRegResponse: " + friendList.toString());
		friendFactory.destroyAll();
		Integer i = 0;
		for (LinkedTreeMap<String, String> fm : friendList){
			Friend f = friendFactory.makeInstance();
			f.set(Friend.Attributes.FIRST_NAME, fm.get("first_name"));
			f.set(Friend.Attributes.LAST_NAME, fm.get("last_name"));
			f.set(Friend.Attributes.ID, fm.get("id"));
			f.set(Friend.Attributes.VIEW_INDEX, i.toString());
			i ++;
		}
		user.set(User.Attributes.REGISTERED, "true");
		regComplete();
	}

	private void regComplete() {
		ActiveModelsHandler.saveAll();
		Intent i = new Intent(this, HomeActivity.class);
		startActivity(i);
		finish();
	}
}
