package com.noplanbees.tbm;

public class UserFactory extends ActiveModelFactory {

	public static UserFactory instance = null;
	
	public static UserFactory getFactoryInstance(){
		if ( instance == null ) 
			instance = new UserFactory();
		return instance;
	}
	
	@Override
	protected User makeInstance() {
		if ( instances.isEmpty() ){
			User i = new User();
			i.init();
			instances.add(i);
			return i;	
		} else {
			return (User) instances.get(0);
		}
	}
}
