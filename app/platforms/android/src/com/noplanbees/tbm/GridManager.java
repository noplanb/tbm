package com.noplanbees.tbm;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.FriendFactory;
import com.noplanbees.tbm.model.GridElement;
import com.noplanbees.tbm.model.GridElementFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class GridManager{
	private static final String STAG = GridManager.class.getSimpleName();
    private static GridManager gridManager;
	
    public static final int GRID_ELEMENTS_COUNT = 8;

	public static interface GridEventNotificationDelegate{
		void gridDidChange();
	}

    private Set<GridEventNotificationDelegate> gridEventNotificationDelegates;

    private GridManager() {
        gridEventNotificationDelegates  = new HashSet<GridEventNotificationDelegate>();
    }

    public static GridManager getInstance(){
        if(gridManager == null)
            gridManager = new GridManager();
        return gridManager;
    }

	public void addGridEventNotificationDelegate(GridEventNotificationDelegate delegate) {
        gridEventNotificationDelegates.add(delegate);
    }

	//----------
	// Init grid
	//----------
	public void initGrid(Context context){
		if (GridElementFactory.getFactoryInstance().all().size() == GRID_ELEMENTS_COUNT)
			return;

		GridElementFactory.getFactoryInstance().destroyAll(context);
		ArrayList<Friend> allFriends = FriendFactory.getFactoryInstance().all();
		for (Integer i = 0; i < GRID_ELEMENTS_COUNT; i++) {
			GridElement g = GridElementFactory.getFactoryInstance().makeInstance(context);
			if (i < allFriends.size()) {
				Friend f = allFriends.get(i);
				g.set(GridElement.Attributes.FRIEND_ID, f.getId());
			}
		}
	}

    public void removeGridEventNotificationDelegate(GridEventNotificationDelegate delegate){
        gridEventNotificationDelegates.remove(delegate);
    }

    private void notifyListeners(){
        for (GridEventNotificationDelegate gridEventNotificationDelegate : gridEventNotificationDelegates) {
            gridEventNotificationDelegate.gridDidChange();
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

    public void moveFriendToGrid(Context c, Friend f){
		rankingActionOccurred(f);
		if (!GridElementFactory.getFactoryInstance().friendIsOnGrid(f)){
			nextAvailableGridElement().setFriend(f);
			notifyListeners();
			highLightElementForFriend(c, f);
		}
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
	
	//----------------------
	// Highlight grid change
	//----------------------
	public void highLightElementForFriend(Context c, Friend f){
		final GridElement ge = GridElementFactory.getFactoryInstance().findWithFriend(f);
		if (ge == null)
			return;
		
		final View v = new View(c);
		LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		v.setLayoutParams(lp);
		v.setBackgroundColor(c.getResources().getColor(R.color.yellow));
		v.setAlpha(0f);
		
		ObjectAnimator a1 = ObjectAnimator.ofFloat(v, "Alpha", 0f, 1f);
		a1.setDuration(400);
		ObjectAnimator a2 = ObjectAnimator.ofFloat(v, "Alpha", 1f, 0f);
		a2.setDuration(400);
		a2.addListener(new AnimatorListener(){
			@Override
			public void onAnimationEnd(Animator animation) {
				Log.i(STAG, "onAnimationEnd");
				//ge.frame.removeView(v);
			}
			@Override
			public void onAnimationStart(Animator animation) {}
			@Override
			public void onAnimationCancel(Animator animation) {}
			@Override
			public void onAnimationRepeat(Animator animation) {}
		});
		
		AnimatorSet as = new AnimatorSet();
		as.play(a1).before(a2);
		//ge.frame.addView(v);
		as.start();
	}
}

