package com.zazoapp.client.bench;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
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
import com.zazoapp.client.ui.view.TextImageView;
import com.zazoapp.client.utilities.AsyncTaskManager;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BenchController implements BenchDataHandler.BenchDataHandlerCallback, OnItemClickListener,
        BenchViewManager, AdapterView.OnItemSelectedListener {

    private static final String TAG = BenchController.class.getSimpleName();

    private Context context;
    private ZazoManagerProvider managerProvider;
    @InjectView(R.id.bench_list) ListView listView;
    @InjectView(R.id.contacts_heading) View slidingHeading;
    @InjectView(R.id.contacts_auto_complete_text_view) ClearableAutoCompleteTextView autoCompleteTextView;
    @InjectView(R.id.contacts_group_selector) Spinner groupSelector;

    private TextImageView slidingIcon;
    private BenchAdapter adapter;
    private FriendFactory friendFactory;
    private BenchDataHandler benchDataHandler;
    private ArrayList<BenchObject> smsBenchObjects;
    private BenchObjectList contactBenchObjects;
    private ArrayList<BenchObject> favoriteBenchObjects;
    private BenchObjectList currentAllOnBench;
    private ContactsManager contactsManager;
    private boolean isBenchShown;
    private GeneralContactsGroup currentSelectedGroup = GeneralContactsGroup.ALL;
    private final Object filterLock = new Object();

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
		benchDataHandler.getRankedPhoneData();
	}

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        hideBench();
        BenchObject bo = adapter.getItem(position);
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

    @Override
    public void showBench() {
        isBenchShown = true;
    }

    @Override
    public void hideBench() {
        isBenchShown = false;
        if (isViewAttached()) {
            //clear contacts_auto_complete_text_view because after resume, "old filtering" word appear
            resetViews();
        }
    }

    @Override
    public void updateBench() {
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
            AbsListView.OnScrollListener mScrollListener = new BenchScrollListener();
            listView.setOnItemClickListener(this);
            listView.setOnScrollListener(mScrollListener);
            groupSelector.setAdapter(new ContactsGroupAdapter(context));
            groupSelector.setOnItemSelectedListener(this);
            autoCompleteTextView.addTextChangedListener(new MyWatcher());
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
        for (Friend f : GridManager.getInstance().friendsOnBench()) {
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
        //contactsManager.setupAutoComplete(autoCompleteTextView);
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
                applyFilter();
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    class BenchAdapter extends BaseAdapter implements Filterable {

        private Context context;
        private SearchFilter filter;
        private BenchObjectList list;
        private List<ContactsGroup> groups;

        private final int icons[] = {R.drawable.bgn_thumb_1, R.drawable.bgn_thumb_2, R.drawable.bgn_thumb_3, R.drawable.bgn_thumb_4};
        private final int colors[];
        private SparseArray<ColorDrawable> colorDrawables = new SparseArray<>();

        public BenchAdapter(Context context) {
            this.context = context;
            this.list = null;
            colors = context.getResources().getIntArray(R.array.thumb_colors);
        }

        public boolean isDataSetReady() {
            return list != null;
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
            return list.getGroupCount();
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
            ContactsGroup group = groups.get(itemViewType);
            if (!isFirstPositionForType(position, itemViewType)) {
                holder.header.setVisibility(View.INVISIBLE);
            } else {
                holder.header.setVisibility(View.VISIBLE);
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

        private Drawable getColorDrawable(int color) {
            ColorDrawable colorDrawable = colorDrawables.get(color);
            if (colorDrawable == null) {
                colorDrawable = new ColorDrawable(color);
                colorDrawables.put(color, colorDrawable);
            }
            return colorDrawable;
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
            @InjectView(R.id.thumb_background) ImageView thumbBackground;
            @InjectView(R.id.thumb) ImageView thumb;
            @InjectView(R.id.thumb_title) TextView thumbTitle;
            @InjectView(R.id.name) TextView name;
            @InjectView(R.id.icon) TextImageView headerIcon;

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
                    for (BenchObject name : listCopy) {
                        if (currentSelectedGroup == GeneralContactsGroup.ALL || currentSelectedGroup.equals(name.getGroup().getGeneralGroup())) {
                            allMatching.add(name);
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
                            if (currentSelectedGroup == GeneralContactsGroup.ALL || currentSelectedGroup.equals(name.getGroup().getGeneralGroup())) {
                                allMatching.add(name);
                            }
                        }
                    }

                    results.values = allMatching;
                    results.count = allMatching.size();
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                setList((BenchObjectList) results.values);
                if (results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        }
    }

    private class MyWatcher implements TextWatcher {
        public void afterTextChanged(Editable s) {
            applyFilter();
        }
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    private void applyFilter() {
        // filter is applied only when a minimum number of characters
        // was typed in the text view
        if (enoughToFilter()) {
            if (listView.getAdapter() != null) {
                adapter.getFilter().filter(autoCompleteTextView.getText());
            }
        } else {
            if (listView.getAdapter() != null) {
                adapter.getFilter().filter(null);
            }
        }
    }

    private boolean enoughToFilter() {
        return autoCompleteTextView.getText().length() >= 1;
    }

    private class BenchScrollListener implements AbsListView.OnScrollListener {

        BenchScrollListener() {
            slidingIcon.setTag(-1);
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (visibleItemCount > 0) {
                int type = adapter.getItemViewType(firstVisibleItem);
                ContactsGroup typeObject = adapter.groups.get(type);
                View item = view.getChildAt(0);
                boolean lastForGroup = adapter.isLastPositionForType(firstVisibleItem, type);
                if (!typeObject.equals(slidingHeading.getTag())) {
                    slidingHeading.setTag(typeObject);
                    if (typeObject.isGeneralGroup()) {
                        slidingIcon.setImageAndText(typeObject.getIcon(), "");
                    } else {
                        slidingIcon.setImageAndText(null, typeObject.getText());
                    }
                }
                if (type < adapter.groups.size() - 1 && lastForGroup && item.getBottom() < slidingHeading.getHeight()) {
                    slidingHeading.setTranslationY(item.getBottom() - slidingHeading.getHeight());
                } else {
                    slidingHeading.setTranslationY(0);
                }
                if (adapter.isFirstPositionForType(firstVisibleItem, 0) && item.getTop() >= 0) {
                    slidingHeading.setVisibility(View.INVISIBLE);
                } else {
                    slidingHeading.setVisibility(View.VISIBLE);
                }
            } else {
                slidingHeading.setVisibility(View.INVISIBLE);
                slidingHeading.setTag(-1);
            }
        }
    }

    public void clearTextView() {
        if (autoCompleteTextView == null)
            return;
        TextKeyListener.clear(autoCompleteTextView.getEditableText());
    }

    public void hideKeyboard() {
        if (autoCompleteTextView == null)
            return;

        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(autoCompleteTextView.getWindowToken(), 0);
    }

    public void resetViews() {
        hideKeyboard();
        clearTextView();
    }
}
