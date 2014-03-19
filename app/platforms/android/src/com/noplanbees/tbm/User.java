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
		UserFactory cf = UserFactory.getFactoryInstance();
		return cf.hasInstances() && cf.instances.get(0).get("registered").startsWith("t");
	}
	

	
}
