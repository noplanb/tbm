package com.noplanbees.tbm.bench;

import android.app.Activity;
import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
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
import com.noplanbees.tbm.GridManager;
import com.noplanbees.tbm.R;
import com.noplanbees.tbm.model.Contact;
import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.Friend.Attributes;
import com.noplanbees.tbm.model.FriendFactory;
import com.noplanbees.tbm.ui.dialogs.SelectPhoneNumberDialog;
import com.noplanbees.tbm.utilities.DialogShower;

import java.util.ArrayList;
import java.util.List;

public class BenchController implements SmsStatsHandler.SmsManagerCallback, OnItemClickListener,
        ContactsManager.ContactSelected, SelectPhoneNumberDialog.Callbacks, BenchViewManager {

	private final String TAG = getClass().getSimpleName();

	private Activity activity;
	private ListView listView;
  private DrawerLayout drawerLayout;
  private BenchAdapter adapter;
	private FriendFactory friendFactory;
	private SmsStatsHandler smsStatsHandler;
	private ArrayList<BenchObject> smsBenchObjects;
	private ArrayList<BenchObject> currentAllOnBench;
	private ContactsManager contactsManager;

	// ----------------------
	// Constructor and setup
	// ----------------------
	public BenchController(Activity a) {
		activity = a;
    drawerLayout = (DrawerLayout) activity.findViewById(R.id.drawer_layout);
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

        hideBench();
		BenchObject bo = currentAllOnBench.get(position);
		Log.i(TAG, "Postion:" + position + " " + bo.displayName);

		Friend friend = (Friend) friendFactory.find(bo.friendId);
		if (friend == null) {
            InviteManager.getInstance().invite(bo);
			return;
		}

		GridManager.getInstance().moveFriendToGrid(friend);
	}

    @Override
    public void showBench() {
        drawerLayout.openDrawer(Gravity.RIGHT);
    }

    @Override
    public void hideBench() {
        drawerLayout.closeDrawers();
    }

    @Override
    public void updateBench() {
        adapter.setList(allOnBench());
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean isBenchShowed() {
        return drawerLayout.isDrawerOpen(Gravity.RIGHT);
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

        hideBench();

		Friend f = friendMatchingContact(contact);
		if (f != null) {
			GridManager.getInstance().moveFriendToGrid(f);
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

        DialogShower.showSelectPhoneNumberDialog(activity, contact, this);
	}

    private void showNoValidPhonesDialog(Contact contact) {
        DialogShower.showInfoDialog(activity, activity.getString(R.string.dialog_no_valid_phones_title),
                activity.getString(R.string.dialog_no_valid_phones_message, contact.getDisplayName(), contact.getFirstName()));
    }

	private void invite(Contact contact, LinkedTreeMap<String, String> mobileNumber) {
		BenchObject bo = BenchObject.benchObjectWithContact(contact, mobileNumber);
        InviteManager.getInstance().invite(bo);
	}

    @Override
    public void phoneSelected(Contact contact, int phoneIndex) {
        InviteManager.getInstance().invite(BenchObject.benchObjectWithContact(contact, contact.phoneObjects.get(phoneIndex)));
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
                holder.thumb.setVisibility(View.VISIBLE);
			}else{
				holder.thumb.setVisibility(View.GONE);
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
