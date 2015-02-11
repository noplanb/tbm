package com.noplanbees.tbm;

import android.content.Context;
import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.FriendFactory;
import com.noplanbees.tbm.model.GridElement;
import com.noplanbees.tbm.model.GridElementFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class GridManager implements Friend.VideoStatusChangedCallback{
    private static final String TAG = GridManager.class.getSimpleName();
    private static GridManager gridManager;

    public static final int GRID_ELEMENTS_COUNT = 8;

    private GridManager() {
        FriendFactory.getFactoryInstance().addVideoStatusObserver(this);
    }

    public static GridManager getInstance(){
        if(gridManager == null)
            gridManager = new GridManager();
        return gridManager;
    }

    //----------
    // Init grid
    //----------
    public void initGrid(Context context){
        if (GridElementFactory.getFactoryInstance().all().size() == GRID_ELEMENTS_COUNT)
            return;

        GridElementFactory.getFactoryInstance().destroyAll(context);
        ArrayList<Friend> allFriends = FriendFactory.getFactoryInstance().all();
        for (int i = 0; i < GRID_ELEMENTS_COUNT; i++) {
            GridElement g = GridElementFactory.getFactoryInstance().makeInstance(context);
            if (i < allFriends.size()) {
                Friend f = allFriends.get(i);
                g.setFriend(f, hasFriendName(f.getDisplayName()), false);
            }
        }
    }

    public boolean hasEmptySpace(){
        boolean hasEmptySpace = false;
        for (GridElement ge : GridElementFactory.getFactoryInstance().all()){
            if (!ge.hasFriend()) {
                hasEmptySpace = true;
                break;
            }
        }
        return hasEmptySpace;
    }

    public boolean isOnGrid(Friend f){
        boolean isOnGrid = false;
        for (GridElement ge : GridElementFactory.getFactoryInstance().all()){
            if (ge.hasFriend() && ge.getFriend().equals(f)) {
                isOnGrid = true;
                break;
            }
        }
        return isOnGrid;
    }

	public ArrayList<Friend> friendsOnBench(){
		ArrayList<Friend> allFriends = FriendFactory.getFactoryInstance().all();
		ArrayList<Friend> gridFriends = friendsOnGrid();
		for (Friend gf : gridFriends){
			allFriends.remove(gf);
		}
		return allFriends;
	}

    public ArrayList<Friend> friendsOnGrid(){
        ArrayList<Friend> r = new ArrayList<Friend>();
        for (GridElement ge : GridElementFactory.getFactoryInstance().all()){
            if (ge.hasFriend())
                r.add(ge.getFriend());
        }
        return r;
    }

    public void moveFriendToGrid(Friend f){
		rankingActionOccurred(f);
		if (!GridElementFactory.getFactoryInstance().friendIsOnGrid(f)) {
			nextAvailableGridElement().setFriend(f, hasFriendName(f.getDisplayName()));
		}
	}

    private boolean hasFriendName(String name) {
        for (GridElement ge : GridElementFactory.getFactoryInstance().all()) {
            if (ge.hasFriend() && ge.getFriend().getDisplayName().equals(name)) {
                return true;
            }
        }
        return false;
    }

	//--------
    // Ranking
	//--------
	public void rankingActionOccurred(Friend f){
		f.set(Friend.Attributes.TIME_OF_LAST_ACTION, System.currentTimeMillis() + "");
	}

	public ArrayList<Friend> rankedFriendsOnGrid(){
		ArrayList<Friend> fog = friendsOnGrid();
		Collections.sort(fog, new FriendRankComparator()); 
		return fog;
	}

	public class FriendRankComparator implements Comparator<Friend>{
		@Override
		public int compare(Friend lhs, Friend rhs) {
			return timeOfLastAction(lhs).compareTo(timeOfLastAction(rhs));
		}
	}

	public Long timeOfLastAction(Friend f){
		String stime = f.get(Friend.Attributes.TIME_OF_LAST_ACTION);
		if (stime == null || stime.equals(""))
			return 0l;
		return Long.valueOf(stime);
	}

	public Friend lowestRankedFriendOnGrid(){
		return rankedFriendsOnGrid().get(0);
	}

	public GridElement nextAvailableGridElement(){
		GridElement ge = GridElementFactory.getFactoryInstance().firstEmptyGridElement();
		if (ge != null)
			return ge;

		Friend f = lowestRankedFriendOnGrid();
		return GridElementFactory.getFactoryInstance().findWithFriendId(f.getId());
	}
	
	
    //----------------------------
	// VideoStatusChanged Observer
	//----------------------------
    @Override
    public void onVideoStatusChanged(Friend friend) {
        if (!isOnGrid(friend))
            moveFriendToGrid(friend);
    }

}

