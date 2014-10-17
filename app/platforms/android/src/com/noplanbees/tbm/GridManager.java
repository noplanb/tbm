package com.noplanbees.tbm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class GridManager {
	public static interface GridEventNotificationDelegate{
		public void gridDidChange();
	}
	
	private static GridEventNotificationDelegate gridEventDelegate;
	public static void setGridEventNotificationDelegate(GridEventNotificationDelegate delegate){
		gridEventDelegate = delegate;
	}
	
	public static ArrayList<Friend> friendsOnGrid(){
		ArrayList<Friend> r = new ArrayList<Friend>();
		for (GridElement ge : GridElementFactory.getFactoryInstance().all()){
			if (ge.hasFriend())
				r.add(ge.friend());
		}
		return r;
	}
	
	public static ArrayList<Friend> friendsOnBench(){
		ArrayList<Friend> allFriends = FriendFactory.getFactoryInstance().all();
		ArrayList<Friend> gridFriends = friendsOnGrid();
		for (Friend gf : gridFriends){
			allFriends.remove(gf);
		}
		return allFriends;
	}
	
	public static void moveFriendToGrid(Friend f){
		rankingActionOccurred(f);
		if (!GridElementFactory.getFactoryInstance().friendIsOnGrid(f)){
			nextAvailableGridElement().setFriend(f);
			if (gridEventDelegate != null)
				gridEventDelegate.gridDidChange();
		}
	}
	
	//--------
    // Ranking
	//--------
	public static void rankingActionOccurred(Friend f){
		f.set(Friend.Attributes.TIME_OF_LAST_ACTION, System.currentTimeMillis() + "");
	}
	
	public static ArrayList<Friend> rankedFriendsOnGrid(){
		ArrayList<Friend> fog = friendsOnGrid();
		Collections.sort(fog, new FriendRankComparator()); 
		return fog;
	}
	
	public static class FriendRankComparator implements Comparator<Friend>{
		@Override
		public int compare(Friend lhs, Friend rhs) {
			return timeOfLastAction(lhs).compareTo(timeOfLastAction(rhs));
		}
	}
	
	public static Long timeOfLastAction(Friend f){
		String stime = f.get(Friend.Attributes.TIME_OF_LAST_ACTION);
		if (stime == null || stime.equals(""))
			return 0L;
		return Long.parseLong(stime);
	}
	
	public static Friend lowestRankedFriendOnGrid(){
		return rankedFriendsOnGrid().get(0);
	}
	
	public static GridElement nextAvailableGridElement(){
		GridElement ge = GridElementFactory.getFactoryInstance().firstEmptyGridElement();
		if (ge != null)
			return ge;
		
		Friend f = lowestRankedFriendOnGrid();
		return GridElementFactory.getFactoryInstance().findWithFriendId(f.getId());
	}
	
	
}

