package com.noplanbees.tbm;

import java.util.ArrayList;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

public class GridElementFactory extends ActiveModelFactory {

	public static GridElementFactory instance = null;

	public static GridElementFactory getFactoryInstance(){
		if ( instance == null ) 
			instance = new GridElementFactory();
		return instance;
	}

	@Override
	protected GridElement makeInstance(Context context) {
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
	
	public GridElement getGridElementWithFrame(View v){
		for (GridElement ge : all()){
			if (ge.frame.getId() == v.getId())
				return ge;
		}
		return null;
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
