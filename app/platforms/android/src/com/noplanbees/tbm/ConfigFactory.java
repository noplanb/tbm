package com.noplanbees.tbm;

public class ConfigFactory extends ActiveModelFactory {

	public static ConfigFactory instance = null;
	
	public static ConfigFactory getFactoryInstance(){
		if ( instance == null ) 
			instance = new ConfigFactory();
		return instance;
	}
	
	@Override
	protected Config makeInstance() {
		if ( instances.isEmpty() ){
			Config i = new Config();
			i.init();
			instances.add(i);
			return i;	
		} else {
			return (Config) instances.get(0);
		}
	}
}
