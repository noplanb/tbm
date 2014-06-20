package com.noplanbees.tbm;

import java.io.File;

import android.os.Environment;
import android.util.Log;

public class User extends ActiveModel{
    
	public static class Attributes{
		public static final String ID = "id";
		public static final String FIRST_NAME = "firstName";
		public static final String LAST_NAME = "lastName";
		public static final String REGISTERED = "registered";
	}
	
	@Override
	public String[] attributeList() {
      final String[] a = {
    		  Attributes.ID, 
    		  Attributes.FIRST_NAME,
    		  Attributes.LAST_NAME,
    		  Attributes.REGISTERED};
      return a;
	}
	
	public static boolean isRegistered(){
		UserFactory uf = ActiveModelsHandler.ensureUser();
		return uf.hasInstances() && uf.instances.get(0).get(User.Attributes.REGISTERED).startsWith("t");
	}
	
	public static String userId(){
		String id = null;
		UserFactory uf = ActiveModelsHandler.ensureUser();
		if (uf.hasInstances()){
			id = uf.instances.get(0).getId();
		}
		return id;
	}
	
    public String getId(){
    	return get(User.Attributes.ID);
    }
	
}
