package com.zazoapp.client.bench;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
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
import com.zazoapp.client.ui.helpers.ThumbsHelper;
import com.zazoapp.client.ui.view.FilterWatcher;
import com.zazoapp.client.ui.view.SearchPanel;
import com.zazoapp.client.ui.view.TextImageView;
import com.zazoapp.client.utilities.AsyncTaskManager;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.StringUtils;
import de.hdodenhof.circleimageview.CircleImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BenchController implements BenchDataHandler.BenchDataHandlerCallback, OnItemClickListener,
        BenchViewManager, AdapterView.OnItemSelectedListener, View.OnTouchListener {

    private static final String TAG = BenchController.class.getSimpleName();

    private Context context;
    private ZazoManagerProvider managerProvider;
    @InjectView(R.id.bench_list) ListView listView;
    @InjectView(R.id.contacts_heading) View slidingHeading;
    @InjectView(R.id.contacts_group_selector) Spinner groupSelector;
    @InjectView(R.id.progress) View progressView;

    private TextImageView slidingIcon;
    private BenchAdapter adapter;
    private FriendFactory friendFactory;
    private BenchDataHandler benchDataHandler;
    private ArrayList<BenchObject> smsBenchObjects;
    private BenchObjectList contactBenchObjects;
    private ArrayList<BenchObject> favoriteBenchObjects;
    private BenchObjectList currentAllOnBench;
    private ContactsManager contactsManager;
    private SearchPanel searchPanel;
    private boolean isBenchShown;
    private GeneralContactsGroup currentSelectedGroup = GeneralContactsGroup.ALL;
    private final Object filterLock = new Object();
    private BenchListener benchListener;
    private boolean firstLoaded;

    // ----------------------
    // Constructor and setup
    // ----------------------
    public BenchController(Context c, ZazoManagerProvider mp) {
        context = c.getApplicationContext();
        managerProvider = mp;
        adapter = new BenchAdapter(context);

		contactsManager = new ContactsManager(context);
		benchDataHandler = new BenchDataHandler(context);
        benchDataHandler.setListener(this);
	}

	public void loadContacts() {
        friendFactory = FriendFactory.getFactoryInstance();
        GridManager.getInstance().moveFriendsWithUnviewedOnGrid();
		benchDataHandler.getRankedPhoneData();
	}

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        BenchObject bo = adapter.getItem(position);
        Log.i(TAG, "Position:" + position + " " + bo.displayName);

        // if it is just a friend on bench, move it to grid
        Friend friend = friendFactory.find(bo.friendId);
        if (friend != null) {
            // TODO swap friends
            GridManager.getInstance().moveFriendToGrid(friend);
            hideBench();
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

    @Override
    public void showBench() {
        requestShowBench(true);
    }

    @Override
    public void hideBench() {
        requestShowBench(false);
    }

    @Override
    public void setBenchShown(boolean isShown) {
        isBenchShown = isShown;
        if (!isShown && isViewAttached()) {
            //clear contacts_auto_complete_text_view because after resume, "old filtering" word appear
            resetViews();
        }
    }

    private void requestShowBench(boolean show) {
        if (benchListener != null) {
            benchListener.onBenchStateChangeRequest(show);
        }
    }

    @Override
    public void updateBench() {
        if (!firstLoaded) {
            return;
        }
        AsyncTaskManager.executeAsyncTask(false, new AsyncTask<Void, Void, Void>() {
            private BenchObjectList list;
            @Override
            protected Void doInBackground(Void... params) {
                list = allOnBench();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                applyFilter(searchPanel.getText());
            }
        });
    }

    @Override
    public void attachView(View view) {
        if (!isViewAttached()) {
            ButterKnife.inject(this, view);
            listView.setOnTouchListener(this);
            listView.setSelection(0); // to scroll list to the first position
            listView.setTextFilterEnabled(true);
            if (adapter.isDataSetReady()) {
                listView.setAdapter(adapter);
                applyFilter("");
                doListViewAppearing();
            }
            slidingHeading.setVisibility(View.VISIBLE);
            slidingIcon = ButterKnife.findById(slidingHeading, R.id.icon);
            AbsListView.OnScrollListener mScrollListener = new BenchScrollListener();
            listView.setOnItemClickListener(this);
            listView.setOnScrollListener(mScrollListener);
            groupSelector.setAdapter(new ContactsGroupAdapter(context));
            groupSelector.setOnItemSelectedListener(this);
            searchPanel = new SearchPanel(view);
            searchPanel.addTextChangedListener(new FilterWatcher() {
                @Override
                protected void applyFilter(CharSequence text) {
                    BenchController.this.applyFilter(text);
                }
            });
        }
    }

    private void doListViewAppearing() {
        final float offset = Convenience.dpToPx(context, 50);
        progressView.animate().alpha(0).translationY(-offset).setListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (listView != null) {
                            listView.setTranslationY(offset);
                            listView.setVisibility(View.VISIBLE);
                            listView.setAlpha(0f);
                            listView.animate().alpha(1f).translationY(0).start();
                        }
                    }
                }).start();
    }

    @Override
    public void detachView() {
        ButterKnife.reset(this);
    }

    @Override
    public boolean isBenchShown() {
        return isBenchShown;
    }

	// ---------
	// Populate
	// ---------
	private void populate(final ArrayList<LinkedTreeMap<String, String>> phoneData) {
        AsyncTaskManager.executeAsyncTask(false, new AsyncTask<Void, Void, Void>() {
            private BenchObjectList list;
            @Override
            protected Void doInBackground(Void... params) {
                smsBenchObjects = new ArrayList<>();
                List<BenchObject> contacts = new ArrayList<>();
                contactBenchObjects = new BenchObjectList();
                favoriteBenchObjects = new ArrayList<>();
                for (LinkedTreeMap<String, String> e : phoneData) {
                    LinkedTreeMap<String, String> b = new LinkedTreeMap<>();
                    b.put(BenchObject.Keys.FIRST_NAME, e.get(BenchDataHandler.Keys.FIRST_NAME));
                    b.put(BenchObject.Keys.LAST_NAME, e.get(BenchDataHandler.Keys.LAST_NAME));
                    b.put(BenchObject.Keys.DISPLAY_NAME, e.get(BenchDataHandler.Keys.DISPLAY_NAME));
                    b.put(BenchObject.Keys.MOBILE_NUMBER, e.get(BenchDataHandler.Keys.MOBILE_NUMBER));
                    b.put(BenchObject.Keys.CONTACT_ID, e.get(BenchDataHandler.Keys.CONTACT_ID));
                    b.put(BenchObject.Keys.FAVORITE, e.get(BenchDataHandler.Keys.FAVORITE));
                    b.put(BenchObject.Keys.HAS_PHONE, e.get(BenchDataHandler.Keys.HAS_PHONE));
                    BenchObject bo = new BenchObject(b);
                    switch (bo.getGroup().getGeneralGroup()) {
                        case SMS_CONTACTS:
                            smsBenchObjects.add(bo);
                            break;
                        case FAVORITES:
                            favoriteBenchObjects.add(bo);
                            break;
                        case CONTACTS:
                            contacts.add(bo);
                            break;
                    }
                }
                sortBench(contacts);
                for (BenchObject contact : contacts) {
                    contactBenchObjects.add(contact);
                }
                list = allOnBench();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (isViewAttached()) {
                    boolean groupsCountChanged = adapter.getViewTypeCount() != list.getGroupCount();
                    if (groupsCountChanged) {
                        listView.setAdapter(null);
                        adapter.setList(list);
                        listView.setAdapter(adapter);
                    } else {
                        adapter.setList(list);
                    }
                    applyFilter(searchPanel.getText());
                    if (listView.getVisibility() != View.VISIBLE) {
                        doListViewAppearing();
                    }
                } else {
                    adapter.setList(list);
                }
                firstLoaded = true;
            }
        });
    }

    private BenchObjectList allOnBench() {
        List<List<BenchObject>> allLists = new ArrayList<>();
        List<BenchObject> bench = benchFriendsAsBenchObjects();
        List<BenchObject> sms = dedupedSmsBenchObjects();
        if (!bench.isEmpty()) {
            allLists.add(bench);
        }
        if (favoriteBenchObjects != null && !favoriteBenchObjects.isEmpty()) {
            allLists.add(favoriteBenchObjects);
        }
        if (!sms.isEmpty()) {
            allLists.add(sms);
        }

        synchronized (filterLock) {
            currentAllOnBench = new BenchObjectList();
            for (List<BenchObject> list : allLists) {
                currentAllOnBench.addGroup(list, list.get(0).getGroup());
            }
            if (contactBenchObjects != null && !contactBenchObjects.isEmpty()) {
                currentAllOnBench.addWithSubgroups(contactBenchObjects);
            }
        }
        return currentAllOnBench;
    }

    // --------------------------
    // Friend overflow from grid
    // --------------------------
    private ArrayList<BenchObject> benchFriendsAsBenchObjects() {
        ArrayList<BenchObject> r = new ArrayList<>();
        for (Friend f : FriendFactory.getFactoryInstance().allEnabled()) {
            LinkedTreeMap<String, String> e = new LinkedTreeMap<>();
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

    // -------------
    // Sms Contacts
    // -------------
    @Override
    public void receivePhoneData(ArrayList<LinkedTreeMap<String, String>> phoneData) {
        populate(phoneData);
    }

    private ArrayList<BenchObject> dedupedSmsBenchObjects() {
        ArrayList<BenchObject> r = new ArrayList<>();
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
                return lhs.displayName.compareToIgnoreCase(rhs.displayName);
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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.equals(groupSelector)) {
            if (!currentSelectedGroup.equals(parent.getSelectedItem())) {
                currentSelectedGroup = (GeneralContactsGroup) parent.getSelectedItem();
                applyFilter(searchPanel.getText());
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void selectGroup(GeneralContactsGroup group) {
        if (groupSelector != null) {
            List<GeneralContactsGroup> activeGroups = GeneralContactsGroup.getActive();
            int index;
            if ((index = activeGroups.indexOf(group)) >= 0) {
                groupSelector.setSelection(index);
            }
        }
    }

    public void setBenchListener(BenchListener benchListener) {
        this.benchListener = benchListener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.bench_list && searchPanel != null) {
            searchPanel.hideKeyboard();
        }
        return false;
    }

    class BenchAdapter extends BaseAdapter implements Filterable {

        private Context context;
        private SearchFilter filter;
        private BenchObjectList list;
        private List<ContactsGroup> groups;
        private ThumbsHelper tHelper;

        public BenchAdapter(Context context) {
            this.context = context;
            this.list = null;
            tHelper = new ThumbsHelper(context);
        }

        public boolean isDataSetReady() {
            return list != null && groups != null;
        }

        public void setList(BenchObjectList list) {
            this.list = list;
            this.groups = list.getGroups();
        }

        public boolean isFirstPositionForType(int position, int type) {
            return list.isFirstForGroup(position, groups.get(type));
        }

        public boolean isLastPositionForType(int position, int type) {
            return list.isLastForGroup(position, groups.get(type));
        }

        @Override
        public int getViewTypeCount() {
            return (list == null || list.isEmpty()) ? 1 : list.getGroupCount();
        }

        @Override
        public int getItemViewType(int position) {
            ContactsGroup group = list.getGroup(position);
            int index = groups.indexOf(group);
            if (index >= 0) {
                return index;
            }
            throw new IndexOutOfBoundsException("Wrong position: " + position + " " + getCount());  // never get here
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
                    holder.thumb.setImageResource(tHelper.getIcon(item.displayName));
                    holder.thumb.setFillColor(tHelper.getColor(item.displayName));
                    holder.thumbTitle.setText(friend.getInitials());
                }
                if (friend.incomingMessagesNotViewedCount() > 0) {
                    holder.thumb.setBorderColorResource(R.color.primary);
                    holder.thumb.setBorderWidth((int) Convenience.dpToPx(context, 2.5f));
                    holder.thumb.setBorderOverlay(true);
                    holder.twUnreadCount.setText(String.valueOf(friend.incomingMessagesNotViewedCount()));
                    holder.unreadCountLayout.setVisibility(View.VISIBLE);
                } else {
                    holder.thumb.setBorderWidth(0);
                    holder.thumb.setBorderOverlay(false);
                    holder.unreadCountLayout.setVisibility(View.GONE);
                }
            } else {
                holder.thumb.setImageResource(tHelper.getIcon(item.displayName));
                holder.thumb.setFillColor(tHelper.getColor(item.displayName));
                holder.thumbTitle.setText(StringUtils.getInitials(item.firstName, item.lastName));
                holder.thumb.setBorderWidth(0);
                holder.thumb.setBorderOverlay(false);
                holder.unreadCountLayout.setVisibility(View.GONE);
            }
            int itemViewType = getItemViewType(position);
            ContactsGroup group = groups.get(itemViewType);
            if (!isFirstPositionForType(position, itemViewType)) {
                holder.header.setVisibility(View.INVISIBLE);
                holder.header.setTag(null);
            } else {
                holder.header.setVisibility(View.VISIBLE);
                holder.header.setTag(group);
                if (group.isGeneralGroup()) {
                    holder.headerIcon.setImageAndText(group.getIcon(), "");
                } else {
                    holder.headerIcon.setImageAndText(null, group.getText());
                }
            }
            if (itemViewType < getViewTypeCount() - 1 && isLastPositionForType(position, itemViewType) && group.getGeneralGroup() != GeneralContactsGroup.CONTACTS) {
                holder.groupDivider.setVisibility(View.VISIBLE);
            } else {
                holder.groupDivider.setVisibility(View.INVISIBLE);
            }
            holder.name.setText(item.displayName);
            return v;
        }

        @Override
        public Filter getFilter() {
            if (filter == null) {
                filter = new SearchFilter();
            }
            return filter;
        }

        class ViewHolder{
            @InjectView(R.id.header) View header;
            @InjectView(R.id.group_divider) View groupDivider;
            @InjectView(R.id.thumb) CircleImageView thumb;
            @InjectView(R.id.thumb_title) TextView thumbTitle;
            @InjectView(R.id.name) TextView name;
            @InjectView(R.id.icon) TextImageView headerIcon;
            @InjectView(R.id.unread_count_layout) View unreadCountLayout;
            @InjectView(R.id.tw_unread_count) TextView twUnreadCount;

            public ViewHolder(View v) {
                ButterKnife.inject(this, v);
            }
        }

        private class SearchFilter extends Filter {

            @Override
            protected FilterResults performFiltering(CharSequence filterString) {

                FilterResults results = new FilterResults();
                BenchObjectList listCopy;
                synchronized (filterLock) {
                    listCopy = (BenchObjectList) currentAllOnBench.clone();
                }
                if (filterString == null || filterString.length() == 0) {
                    // find all matching objects here and add
                    // them to allMatching, use filterString.
                    BenchObjectList allMatching = new BenchObjectList();
                    ArrayList<Friend> friendsOnGrid = GridManager.getInstance().friendsOnGrid();
                    ArrayList<String> friendIdsOnGrid = new ArrayList<>();
                    for (Friend friend : friendsOnGrid) {
                        friendIdsOnGrid.add(friend.getId());
                    }
                    for (BenchObject name : listCopy) {
                        if (currentSelectedGroup == GeneralContactsGroup.ALL || currentSelectedGroup.equals(name.getGroup().getGeneralGroup())) {
                            if (name.getGroup().getGeneralGroup() != GeneralContactsGroup.ZAZO_FRIEND || !friendIdsOnGrid.contains(name.friendId)) {
                                allMatching.add(name);
                            }
                        }
                    }

                    results.values = allMatching;
                    results.count = allMatching.size();
                } else {
                    String prefixString = filterString.toString().toLowerCase();

                    // find all matching objects here and add
                    // them to allMatching, use filterString.
                    BenchObjectList allMatching = new BenchObjectList();
                    for (BenchObject name : listCopy) {
                        if (name != null && name.displayName.toLowerCase().contains(prefixString)) {
                            allMatching.add(name);
                        }
                    }

                    results.values = allMatching;
                    results.count = allMatching.size();
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                BenchObjectList newList = (BenchObjectList) results.values;
                if (isViewAttached()) {
                    boolean groupsCountChanged = adapter.getViewTypeCount() != newList.getGroupCount();
                    if (groupsCountChanged) {
                        listView.setAdapter(null);
                        setList(newList);
                        listView.setAdapter(adapter);
                    } else {
                        setList(newList);
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    setList(newList);
                }
            }
        }
    }

    protected void applyFilter(CharSequence text) {
        if (listView.getAdapter() != null) {
            adapter.getFilter().filter(text);
        }
    }

    private class BenchScrollListener implements AbsListView.OnScrollListener {

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (visibleItemCount > 0) {
                int type = adapter.getItemViewType(firstVisibleItem);
                ContactsGroup typeObject = adapter.groups.get(type);
                View item = view.getChildAt(0);
                View currentItemHeading = ((BenchAdapter.ViewHolder) item.getTag()).header;
                View nextView = null;
                View nextViewHeading = null;
                // if second visible item exists, get its view and header
                if (visibleItemCount > 1) {
                    nextView = view.getChildAt(1);
                    nextViewHeading = ((BenchAdapter.ViewHolder) nextView.getTag()).header;
                }
                // check if sliding heading should be changed
                if (!typeObject.equals(slidingHeading.getTag())) {
                    slidingHeading.setTag(typeObject);
                    if (typeObject.isGeneralGroup()) {
                        slidingIcon.setImageAndText(typeObject.getIcon(), "");
                    } else {
                        slidingIcon.setImageAndText(null, typeObject.getText());
                    }
                    // As soon as sliding heading is changed update next item heading's visibility state
                    if (nextView != null && nextViewHeading.getTag() != null) {
                        nextViewHeading.setVisibility(View.VISIBLE);
                    }
                }
                // While current group is shown by scrolling heading, keep current item heading invisible
                if (typeObject.equals(currentItemHeading.getTag())) {
                    currentItemHeading.setVisibility(View.INVISIBLE);
                } else if (currentItemHeading.getTag() != null) {
                    currentItemHeading.setVisibility(View.VISIBLE);
                }
                // Slide and animate last group heading
                boolean lastForGroup = adapter.isLastPositionForType(firstVisibleItem, type);
                if (type < adapter.groups.size() - 1 && lastForGroup && item.getBottom() < slidingHeading.getHeight()) {
                    slidingHeading.setTranslationY(Math.max(item.getBottom(), 1) - slidingHeading.getHeight());
                    float alpha = item.getBottom() / (float) slidingHeading.getHeight();
                    slidingIcon.setAlpha(alpha*alpha);
                } else {
                    slidingHeading.setTranslationY(0);
                    slidingIcon.setAlpha(1f);
                }
                // To support overscroll issues restore original heading instead of scrolling one
                if (adapter.isFirstPositionForType(firstVisibleItem, 0) && item.getTop() >= 0) {
                    slidingHeading.setVisibility(View.INVISIBLE);
                    currentItemHeading.setVisibility(View.VISIBLE);
                } else {
                    slidingHeading.setVisibility(View.VISIBLE);
                }
            } else {
                // Hide sliding heading if no visible items in the list
                slidingHeading.setVisibility(View.INVISIBLE);
                slidingHeading.setTag(null);
            }
        }
    }

    public void resetViews() {
        if (isViewAttached()) {
            searchPanel.hideKeyboard();
            searchPanel.clearTextView();
            searchPanel.closeSearch();
        }
        if (groupSelector != null && groupSelector.getSelectedItemPosition() != 0) {
            groupSelector.setSelection(0);
        }
    }

    public interface BenchListener {
        void onBenchStateChangeRequest(boolean visible);
    }
}
