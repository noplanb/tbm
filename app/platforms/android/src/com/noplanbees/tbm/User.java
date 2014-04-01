package com.noplanbees.tbm;

import java.io.File;

import android.os.Environment;
import android.util.Log;

public class User extends ActiveModel{
    
	
	@Override
	public String[] attributeList() {
      final String[] a = {"id", "firstName", "lastName", "registered"};
      return a;
	}
	
	public static boolean isRegistered(){
		UserFactory uf = ActiveModelsHandler.ensureUser();
		return uf.hasInstances() && uf.instances.get(0).get("registered").startsWith("t");
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
    	return get("id");
    }
	
}
