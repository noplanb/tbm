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
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.google.gson.internal.LinkedTreeMap;

public class BenchController implements SmsManager.SmsManagerCallback, OnItemClickListener, ContactsManager.ContactSelected{
	
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
	private ArrayList<BenchObject> smsBenchObjects;
	private ArrayList<BenchObject> currentAllOnBench;
	private ContactsManager contactsManager;

	//----------------------
	// Constructor and setup
	//----------------------
	public BenchController(Activity a){
		activity = a;
		friendFactory = FriendFactory.getFactoryInstance();
		smsManager = SmsManager.getInstance(activity, this);
		contactsManager = new ContactsManager(activity);
		contactsManager.setContactSelectedDelegate(this);
		
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
		BenchObject target = currentAllOnBench.get(position);
    	Log.i(TAG, "Postion:" + position + " " + target.displayName);
    	if (target.friendId != null){
    		Friend f = (Friend) FriendFactory.getFactoryInstance().find(target.friendId);
    		GridManager.moveFriendToGrid(f);
    	} else {
    		new InviteManager(activity, target);
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
	
	private ArrayList<BenchObject> allOnBench(){
		currentAllOnBench = benchFriendsAsBenchObjects();
		currentAllOnBench.addAll(dedupedSmsBenchObjects());
		return currentAllOnBench;
	}
	
	private String[] nameArray(){
		ArrayList<BenchObject> all = allOnBench();
		String[] na = new String[all.size()];
		for (int i=0; i<all.size(); i++){
			na[i] = all.get(i).displayName;
		}
		return na;
	}
	
	//--------------------------
	// Friend overflow from grid
	//--------------------------
	private ArrayList<BenchObject> benchFriendsAsBenchObjects(){
		ArrayList<BenchObject> r = new ArrayList<BenchObject>();
		for (Friend f : GridManager.friendsOnBench()){
			LinkedTreeMap<String, String> e = new LinkedTreeMap<String, String>();
			e.put(BenchObject.Keys.FRIEND_ID, f.getId());
			e.put(BenchObject.Keys.MOBILE_NUMBER, f.get(Friend.Attributes.MOBILE_NUMBER));
			e.put(BenchObject.Keys.DISPLAY_NAME, f.fullName());
			r.add(new BenchObject(e));
		}
		return r;
	}
	
	//-------------
	// Sms Contacts
	//-------------
	@Override
	public void didRecieveRankedPhoneData(ArrayList<LinkedTreeMap<String, String>> rankedPhoneData) {
		smsBenchObjects = new ArrayList<BenchObject>();
		for(LinkedTreeMap<String, String> e : rankedPhoneData){
			LinkedTreeMap<String, String> b = new LinkedTreeMap<String, String>();
			b.put(BenchObject.Keys.FIRST_NAME, e.get(SmsManager.Keys.FIRST_NAME));
			b.put(BenchObject.Keys.LAST_NAME, e.get(SmsManager.Keys.LAST_NAME));
			b.put(BenchObject.Keys.DISPLAY_NAME, e.get(SmsManager.Keys.DISPLAY_NAME));
			b.put(BenchObject.Keys.MOBILE_NUMBER, e.get(SmsManager.Keys.MOBILE_NUMBER));
			smsBenchObjects.add(new BenchObject(b));
		}
	}
	
	private ArrayList<BenchObject> dedupedSmsBenchObjects(){
		ArrayList<BenchObject> r = new ArrayList<BenchObject>();
		if (smsBenchObjects == null)
			return r;
		
		for (BenchObject b: smsBenchObjects){
			if (!isBenchObjectAFriend(b))
				r.add(b);
		}
		return r;
	}
	
	private boolean isBenchObjectAFriend(BenchObject bo) {
		for (Friend f : FriendFactory.getFactoryInstance().all()){
			if (Convenience.mobileNumbersMatch(f.get(Friend.Attributes.MOBILE_NUMBER), bo.mobileNumber))
				return true;
		}
		return false;
	}
	
	
	//---------------------
	// Contact List Contact
	//---------------------
	
	@Override
	public void contactSelected(LinkedTreeMap<String, String> contact) {	
		Log.i(TAG, contact.toString());
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
		contactsManager.setupAutoComplete((AutoCompleteTextView) activity.findViewById(R.id.contacts_auto_complete_text_view));
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
