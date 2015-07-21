package com.zazoapp.client.bench;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import butterknife.ButterKnife;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.R;
import com.zazoapp.client.model.Contact;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.GridManager;
import com.zazoapp.client.ui.ZazoManagerProvider;
import com.zazoapp.client.ui.helpers.ContactsManager;
import com.zazoapp.client.utilities.AsyncTaskManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BenchController implements BenchDataHandler.BenchDataHandlerCallback, OnItemClickListener,
        ContactsManager.ContactSelected, BenchViewManager, DrawerLayout.DrawerListener {

    private static final String TAG = BenchController.class.getSimpleName();
    private final AbsListView.OnScrollListener mScrollListener;

    private Activity activity;
    private ZazoManagerProvider managerProvider;
	private ListView listView;
    private DrawerLayout drawerLayout;
    private BenchAdapter adapter;
	private FriendFactory friendFactory;
	private BenchDataHandler benchDataHandler;
	private ArrayList<BenchObject> smsBenchObjects;
	private ArrayList<BenchObject> contactBenchObjects;
	private ArrayList<BenchObject> currentAllOnBench;
	private ContactsManager contactsManager;
    private final View slidingHeading;
    private final TextView slidingTitle;

    // ----------------------
	// Constructor and setup
	// ----------------------
	public BenchController(Activity a, ZazoManagerProvider mp) {
		activity = a;
        managerProvider = mp;
        drawerLayout = ButterKnife.findById(activity, R.id.drawer_layout);
        drawerLayout.setDrawerListener(this);
        adapter = new BenchAdapter(activity);
        slidingHeading = ButterKnife.findById(activity, R.id.contacts_heading);
        slidingTitle = ButterKnife.findById(slidingHeading, R.id.title);
        listView = ButterKnife.findById(activity, R.id.bench_list);
        listView.setOnItemClickListener(this);
        mScrollListener = new BenchScrollListener();
        listView.setOnScrollListener(mScrollListener);

		contactsManager = new ContactsManager(activity, this);
		benchDataHandler = new BenchDataHandler(activity);
        benchDataHandler.setListener(this);
	}

	public void loadContacts() {
        friendFactory = FriendFactory.getFactoryInstance();
        listView.setSelection(0); // to scroll list to the first position
		benchDataHandler.getRankedPhoneData();
	}

	public void callSms() {
		benchDataHandler.getRankedPhoneData();
	}

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        hideBench();
        BenchObject bo = currentAllOnBench.get(position);
        Log.i(TAG, "Position:" + position + " " + bo.displayName);

        // if it is just a friend on bench, move it to grid
        Friend friend = friendFactory.find(bo.friendId);
        if (friend != null) {
            GridManager.getInstance().moveFriendToGrid(friend);
            return;
        }

        // if it is an sms contact with fixed number: invite directly, otherwise invite as a simple contact
        if (bo.hasFixedContact()) {
            managerProvider.getInviteHelper().invite(bo);
        } else {
            Contact contact = contactsManager.contactWithId(bo.contactId, bo.displayName);
            managerProvider.getInviteHelper().invite(contact);
        }
    }

    // ---------------------
    // Contact List Contact
    // ---------------------
    @Override
    public void contactSelected(Contact contact) {
        Log.i(TAG, contact.toString());

        hideBench();
        managerProvider.getInviteHelper().invite(contact);
    }

    @Override
    public void showBench() {
        drawerLayout.openDrawer(Gravity.RIGHT);
    }

    @Override
    public void hideBench() {
        drawerLayout.closeDrawers();
        //clear contacts_auto_complete_text_view because after resume, "old filtering" word appear
        ((AutoCompleteTextView) activity.findViewById(R.id.contacts_auto_complete_text_view)).setText("");
    }

    @Override
    public void updateBench() {
        AsyncTaskManager.executeAsyncTask(false, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                adapter.setList(allOnBench());
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public boolean isBenchShowed() {
        return drawerLayout.isDrawerOpen(Gravity.RIGHT);
    }

	// ---------
	// Populate
	// ---------
	private void populate(final ArrayList<LinkedTreeMap<String, String>> phoneData) {
        AsyncTaskManager.executeAsyncTask(false, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                smsBenchObjects = new ArrayList<>();
                contactBenchObjects = new ArrayList<>();
                for (LinkedTreeMap<String, String> e : phoneData) {
                    LinkedTreeMap<String, String> b = new LinkedTreeMap<String, String>();
                    b.put(BenchObject.Keys.FIRST_NAME, e.get(BenchDataHandler.Keys.FIRST_NAME));
                    b.put(BenchObject.Keys.LAST_NAME, e.get(BenchDataHandler.Keys.LAST_NAME));
                    b.put(BenchObject.Keys.DISPLAY_NAME, e.get(BenchDataHandler.Keys.DISPLAY_NAME));
                    b.put(BenchObject.Keys.MOBILE_NUMBER, e.get(BenchDataHandler.Keys.MOBILE_NUMBER));
                    b.put(BenchObject.Keys.CONTACT_ID, e.get(BenchDataHandler.Keys.CONTACT_ID));
                    BenchObject bo = new BenchObject(b);
                    if (bo.hasFixedContact()) {
                        smsBenchObjects.add(bo);
                    } else {
                        contactBenchObjects.add(bo);
                    }
                }
                adapter.setList(allOnBench());
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (listView.getAdapter() == adapter) {
                    adapter.notifyDataSetChanged();
                } else {
                    listView.setAdapter(adapter);
                }
                listView.setVisibility(View.VISIBLE);
            }
        });
	}

    private List<BenchObject>[] allOnBench() {
        List<BenchObject>[] allLists = new List[3];
        allLists[0] = benchFriendsAsBenchObjects();
        allLists[1] = dedupedSmsBenchObjects();
        allLists[2] = allContacts();
        currentAllOnBench = new ArrayList<>();
        for (List<BenchObject> list : allLists) {
            currentAllOnBench.addAll(list);
        }
        return allLists;
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
			e.put(BenchObject.Keys.DISPLAY_NAME, f.getFullName());
			e.put(BenchObject.Keys.FIRST_NAME, f.get(Friend.Attributes.FIRST_NAME));
			e.put(BenchObject.Keys.LAST_NAME, f.get(Friend.Attributes.LAST_NAME));
			r.add(new BenchObject(e));
		}
        sortBench(r);
        return r;
	}

    private List<BenchObject> allContacts() {
        return (contactBenchObjects != null) ? contactBenchObjects : new ArrayList<BenchObject>();
    }

	// -------------
	// Sms Contacts
	// -------------
	@Override
	public void receivePhoneData(ArrayList<LinkedTreeMap<String, String>> phoneData) {
		populate(phoneData);
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

    private void sortBench(List<BenchObject> list) {
        Collections.sort(list, new Comparator<BenchObject>() {
            @Override
            public int compare(BenchObject lhs, BenchObject rhs) {
                return lhs.displayName.compareTo(rhs.displayName);
            }
        });
    }

	private boolean isBenchObjectAFriend(BenchObject bo) {
		for (Friend f : FriendFactory.getFactoryInstance().all()) {
			if (ContactsManager.isPhoneNumberMatch(f.get(Friend.Attributes.MOBILE_NUMBER), bo.mobileNumber))
				return true;
		}
		return false;
	}

    @Override
    public void onDrawerSlide(View view, float v) {
    }

    @Override
    public void onDrawerOpened(View view) {
    }

    @Override
    public void onDrawerClosed(View view) {
        if (contactsManager != null) {
            contactsManager.hideKeyboard();
        }
    }

    @Override
    public void onDrawerStateChanged(int i) {
    }

    private class BenchAdapter extends BaseAdapter{

        private Context context;
        private List<BenchObject>[] lists;

        public BenchAdapter(Context context) {
            this.context = context;
            this.lists = null;
        }

        public void setList(List<BenchObject>[] lists) {
            this.lists = lists;
        }

        public boolean isFirstPositionForType(int position, int type) {
            BenchObject o = getItem(position);
            if (lists[type].size() > 0 && lists[type].get(0).equals(o)) {
                return true;
            }
            return false;
        }

        public boolean isLastPositionForType(int position, int type) {
            BenchObject o = getItem(position);
            if (lists[type].size() > 0 && lists[type].get(lists[type].size() - 1).equals(o)) {
                return true;
            }
            return false;
        }

        @Override
        public int getViewTypeCount() {
            return lists.length;
        }

        @Override
        public int getItemViewType(int position) {
            int offset = 0;
            for (int i = 0; i < getViewTypeCount(); i++) {
                if (offset + lists[i].size() > position) {
                    return i;
                }
                offset += lists[i].size();
            }
            throw new IndexOutOfBoundsException("Wrong position: " + position + " " + getCount());  // never get here
        }

        @Override
        public int getCount() {
            int count = 0;
            for (List<BenchObject> list : lists) {
                count += list.size();
            }
            return count;
        }

        @Override
        public BenchObject getItem(int position) {
            int correctedPos = position;
            for (List<BenchObject> list : lists) {
                if (list.size() > correctedPos) {
                    return list.get(correctedPos);
                } else {
                    correctedPos -= list.size();
                }
            }
            throw new IndexOutOfBoundsException("Wrong position: " + position + " " + getCount()); // never get here
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            View v;
            if(convertView == null){
                v = LayoutInflater.from(context).inflate(R.layout.bench_list_item, parent, false);
                holder = new ViewHolder();
                holder.name = (TextView) v.findViewById(R.id.name);
                holder.thumb = (ImageView) v.findViewById(R.id.thumb);
                holder.thumbBorder = (ImageView) v.findViewById(R.id.borderImage);
                holder.header = v.findViewById(R.id.header);
                holder.headerTitle = (TextView) v.findViewById(R.id.title);
                v.setTag(holder);
            }else{
                v = convertView;
                holder = (ViewHolder) v.getTag();
            }

            BenchObject item = getItem(position);

            Friend friend = FriendFactory.getFactoryInstance().find(item.friendId);
            if (friend!=null){
                if(friend.thumbExists()) {
                    holder.thumb.setImageBitmap(friend.thumbBitmap());
                    holder.thumbBorder.setVisibility(View.VISIBLE);
                }else {
                    holder.thumb.setImageResource(R.drawable.ic_no_pic_z);
                    holder.thumbBorder.setVisibility(View.INVISIBLE);
                }
                holder.thumb.setVisibility(View.VISIBLE);
            }else{
                holder.thumb.setVisibility(View.GONE);
                holder.thumbBorder.setVisibility(View.GONE);
            }
            if (isFirstPositionForType(position, 1)) {
                holder.header.setVisibility(View.VISIBLE);
                holder.headerTitle.setText(R.string.bench_heading_sms);
            } else if (isFirstPositionForType(position, 2)) {
                holder.header.setVisibility(View.VISIBLE);
                holder.headerTitle.setText(R.string.bench_heading_all);
            } else {
                holder.header.setVisibility(View.INVISIBLE);
            }
            holder.name.setText(item.displayName);

            return v;
        }

        private class ViewHolder{
            ImageView thumb;
            ImageView thumbBorder;
            TextView name;
            View header;
            TextView headerTitle;
        }
    }

    private class BenchScrollListener implements AbsListView.OnScrollListener {

        BenchScrollListener() {
            slidingHeading.setTag(0);
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (visibleItemCount > 0) {
                int type = adapter.getItemViewType(firstVisibleItem);
                View item = view.getChildAt(0);
                boolean firstForSms = adapter.isFirstPositionForType(firstVisibleItem, 1);
                boolean lastForSms = adapter.isLastPositionForType(firstVisibleItem, 1);
                boolean firstForAll = adapter.isFirstPositionForType(firstVisibleItem, 2);
                if (firstForSms && item.getTop() >= 0 || firstForAll && item.getTop() >= 0) {
                    type = 0;
                }
                if ((int) slidingHeading.getTag() != type) {
                    slidingHeading.setTag(type);
                    switch (type) {
                        case 1:
                            slidingTitle.setText(R.string.bench_heading_sms);
                            slidingHeading.setVisibility(View.VISIBLE);
                            break;
                        case 2:
                            slidingTitle.setText(R.string.bench_heading_all);
                            slidingHeading.setVisibility(View.VISIBLE);
                            break;
                        default:
                            slidingHeading.setVisibility(View.INVISIBLE);
                            break;
                    }
                }
                if (lastForSms && item.getBottom() < slidingHeading.getHeight()) {
                    slidingHeading.setTranslationY(item.getBottom() - slidingHeading.getHeight());
                } else {
                    slidingHeading.setTranslationY(0);
                }
            }
        }
    }
}
