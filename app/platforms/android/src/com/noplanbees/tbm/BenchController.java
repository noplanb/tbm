package com.noplanbees.tbm;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;

import com.google.gson.internal.LinkedTreeMap;
import com.noplanbees.tbm.Friend.Attributes;

public class BenchController implements SmsStatsHandler.SmsManagerCallback, OnItemClickListener,
		ContactsManager.ContactSelected, SelectPhoneNumberDialog.SelectPhoneNumberDelegate {

	public static final float PERCENT_OF_WINDOW = 0.75f;
	public static final int ANIMATION_DURATION = 100;

	private final String TAG = getClass().getSimpleName();

	public interface Callbacks {
		void onHide();
	}

	private Activity activity;
	private ListView listView;
	private FriendFactory friendFactory;
	private SmsStatsHandler smsStatsHandler;
	private ArrayList<BenchObject> smsBenchObjects;
	private ArrayList<BenchObject> currentAllOnBench;
	private ContactsManager contactsManager;
	private Callbacks benchControllerCallbacks;

	// ----------------------
	// Constructor and setup
	// ----------------------
	public BenchController(Activity a) {
		activity = a;
		try {
			benchControllerCallbacks = (Callbacks) a;
		} catch (ClassCastException e) {
			Log.e(TAG, "Your activity must implemnets BenchController.Callbacks", e);
		}
		friendFactory = FriendFactory.getFactoryInstance();

		listView = (ListView) activity.findViewById(R.id.bench_list);
		listView.setOnItemClickListener(this);

		contactsManager = new ContactsManager(activity, this);
		smsStatsHandler = SmsStatsHandler.getInstance(activity, this);
	}

	public void onDataLoaded() {
		if (!smsStatsHandler.isWasCalledAsync())
			smsStatsHandler.getRankedPhoneData();
	}

	public void callSms() {
		smsStatsHandler.getRankedPhoneData();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		benchControllerCallbacks.onHide();

		BenchObject bo = currentAllOnBench.get(position);
		Log.i(TAG, "Postion:" + position + " " + bo.displayName);

		Friend friend = (Friend) friendFactory.find(bo.friendId);

		if (friend == null || !friend.hasApp()) {
			new InviteManager(activity, bo);
			return;
		}

		GridManager.moveFriendToGrid(activity, friend);
	}

	// ---------
	// Populate
	// ---------
	private void populate() {
		ArrayAdapter<String> ad = new ArrayAdapter<String>(activity, 
				R.layout.bench_list_item,
				R.id.text1,
				nameArray());
		listView.setAdapter(ad);
	}

	private ArrayList<BenchObject> allOnBench() {
		currentAllOnBench = benchFriendsAsBenchObjects();
		currentAllOnBench.addAll(dedupedSmsBenchObjects());
		return currentAllOnBench;
	}

	private String[] nameArray() {
		ArrayList<BenchObject> all = allOnBench();
		String[] na = new String[all.size()];
		for (int i = 0; i < all.size(); i++) {
			na[i] = all.get(i).displayName;
		}
		return na;
	}

	// --------------------------
	// Friend overflow from grid
	// --------------------------
	private ArrayList<BenchObject> benchFriendsAsBenchObjects() {
		ArrayList<BenchObject> r = new ArrayList<BenchObject>();
		for (Friend f : GridManager.friendsOnBench()) {
			LinkedTreeMap<String, String> e = new LinkedTreeMap<String, String>();
			e.put(BenchObject.Keys.FRIEND_ID, f.getId());
			e.put(BenchObject.Keys.MOBILE_NUMBER, f.get(Friend.Attributes.MOBILE_NUMBER));
			e.put(BenchObject.Keys.DISPLAY_NAME, f.fullName());
			e.put(BenchObject.Keys.FIRST_NAME, f.get(Friend.Attributes.FIRST_NAME));
			e.put(BenchObject.Keys.LAST_NAME, f.get(Friend.Attributes.LAST_NAME));
			r.add(new BenchObject(e));
		}
		return r;
	}

	// -------------
	// Sms Contacts
	// -------------
	@Override
	public void didRecieveRankedPhoneData(ArrayList<LinkedTreeMap<String, String>> rankedPhoneData) {
		smsBenchObjects = new ArrayList<BenchObject>();
		for (LinkedTreeMap<String, String> e : rankedPhoneData) {
			LinkedTreeMap<String, String> b = new LinkedTreeMap<String, String>();
			b.put(BenchObject.Keys.FIRST_NAME, e.get(SmsStatsHandler.Keys.FIRST_NAME));
			b.put(BenchObject.Keys.LAST_NAME, e.get(SmsStatsHandler.Keys.LAST_NAME));
			b.put(BenchObject.Keys.DISPLAY_NAME, e.get(SmsStatsHandler.Keys.DISPLAY_NAME));
			b.put(BenchObject.Keys.MOBILE_NUMBER, e.get(SmsStatsHandler.Keys.MOBILE_NUMBER));
			smsBenchObjects.add(new BenchObject(b));
		}

		populate();
		contactsManager.setupAutoComplete((AutoCompleteTextView) activity
				.findViewById(R.id.contacts_auto_complete_text_view));
	}

	private ArrayList<BenchObject> dedupedSmsBenchObjects() {
		ArrayList<BenchObject> r = new ArrayList<BenchObject>();
		if (smsBenchObjects == null)
			return r;

		for (BenchObject b : smsBenchObjects) {
			if (!isBenchObjectAFriend(b))
				r.add(b);
		}
		return r;
	}

	private boolean isBenchObjectAFriend(BenchObject bo) {
		for (Friend f : FriendFactory.getFactoryInstance().all()) {
			if (ContactsManager.isPhoneNumberMatch(f.get(Friend.Attributes.MOBILE_NUMBER), bo.mobileNumber))
				return true;
		}
		return false;
	}

	private Friend friendMatchingContact(Contact contact) {
		for (Friend f : friendFactory.all()) {
			for (LinkedTreeMap<String, String> pno : contact.phoneObjects) {
				if (ContactsManager.isPhoneNumberMatch(f.get(Attributes.MOBILE_NUMBER),
						pno.get(Contact.PhoneNumberKeys.E164))) {
					return f;
				}
			}
		}
		return null;
	}

	// ---------------------
	// Contact List Contact
	// ---------------------

	@Override
	public void contactSelected(Contact contact) {
		Log.i(TAG, contact.toString());

		benchControllerCallbacks.onHide();

		Friend f = friendMatchingContact(contact);
		if (f != null) {
			GridManager.moveFriendToGrid(activity, f);
			return;
		}

		if (contact.phoneObjects.size() == 0) {
			showNoValidPhonesDialog(contact);
			return;
		}

		if (contact.phoneObjects.size() == 1) {
			invite(contact, contact.phoneObjects.get(0));
			return;
		}

		new SelectPhoneNumberDialog(activity, contact, this);
	}

	@Override
	public void phoneSelected(Contact contact, int phoneIndex) {
		invite(contact, contact.phoneObjects.get(phoneIndex));
	}

	private void showNoValidPhonesDialog(Contact contact) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle("No Mobile Number")
				.setMessage(
						"I could not find a valid mobile number for " + contact.getDisplayName()
								+ ".\n\nPlease add a mobile number for " + contact.getFirstName()
								+ " in your device contacts and try again.")
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
					}
				}).create().show();
	}

	private void invite(Contact contact, LinkedTreeMap<String, String> mobileNumber) {
		BenchObject bo = benchObjectWithContact(contact, mobileNumber);
		new InviteManager(activity, bo);
	}

	private BenchObject benchObjectWithContact(Contact contact, LinkedTreeMap<String, String> mobileNumber) {
		LinkedTreeMap<String, String> boParams = new LinkedTreeMap<String, String>();
		boParams.put(BenchObject.Keys.DISPLAY_NAME, contact.getDisplayName());
		boParams.put(BenchObject.Keys.FIRST_NAME, contact.getFirstName());
		boParams.put(BenchObject.Keys.LAST_NAME, contact.getLastName());
		boParams.put(BenchObject.Keys.MOBILE_NUMBER, mobileNumber.get(Contact.PhoneNumberKeys.E164));
		return new BenchObject(boParams);
	}
}
