package com.noplanbees.tbm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

public class GridManager{
	private static final String STAG = GridManager.class.getSimpleName();
	
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
	
	public static void moveFriendToGrid(Context c, Friend f){
		rankingActionOccurred(f);
		if (!GridElementFactory.getFactoryInstance().friendIsOnGrid(f)){
			nextAvailableGridElement().setFriend(f);
			if (gridEventDelegate != null)
				gridEventDelegate.gridDidChange();
			GridManager.highLightElementForFriend(c, f);
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
	
	//----------------------
	// Highlight grid change
	//----------------------
	public static void highLightElementForFriend(Context c, Friend f){
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

