package com.noplanbees.tbm;

import java.util.ArrayList;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Point;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.google.gson.internal.LinkedTreeMap;

public class BenchController implements SmsManager.SmsManagerCallback, OnItemClickListener{
	
	public static final float PERCENT_OF_WINDOW = 0.75f;
	public static final int ANIMATION_DURATION = 100;
	
	private final String TAG = getClass().getSimpleName();
	
	private Activity activity;
	private RelativeLayout benchLayout;
	private boolean isShowing;
	private ObjectAnimator anim;
	private ListView listView;
	private FriendFactory friendFactory;
	private SmsManager smsManager;
	private ArrayList<LinkedTreeMap<String, String>> smsContacts;
	private ArrayList<LinkedTreeMap<String, String>> currentAllOnBench;

	//----------------------
	// Constructor and setup
	//----------------------
	public BenchController(Activity a){
		activity = a;
		friendFactory = FriendFactory.getFactoryInstance();
		smsManager = SmsManager.getInstance(activity, this);
		
		setupFrame();
		setupAnimator();
		setupListView();
	}

	private void setupFrame() {
		benchLayout = (RelativeLayout) activity.findViewById(R.id.bench_frame);
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(benchWidth(), windowSize().y);
		benchLayout.setLayoutParams(lp);
		benchLayout.setX(hiddenX());
		isShowing = false;
	}
	
	private void setupAnimator(){
		anim = ObjectAnimator.ofFloat(benchLayout, "X", (float)hiddenX(), (float)shownX());
		anim.setDuration(BenchController.ANIMATION_DURATION);
	}

	private void setupListView() {
		listView = (ListView) activity.findViewById(R.id.bench_list);
		listView.setOnItemClickListener(this);		
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		LinkedTreeMap<String, String> target = currentAllOnBench.get(position);
    	Log.i(TAG, "Postion:" + position + " " + target.get(SmsManager.Keys.DISPLAY_NAME));
    	String fid = target.get(Friend.Attributes.ID);
    	if (fid != null){
    		Friend f = (Friend) FriendFactory.getFactoryInstance().find(fid);
    		GridManager.moveFriendToGrid(f);
    	} else {
    		
    	}
    	hide();
	}
	
	//---------
	// Populate
	//---------
	private void populate(){
		ArrayAdapter<String> ad = new ArrayAdapter<String>(activity, R.layout.bench_list_item, nameArray()); 
		listView.setAdapter(ad);
	}
	
	private ArrayList<LinkedTreeMap<String, String>> allOnBench(){
		currentAllOnBench = benchFriendsAsBenchObjects();
		currentAllOnBench.addAll(dedupedSmsFriends());
		return currentAllOnBench;
	}
	
	private String[] nameArray(){
		ArrayList<LinkedTreeMap<String, String>> all = allOnBench();
		String[] na = new String[all.size()];
		for (int i=0; i<all.size(); i++){
			na[i] = all.get(i).get(SmsManager.Keys.DISPLAY_NAME);
		}
		return na;
	}
	
	//--------------------------
	// Friend overflow from grid
	//--------------------------
	private ArrayList<LinkedTreeMap<String, String>> benchFriendsAsBenchObjects(){
		ArrayList<LinkedTreeMap<String, String>> r = new ArrayList<LinkedTreeMap<String, String>>();
		for (Friend f : GridManager.friendsOnBench()){
			LinkedTreeMap<String, String> e = new LinkedTreeMap<String, String>();
			e.put(Friend.Attributes.ID, f.getId());
			e.put(Friend.Attributes.MOBILE_NUMBER, f.get(Friend.Attributes.MOBILE_NUMBER));
			e.put(SmsManager.Keys.DISPLAY_NAME, f.fullName());
			r.add(e);
		}
		return r;
	}
	
	//-------------
	// Sms Contacts
	//-------------
	@Override
	public void didRecieveRankedPhoneData(ArrayList<LinkedTreeMap<String, String>> rankedPhoneData) {
		smsContacts = rankedPhoneData;
	}
	
	private ArrayList<LinkedTreeMap<String, String>> dedupedSmsFriends(){
		ArrayList<LinkedTreeMap<String, String>> r = new ArrayList<LinkedTreeMap<String, String>>();
		if (smsContacts == null)
			return r;
		
		for (LinkedTreeMap<String, String> e: smsContacts){
			if (!smsFriendIsBenchFriend(e))
				r.add(e);
		}
		return r;
	}
	
	private boolean smsFriendIsBenchFriend(LinkedTreeMap<String, String> smsFriend) {
		for (Friend f : FriendFactory.getFactoryInstance().all()){
			if (Convenience.mobileNumbersMatch(f.get(Friend.Attributes.MOBILE_NUMBER), smsFriend.get(SmsManager.Keys.MOBILE_NUMBER)))
				return true;
		}
		return false;
	}
	

	//--------------
	// Show and hide
	//--------------
	public void toggle() {
		if (isShowing)
			hide();
		else
			show();
	}

	public void show(){
		if (isShowing)
			return;
		populate();
		anim.setFloatValues((float)hiddenX(), (float)shownX());
		anim.start();
		isShowing = true;
	}

	public void hide(){
		if (!isShowing)
			return;

		anim.setFloatValues((float)shownX(), (float)hiddenX());
		anim.start();	
		isShowing = false;
	}



	//------------------
	// Size calculations
	//------------------
	private int benchWidth(){
		return (int) (BenchController.PERCENT_OF_WINDOW * windowSize().x);
	}

	private int shownX(){
		return windowSize().x - benchWidth();
	}

	private int hiddenX(){
		return windowSize().x;
	}

	private Point windowSize(){
		Point p = new Point();
		activity.getWindowManager().getDefaultDisplay().getSize(p);
		return p;
	}










}
