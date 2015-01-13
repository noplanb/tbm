package com.noplanbees.tbm;

import android.content.Context;

import java.util.ArrayList;


public class GridElementFactory extends ActiveModelFactory {

	private static GridElementFactory instance = null;

	public static GridElementFactory getFactoryInstance(){
		if ( instance == null ) 
			instance = new GridElementFactory();
		return instance;
	}

	@Override
	public GridElement makeInstance(Context context) {
		GridElement i = new GridElement();
		i.init(context);
		instances.add(i);
		return i;	
	}
	
	public ArrayList<GridElement> all(){
		ArrayList<GridElement> r = new ArrayList<GridElement>();
		for (ActiveModel a : instances){
			r.add((GridElement) a); 
		}
		return r;
	}
	
	public GridElement getGridElementByFriendId(String friendId){
		for (GridElement ge : all()){
			if (ge.getFriendId().equals(friendId))
				return ge;
		}
		return null;
	}
	
	public GridElement findWithFriend(Friend f){
		if (f==null)
			return null;
		
		return findWithFriendId(f.getId());
	}

	public GridElement findWithFriendId(String friendId) {
		return (GridElement) findWhere(GridElement.Attributes.FRIEND_ID, friendId);
	}
	
	public GridElement firstEmptyGridElement(){
		for (GridElement ge : all()){
			if (!ge.hasFriend())
				return ge;
		}
		return null;
	}
	
	public boolean friendIsOnGrid(Friend f){
		GridElement ge = findWithFriendId(f.getId());
		return ge == null ? false : true;
	}
}
