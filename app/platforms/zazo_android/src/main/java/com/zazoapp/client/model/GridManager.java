package com.zazoapp.client.model;

import android.content.Context;
import com.zazoapp.client.core.PreferencesHelper;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.ui.GridViewFragment;
import com.zazoapp.client.ui.view.NineViewGroup;

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

    public static void onFriendDeleteStatusChanged(Friend friend) {
        if (friend.isDeleted()) {
            if (GridElementFactory.getFactoryInstance().friendIsOnGrid(friend)) {
                getInstance().moveNextFriendTo(GridElementFactory.getFactoryInstance().findWithFriendId(friend.getId()));
            } else {
                TbmApplication.getInstance().getManagerProvider().getBenchViewManager().updateBench();
            }
        } else {
            getInstance().moveFriendToGrid(friend);
        }
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

    /**
     * Lowest ranked friend will be replaced with new friend and moved to specified box
     * The friend from the specified box will be moved to the box where lowest ranked friend was
     *
     * Works only if there are no more free grid elements. Otherwise just calls {@link #moveFriendToGrid(Friend)}
     *
     * @param f friend
     * @param box specified box
     */
    public void moveFriendToSpecificBox(Friend f, NineViewGroup.Box box) {
        if (GridElementFactory.getFactoryInstance().firstEmptyGridElement() != null) {
            moveFriendToGrid(f);
            return;
        }
        if (f == null)
            return;
        f.setLastActionTime();
        ArrayList<GridElement> allElements = GridElementFactory.getFactoryInstance().all();
        int spinOffset = new PreferencesHelper(f.getContext()).getInt(GridViewFragment.PREF_SPIN_OFFSET, 0);
        int elementFromBoxIndex = box.getWithOffset(-spinOffset).getPos();
        if (!GridElementFactory.getFactoryInstance().friendIsOnGrid(f)) {
            Friend lowestRankedFriendOnGrid = lowestRankedFriendOnGrid();
            GridElement availableElement = GridElementFactory.getFactoryInstance().findWithFriendId(lowestRankedFriendOnGrid.getId());
            if (availableElement != null) {
                int availableIndex = allElements.indexOf(availableElement);
                if (availableIndex == elementFromBoxIndex) {
                    availableElement.setFriend(f);
                } else {
                    GridElement elementFromBox = allElements.get(elementFromBoxIndex);
                    availableElement.setFriend(elementFromBox.getFriend());
                    elementFromBox.setFriend(f);
                }
            }
        } else {
            GridElement ge = GridElementFactory.getFactoryInstance().findWithFriendId(f.getId());
            int availableIndex = allElements.indexOf(ge);
            if (ge != null) {
                if (availableIndex == elementFromBoxIndex) {
                    ge.notifyUpdate();
                } else {
                    GridElement elementFromBox = allElements.get(elementFromBoxIndex);
                    ge.setFriend(elementFromBox.getFriend());
                    elementFromBox.setFriend(f);
                }
            }
        }
        updateAllEmpty();
    }

    /**
     * Fills specified grid element with friend from bench if available
     * @param ge
     */
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
            if (friend.incomingMessagesNotViewed()) {
                emptySpaces++;
            }
        }
        if (emptySpaces == 0) {
            return;
        }
        ArrayList<Friend> friendsWithUnviewedOnBench = new ArrayList<>();
        for (Friend friend : FriendFactory.getFactoryInstance().allEnabled()) {
            if (friend.incomingMessagesNotViewed() && !fog.contains(friend)) {
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
            boolean lhsHasNotViewed = lhs.incomingMessagesNotViewed();
            boolean rhsHasNotViewed = rhs.incomingMessagesNotViewed();
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

