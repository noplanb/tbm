package com.zazoapp.client.model;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

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
                g.setFriend(f, false);
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
            if (ge.hasFriend() && f != null && f.equals(ge.getFriend())) {
                isOnGrid = true;
                break;
            }
        }
        return isOnGrid;
    }

    public ArrayList<Friend> friendsOnBench() {
        ArrayList<Friend> allFriends = FriendFactory.getFactoryInstance().all();
        ArrayList<Friend> gridFriends = friendsOnGrid();
        for (Friend gf : gridFriends) {
            allFriends.remove(gf);
        }
        Iterator<Friend> it = allFriends.iterator();
        while (it.hasNext()) {
            if (it.next().isDeleted()) {
                it.remove();
            }
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

    public void moveFriendToGrid(Friend f) {
        if (f == null)
            return;
        f.setLastActionTime();
        if (!GridElementFactory.getFactoryInstance().friendIsOnGrid(f)) {
            nextAvailableGridElement().setFriend(f);
        } else {
            GridElement ge = GridElementFactory.getFactoryInstance().findWithFriendId(f.getId());
            if (ge != null) {
                ge.notifyUpdate();
            }
        }
        updateAllEmpty();
    }

    public void moveNextFriendTo(GridElement ge) {
        ArrayList<Friend> list = friendsOnBench();
        Friend newFriend = (list.size() > 0) ? list.get(0): null;
        if (newFriend != null) {
            newFriend.setLastActionTime();
        }
        ge.setFriend(newFriend);
        updateAllEmpty();
    }

    public void updateAllEmpty() {
        for (GridElement ge : GridElementFactory.getFactoryInstance().all()) {
            if (!ge.hasFriend()) {
                ge.forceUpdate();
            }
        }
    }

    //--------
    // Ranking
    //--------
    public ArrayList<Friend> rankedFriendsOnGrid(){
		ArrayList<Friend> fog = friendsOnGrid();
		Collections.sort(fog, new FriendRankComparator()); 
		return fog;
	}

    public void moveFriendsWithUnviewedOnGrid() {
        ArrayList<Friend> fog = friendsOnGrid();
        int emptySpaces = 0;
        for (Friend friend : fog) {
            if (friend.incomingVideoNotViewed()) {
                emptySpaces++;
            }
        }
        if (emptySpaces == 0) {
            return;
        }
        ArrayList<Friend> friendsWithUnviewedOnBench = new ArrayList<>();
        for (Friend friend : FriendFactory.getFactoryInstance().all()) {
            if (friend.incomingVideoNotViewed() && !fog.contains(friend)) {
                friendsWithUnviewedOnBench.add(friend);
            }
        }
        Collections.sort(friendsWithUnviewedOnBench, new FriendRankComparator());
        for (int i = emptySpaces; i > 0 && friendsWithUnviewedOnBench.size() > emptySpaces - i; i--) {
            moveFriendToGrid(friendsWithUnviewedOnBench.get(emptySpaces - i));
        }
    }

    public class FriendRankComparator implements Comparator<Friend>{
		@Override
		public int compare(Friend lhs, Friend rhs) {
            boolean lhsHasNotViewed = lhs.incomingVideoNotViewed();
            boolean rhsHasNotViewed = rhs.incomingVideoNotViewed();
            if (lhsHasNotViewed && !rhsHasNotViewed) {
                return 1;
            } else if (!lhsHasNotViewed && rhsHasNotViewed) {
                return -1;
            }
			return timeOfLastAction(lhs).compareTo(timeOfLastAction(rhs));
		}
	}

	public Long timeOfLastAction(Friend f){
		String stime = f.get(Friend.Attributes.TIME_OF_LAST_ACTION);
		if (stime == null || stime.equals(""))
			return 0L;
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

