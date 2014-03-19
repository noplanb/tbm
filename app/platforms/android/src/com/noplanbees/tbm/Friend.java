package com.noplanbees.tbm;

public class Friend extends ActiveModel{

	@Override
	public String[] attributeList() {
      final String[] a = {"id", "viewIndex", "viewId", "firstName", "lastName", "videoPath", "videoViewed"};
      return a;
	}
	
}
