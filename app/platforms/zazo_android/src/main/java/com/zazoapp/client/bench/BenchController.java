package com.zazoapp.client.bench;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.R;
import com.zazoapp.client.model.Contact;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.GridManager;
import com.zazoapp.client.ui.ZazoManagerProvider;
import com.zazoapp.client.ui.helpers.ContactsManager;
import com.zazoapp.client.ui.view.ClearableAutoCompleteTextView;
import com.zazoapp.client.utilities.AsyncTaskManager;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BenchController implements BenchDataHandler.BenchDataHandlerCallback, OnItemClickListener,
        ContactsManager.ContactSelected, BenchViewManager {

    private static final String TAG = BenchController.class.getSimpleName();
    private AbsListView.OnScrollListener mScrollListener;

    private Context context;
    private ZazoManagerProvider managerProvider;
    @InjectView(R.id.bench_list) ListView listView;
    @InjectView(R.id.contacts_heading) View slidingHeading;
    @InjectView(R.id.contacts_auto_complete_text_view) ClearableAutoCompleteTextView autoCompleteTextView;
    @InjectView(R.id.contacts_group_selector) Spinner groupSelector;

    private ImageView slidingIcon;
    private BenchAdapter adapter;
    private FriendFactory friendFactory;
    private BenchDataHandler benchDataHandler;
    private ArrayList<BenchObject> smsBenchObjects;
    private ArrayList<BenchObject> contactBenchObjects;
    private ArrayList<BenchObject> currentAllOnBench;
    private ContactsManager contactsManager;
    private boolean isBenchShown;

    // ----------------------
    // Constructor and setup
    // ----------------------
    public BenchController(Context c, ZazoManagerProvider mp) {
        context = c.getApplicationContext();
        managerProvider = mp;
        adapter = new BenchAdapter(context);

		contactsManager = new ContactsManager(context, this);
		benchDataHandler = new BenchDataHandler(context);
        benchDataHandler.setListener(this);
	}

	public void loadContacts() {
        friendFactory = FriendFactory.getFactoryInstance();
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
        isBenchShown = true;
    }

    @Override
    public void hideBench() {
        isBenchShown = false;
        if (isViewAttached()) {
            //clear contacts_auto_complete_text_view because after resume, "old filtering" word appear
            autoCompleteTextView.setText("");
            contactsManager.hideKeyboard();
        }
    }

    @Override
    public void updateBench() {
        AsyncTaskManager.executeAsyncTask(false, new AsyncTask<Void, Void, Void>() {
            private List<BenchObject>[] list;
            @Override
            protected Void doInBackground(Void... params) {
                list = allOnBench();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                adapter.setList(list);
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void attachView(View view) {
        if (!isViewAttached()) {
            ButterKnife.inject(this, view);
            listView.setSelection(0); // to scroll list to the first position
            if (adapter.isDataSetReady()) {
                listView.setAdapter(adapter);
                listView.setVisibility(View.VISIBLE);
            }
            slidingHeading.setVisibility(View.VISIBLE);
            slidingIcon = ButterKnife.findById(slidingHeading, R.id.icon);
            mScrollListener = new BenchScrollListener();
            listView.setOnItemClickListener(this);
            listView.setOnScrollListener(mScrollListener);
            groupSelector.setAdapter(new ContactsGroupAdapter(context));
        }
    }

    @Override
    public void detachView() {
        ButterKnife.reset(this);
    }

    @Override
    public boolean isBenchShowed() {
        return isBenchShown;
    }

	// ---------
	// Populate
	// ---------
	private void populate(final ArrayList<LinkedTreeMap<String, String>> phoneData) {
        AsyncTaskManager.executeAsyncTask(false, new AsyncTask<Void, Void, Void>() {
            private List<BenchObject>[] list;
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
                list = allOnBench();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                adapter.setList(list);
                if (isViewAttached()) {
                    if (listView.getAdapter() == adapter) {
                        adapter.notifyDataSetChanged();
                    } else {
                        listView.setAdapter(adapter);
                    }
                    listView.setVisibility(View.VISIBLE);
                }
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
        contactsManager.setupAutoComplete(autoCompleteTextView);
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

    private boolean isViewAttached() {
        return listView != null && slidingIcon != null;
    }

    class BenchAdapter extends BaseAdapter {

        private Context context;
        private List<BenchObject>[] lists;
        private List<ContactsGroup> groups;

        private final int icons[] = {R.drawable.bgn_thumb_1, R.drawable.bgn_thumb_2, R.drawable.bgn_thumb_3, R.drawable.bgn_thumb_4};
        private final int colors[];
        private SparseArray<ColorDrawable> colorDrawables = new SparseArray<>();

        public BenchAdapter(Context context) {
            this.context = context;
            this.lists = null;
            colors = context.getResources().getIntArray(R.array.thumb_colors);
        }

        public boolean isDataSetReady() {
            return lists != null;
        }

        public void setList(List<BenchObject>[] lists) {
            this.lists = lists;
            this.groups = ContactsGroup.getActive();
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
                holder = new ViewHolder(v);
                v.setTag(holder);
            }else{
                v = convertView;
                holder = (ViewHolder) v.getTag();
            }

            BenchObject item = getItem(position);

            Friend friend = FriendFactory.getFactoryInstance().find(item.friendId);
            if (friend != null) {
                if (friend.thumbExists()) {
                    holder.thumb.setImageBitmap(friend.thumbBitmap());
                    holder.thumbTitle.setText("");
                } else {
                    holder.thumbBackground.setImageDrawable(getColorDrawable(Convenience.getStringDependentItem(item.displayName, colors)));
                    holder.thumb.setImageResource(Convenience.getStringDependentItem(item.displayName, icons));
                    holder.thumbTitle.setText(friend.getInitials());
                }
            } else {
                holder.thumbBackground.setImageDrawable(getColorDrawable(Convenience.getStringDependentItem(item.displayName, colors)));
                holder.thumb.setImageResource(Convenience.getStringDependentItem(item.displayName, icons));
                holder.thumbTitle.setText(StringUtils.getInitials(item.firstName, item.lastName));
            }
            int itemViewType = getItemViewType(position);
            if (position == 0 || !isFirstPositionForType(position, itemViewType)) {
                holder.header.setVisibility(View.INVISIBLE);
            } else {
                holder.header.setVisibility(View.VISIBLE);
                holder.headerIcon.setImageResource(groups.get(itemViewType).getIconId());
            }
            if (itemViewType < getViewTypeCount() - 1 &&
                    isLastPositionForType(position, itemViewType) && lists[itemViewType+1].size() > 0) {
                holder.groupDivider.setVisibility(View.VISIBLE);
            } else {
                holder.groupDivider.setVisibility(View.INVISIBLE);
            }
            holder.name.setText(item.displayName);
            return v;
        }

        private Drawable getColorDrawable(int color) {
            ColorDrawable colorDrawable = colorDrawables.get(color);
            if (colorDrawable == null) {
                colorDrawable = new ColorDrawable(color);
                colorDrawables.put(color, colorDrawable);
            }
            return colorDrawable;
        }

        class ViewHolder{
            @InjectView(R.id.header) View header;
            @InjectView(R.id.group_divider) View groupDivider;
            @InjectView(R.id.thumb_background) ImageView thumbBackground;
            @InjectView(R.id.thumb) ImageView thumb;
            @InjectView(R.id.thumb_title) TextView thumbTitle;
            @InjectView(R.id.name) TextView name;
            @InjectView(R.id.icon) ImageView headerIcon;

            public ViewHolder(View v) {
                ButterKnife.inject(this, v);
            }
        }
    }

    private class BenchScrollListener implements AbsListView.OnScrollListener {

        BenchScrollListener() {
            slidingHeading.setTag(-1);
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (visibleItemCount > 0) {
                int type = adapter.getItemViewType(firstVisibleItem);
                View item = view.getChildAt(0);
                boolean lastForGroup = adapter.isLastPositionForType(firstVisibleItem, type);

                if ((int) slidingHeading.getTag() != type) {
                    slidingHeading.setTag(type);
                    slidingIcon.setImageResource(adapter.groups.get(type).getIconId());
                    if (type == 0) {
                        slidingHeading.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
                    } else {
                        slidingHeading.setBackgroundColor(context.getResources().getColor(R.color.bgn_main));
                    }
                }
                if (type < adapter.groups.size() - 1 && lastForGroup && item.getBottom() < slidingHeading.getHeight()) {
                    slidingHeading.setTranslationY(item.getBottom() - slidingHeading.getHeight());
                } else {
                    slidingHeading.setTranslationY(0);
                }
            }
        }
    }
}
