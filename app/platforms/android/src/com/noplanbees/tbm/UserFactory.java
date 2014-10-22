package com.noplanbees.tbm;

import android.content.Context;

public class UserFactory extends ActiveModelFactory {

	public static class ServerParamKeys{
		public static final String FIRST_NAME = "firstName";
		public static final String LAST_NAME = "last_name";
		public static final String MOBILE_NUMBER = "mobile_number";
		public static final String ID = "id";
		public static final String MKEY = "mkey";
		public static final String AUTH = "auth";
	}
	
	public static UserFactory instance = null;
	
	public static UserFactory getFactoryInstance(){
		if ( instance == null ) 
			instance = new UserFactory();
		return instance;
	}
	
	@Override
	protected User makeInstance(Context context) {
		if ( instances.isEmpty() ){
			User i = new User();
			i.init(context);
			instances.add(i);
			return i;	
		} else {
			return (User) instances.get(0);
		}
	}
	
	public static User current_user(){
		UserFactory uf = UserFactory.getFactoryInstance();
		if (uf.hasInstances()){
			return (User) uf.instances.get(0);
		} else {
			return null;
		}
	}
}
