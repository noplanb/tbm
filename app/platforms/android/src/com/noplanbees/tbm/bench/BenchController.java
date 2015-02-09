package com.noplanbees.tbm.bench;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.internal.LinkedTreeMap;
import com.noplanbees.tbm.ContactsManager;
import com.noplanbees.tbm.R;
import com.noplanbees.tbm.GridManager;
import com.noplanbees.tbm.model.Contact;
import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.Friend.Attributes;
import com.noplanbees.tbm.dispatch.Dispatch;
import com.noplanbees.tbm.model.FriendFactory;
import com.noplanbees.tbm.ui.dialogs.SelectPhoneNumberDialog;

import java.util.ArrayList;
import java.util.List;

public class BenchController implements SmsStatsHandler.SmsManagerCallback, OnItemClickListener,
        ContactsManager.ContactSelected, SelectPhoneNumberDialog.SelectPhoneNumberDelegate {

	private final String TAG = getClass().getSimpleName();

    public interface Callbacks {
		void onHide();
		void showNoValidPhonesDialog(Contact contact);
		void inviteFriend(BenchObject bo);
	}

	private Activity activity;
	private ListView listView;
    private BenchAdapter adapter;
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
			Dispatch.dispatch("Your activity must implemnets BenchController.Callbacks");
		}

        adapter = new BenchAdapter(activity, null);

        listView = (ListView) activity.findViewById(R.id.bench_list);
		listView.setOnItemClickListener(this);

		contactsManager = new ContactsManager(activity, this);
		smsStatsHandler = SmsStatsHandler.getInstance(activity, this);
	}

	public void onDataLoaded() {
        friendFactory = FriendFactory.getFactoryInstance();
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
		if (friend == null) {
			benchControllerCallbacks.inviteFriend(bo);
			return;
		}

		GridManager.getInstance().moveFriendToGrid(activity, friend);
	}

	// ---------
	// Populate
	// ---------
	private void populate() {
        adapter.setList(allOnBench());
        listView.setAdapter(adapter);
	}

	private ArrayList<BenchObject> allOnBench() {
		currentAllOnBench = benchFriendsAsBenchObjects();
		currentAllOnBench.addAll(dedupedSmsBenchObjects());
		return currentAllOnBench;
	}

	// --------------------------
	// Friend overflow from grid
	// --------------------------
	private ArrayList<BenchObject> benchFriendsAsBenchObjects() {
		ArrayList<BenchObject> r = new ArrayList<BenchObject>();
		for (Friend f : GridManager.getInstance().friendsOnBench()) {
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
			GridManager.getInstance().moveFriendToGrid(activity, f);
			return;
		}

		if (contact.phoneObjects.size() == 0) {
			benchControllerCallbacks.showNoValidPhonesDialog(contact);
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

	private void invite(Contact contact, LinkedTreeMap<String, String> mobileNumber) {
		BenchObject bo = benchObjectWithContact(contact, mobileNumber);
		benchControllerCallbacks.inviteFriend(bo);
	}

	private BenchObject benchObjectWithContact(Contact contact, LinkedTreeMap<String, String> mobileNumber) {
		LinkedTreeMap<String, String> boParams = new LinkedTreeMap<String, String>();
		boParams.put(BenchObject.Keys.DISPLAY_NAME, contact.getDisplayName());
		boParams.put(BenchObject.Keys.FIRST_NAME, contact.getFirstName());
		boParams.put(BenchObject.Keys.LAST_NAME, contact.getLastName());
		boParams.put(BenchObject.Keys.MOBILE_NUMBER, mobileNumber.get(Contact.PhoneNumberKeys.E164));
		return new BenchObject(boParams);
	}

    public void onBenchHasChanged(){
        adapter.setList(allOnBench());
        adapter.notifyDataSetChanged();
    }
	
	private class BenchAdapter extends BaseAdapter{

		private Context context;
		private List<BenchObject> list;

		public BenchAdapter(Context context, List<BenchObject> list) {
			this.context = context;
			this.list = list;
		}

        public void setList(List<BenchObject> list) {
            this.list = list;
        }

        @Override
		public int getCount() {
			return list.size();
		}

		@Override
		public BenchObject getItem(int position) {
			return list.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			View v = null; 
			if(convertView == null){
				v = LayoutInflater.from(context).inflate(R.layout.bench_list_item, parent, false);
				holder = new ViewHolder();
				holder.name = (TextView) v.findViewById(R.id.name); 
				holder.thumb = (ImageView) v.findViewById(R.id.thumb);
				v.setTag(holder);
			}else{
				v = convertView;
				holder = (ViewHolder) v.getTag();
			}

			BenchObject item = list.get(position);

			Friend friend = (Friend) FriendFactory.getFactoryInstance().find(item.friendId);
			if (friend!=null){
                if(friend.thumbExists())
             	    holder.thumb.setImageBitmap(friend.lastThumbBitmap());
                else
                    holder.thumb.setImageResource(R.drawable.ic_no_pic_z);
			}else{
				holder.thumb.setImageResource(R.drawable.ic_no_pic_z);
			}
			
			holder.name.setText(item.displayName);
			
			return v;
		}
		
		private class ViewHolder{
			ImageView thumb;
			TextView name;
		}
		
	}
}
