package com.zazoapp.client.model;

import java.util.ArrayList;

public class GridElementFactory extends ActiveModelFactory<GridElement> implements GridElement.GridElementChangedCallback {

	private static GridElementFactory instance = null;

	public static GridElementFactory getFactoryInstance(){
		if ( instance == null ) 
			instance = new GridElementFactory();
		return instance;
	}

	public GridElement get(int i){
		return all().get(i);
	}

	public GridElement findWithFriendId(String friendId) {
		return findWhere(GridElement.Attributes.FRIEND_ID, friendId);
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

	public int gridElementIndexWithFriend(Friend f) {
		GridElement ge = findWithFriendId(f.getId());
		if (ge == null)
			return -1;
		
		return all().indexOf(ge);
	}

    @Override
    public Class<GridElement> getModelClass() {
        return GridElement.class;
    }

    @Override
    protected boolean checkAndNormalize() {
        boolean result = false;
        ArrayList<GridElement> allElements = all();
        for (GridElement ge : allElements) {
            if (ge.hasFriend()) {
                Friend friend = ge.getFriend();
                if (friend == null || !friend.validate()) {
                    ge.setFriend(null, false);
                    result = true;
                }
            }
        }
        return result;
    }

    @Override
    public void onModelUpdated(boolean changed, boolean onlyMoved) {
        onModelUpdated(changed);
    }
}
