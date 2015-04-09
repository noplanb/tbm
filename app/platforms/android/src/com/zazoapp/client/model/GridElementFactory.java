package com.zazoapp.client.model;

public class GridElementFactory extends ActiveModelFactory<GridElement> {

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
}
