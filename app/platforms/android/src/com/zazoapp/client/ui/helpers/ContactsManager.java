package com.zazoapp.client.ui.helpers;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.Profile;
import android.provider.ContactsContract.RawContacts;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import com.google.gson.internal.LinkedTreeMap;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.zazoapp.client.R;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.model.Contact;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.utilities.AsyncTaskManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContactsManager implements OnItemClickListener {

	private final static String TAG = ContactsManager.class.getSimpleName();

	public static interface ContactSelected {
		public void contactSelected(Contact contact);
	}

	private Context context;
	private ContactSelected contactSelectedDelegate;
	private AutoCompleteTextView autoCompleteTextView;

	public ContactsManager(Context c, ContactSelected delegate) {
		context = c;
		contactSelectedDelegate = delegate;
	}

    public ContactsManager(Context c) {
        this(c, null);
    }

	// -------------------------
	// AutocompleteContactsView
	// -------------------------
	public void setupAutoComplete(AutoCompleteTextView view) {
		AsyncTaskManager.executeAsyncTask(false, new SetupAutoCompleteAsync(view), new Void[] {});
	}

	public class SetupAutoCompleteAsync extends AsyncTask<Void, Void, List<String>> {

		private AutoCompleteTextView view;

		public SetupAutoCompleteAsync(AutoCompleteTextView view) {
			this.view = view;
		}

		@Override
		protected List<String> doInBackground(Void... params) {
			return setAutoCompleteNames();
		}

		@Override
		protected void onPostExecute(List<String> params) {
			super.onPostExecute(params);
			setupContactsAutoCompleteView(view, params);
		}

		private List<String> setAutoCompleteNames() {
			String[] projection = new String[] { Contacts.DISPLAY_NAME };
			// Show all contacts so user can enter the number in his contact
			// list if it isnt there.
			// String selection = Contacts.HAS_PHONE_NUMBER + "=1";
			Cursor c = context.getContentResolver().query(Contacts.CONTENT_URI, projection, null, null, null);
			if (c == null || c.getCount() == 0) {
                Log.i(TAG, "ERROR: setAutoCompleteData: got null cursor from contacts query");
				if (c != null)
					c.close();
				return null;
			}

			Set<String> uniq = new HashSet<String>();
			int di = c.getColumnIndex(Contacts.DISPLAY_NAME);
			c.moveToFirst();
			do {
				uniq.add(c.getString(di));
			} while (c.moveToNext());
			c.close();
			ArrayList<String> autoCompleteNames = new ArrayList<String>();
			autoCompleteNames.addAll(uniq);
			Log.i(TAG, "count = " + autoCompleteNames.size());
			// printAllPhoneNumberObjectForNames(autoCompleteNames);
			return autoCompleteNames;
		}

		private void setupContactsAutoCompleteView(AutoCompleteTextView atv, List<String> autoCompleteNames) {
			autoCompleteTextView = atv;
			AutocompleteBaseAdapter autoCompleteAdapter = new AutocompleteBaseAdapter(context, autoCompleteNames);
			autoCompleteTextView.setAdapter(autoCompleteAdapter);
			autoCompleteTextView.setOnItemClickListener(ContactsManager.this);
		}
	}

	// -----------------------
	// AutoComplete itemClick
	// -----------------------
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		String displayName = autoCompleteTextView.getText().toString();
		Contact contact = contactWithDisplayName(displayName);

		hideKeyboard();
		notifyContactSelected(contact);
		resetViews();
	}

	private void notifyContactSelected(Contact contact) {
		if (contactSelectedDelegate != null)
			contactSelectedDelegate.contactSelected(contact);
	}

	public void clearTextView() {
		if (autoCompleteTextView == null)
			return;
		// autoCompleteTextView.getText().clear();
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

	// ----------------
	// Contact look up used to get contact from sms.
	// ----------------
	public static Map<String, String> getFirstLastWithRawContactId(Context context, String rawContactId) {
		Log.i(TAG, "getFirstLast: rawContactId:" + rawContactId);

		String where = Data.RAW_CONTACT_ID + "=" + rawContactId + " AND " + Data.MIMETYPE + "='"
				+ CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE + "'";
		Cursor c = context.getContentResolver().query(Data.CONTENT_URI, null, where, null, null);
		Map<String, String> result = new HashMap<String, String>();
		if (c != null && c.getCount() != 0) {
			c.moveToFirst();
			result.put(Contact.ContactKeys.FIRST_NAME,
					c.getString(c.getColumnIndex(CommonDataKinds.StructuredName.GIVEN_NAME)));
			result.put(Contact.ContactKeys.LAST_NAME,
					c.getString(c.getColumnIndex(CommonDataKinds.StructuredName.FAMILY_NAME)));
			result.put(Contact.ContactKeys.DISPLAY_NAME,
					c.getString(c.getColumnIndex(CommonDataKinds.StructuredName.DISPLAY_NAME)));
		}
		if (c != null)
			c.close();
		return result;
	}

    private static final String[] DISPLAY_NAME_PROJECTION = new String[] {PhoneLookup.DISPLAY_NAME};
	public static LinkedTreeMap<String, String> getFirstLastWithPhone(Context context, String phoneNumber) {
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
		Cursor c = context.getContentResolver().query(uri, DISPLAY_NAME_PROJECTION, null, null, null);

		String displayName = null;
		if (c != null && c.getCount() != 0) {
			c.moveToFirst();
			displayName = c.getString(c.getColumnIndex(PhoneLookup.DISPLAY_NAME));
		}
		if (c != null)
			c.close();

		return getFirstLastWithDisplayName(displayName);
	}

	public static LinkedTreeMap<String, String> getFirstLastWithDisplayName(String displayName) {
		LinkedTreeMap<String, String> r = null;
		if (displayName != null) {
            r = new LinkedTreeMap<String, String>();
			r.put(Contact.ContactKeys.DISPLAY_NAME, displayName);

			int spaceI = displayName.indexOf(' ');
			if (spaceI == -1) {
				r.put(Contact.ContactKeys.FIRST_NAME, displayName);
				r.put(Contact.ContactKeys.LAST_NAME, "");
			} else {
				r.put(Contact.ContactKeys.FIRST_NAME, displayName.substring(0, spaceI));
				r.put(Contact.ContactKeys.LAST_NAME, displayName.substring(spaceI + 1, displayName.length()));
			}
		}
		return r;
	}

    public static String getPhoneWithId(Context context, String id) {
        Cursor cursor = context.getContentResolver().query(CommonDataKinds.Phone.CONTENT_URI, null, CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
        String phone = null;
        while (cursor.moveToNext()) {
            phone = cursor.getString(cursor.getColumnIndex(CommonDataKinds.Phone.NUMBER));
            break;
        }
        cursor.close();
        return phone;
    }
	// --------------------------
	// Phone numbers for contact
	// --------------------------
	public Contact contactWithDisplayName(String displayName) {
        if (displayName == null) {
            throw new NullPointerException("contact must have display name");
        }
		LinkedTreeMap<String, String> c = getFirstLastWithDisplayName(displayName);
		ArrayList<LinkedTreeMap<String, String>> vpos = validPhoneObjectsWithDisplayName(displayName);
		return new Contact(c, vpos);
	}

    public Contact contactWithId(String id, String displayName) {
        if (id == null || displayName == null) {
            throw new NullPointerException("contact must have id and display name");
        }
        LinkedTreeMap<String, String> c = getFirstLastWithDisplayName(displayName);
        ArrayList<LinkedTreeMap<String, String>> vpos = validPhoneObjectsWithContactIds(id);
        return new Contact(c, vpos);
    }

	public ArrayList<LinkedTreeMap<String, String>> validPhoneObjectsWithDisplayName(String displayName) {
		Log.i(TAG, "validPhoneObjectsWithDisplayName: " + displayName);
		String cIds = contactIdsWithDisplayName(displayName);
		if (cIds == null)
			return null;
		return validPhoneObjectsWithContactIds(cIds);
	}

	public ArrayList<LinkedTreeMap<String, String>> validPhoneObjectsWithContactIds(String cIds) {
		if (cIds == null)
			return null;

		String rcIds = rawContactIdsWithContactIds(cIds);

		ArrayList<LinkedTreeMap<String, String>> pnos = phoneNumberObjectsWithRawContactIds(rcIds);
		Log.i(TAG, "validPhoneObjectsWithContactIds: " + " cIds:" + cIds + "rcIds: " + rcIds + " pnos:" + pnos);
		return validPhoneNumberObjectsWithPhoneNumbers(pnos);
	}

	private String contactIdsWithDisplayName(String displayName) {
		// Android substitution for ? does weird things sometimes so just create
		// the sql query myself.
		String selection = Contacts.DISPLAY_NAME + "=" + DatabaseUtils.sqlEscapeString(displayName);
		String[] projection = { Contacts._ID };
		Cursor c = context.getContentResolver().query(Contacts.CONTENT_URI, projection, selection, null, null);
		String r = null;

		if (c != null && c.getCount() != 0) {
			StringBuilder rb = new StringBuilder();
			c.moveToFirst();
			int i = 0;
            int idIndex = c.getColumnIndex(Contacts._ID);
			do {
				if (i != 0)
                    rb.append(",");

				rb.append("'").append(c.getString(idIndex)).append("'");
				i++;
			} while (c.moveToNext());
            r = rb.toString();
		}

		if (c != null)
			c.close();
		return r;
	}

	private String rawContactIdsWithContactIds(String contactIds) {
		String selection = RawContacts.CONTACT_ID + " IN(" + contactIds + ")";
		String[] projection = { RawContacts._ID };
		Cursor c = context.getContentResolver().query(RawContacts.CONTENT_URI, projection, selection, null, null);
		String r = null;

		if (c != null && c.getCount() != 0) {
            StringBuilder rb = new StringBuilder();
			// Log.i(TAG, "rawContactIds count: " + c.getCount());

			c.moveToFirst();
			int i = 0;
            int idIndex = c.getColumnIndex(RawContacts._ID);
			do {
				if (i != 0) {
                    rb.append(",");
                }
                rb.append("'").append(c.getString(idIndex)).append("'");
				i++;
			} while (c.moveToNext());
            r = rb.toString();
		}
		if (c != null)
			c.close();
		// Log.i(TAG, "rawContactIds: " + r);
		return r;
	}

	private ArrayList<LinkedTreeMap<String, String>> phoneNumberObjectsWithRawContactIds(String rawContactIds) {
		ArrayList<LinkedTreeMap<String, String>> r = new ArrayList<LinkedTreeMap<String, String>>();
		String[] projection = { CommonDataKinds.Phone.NUMBER, CommonDataKinds.Phone.TYPE };
		String selection = Data.RAW_CONTACT_ID + " IN(" + rawContactIds + ") AND " + Data.MIMETYPE + "= '"
				+ CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";

		Cursor c = context.getContentResolver().query(Data.CONTENT_URI, projection, selection, null, null);

		if (c != null && c.getCount() != 0) {
			Log.i(TAG, " dataCount:" + c.getCount());
			c.moveToFirst();
			do {
				LinkedTreeMap<String, String> e = new LinkedTreeMap<String, String>();
				int pt = c.getInt(c.getColumnIndex(CommonDataKinds.Phone.TYPE));
				String phoneType = (String) CommonDataKinds.Phone.getTypeLabel(context.getResources(), pt, "");
				e.put(Contact.PhoneNumberKeys.PHONE_TYPE, phoneType);
				e.put(Contact.PhoneNumberKeys.PHONE_TYPE_INT, pt + "");
				e.put(Contact.PhoneNumberKeys.PHONE_NUMBER, c.getString(c.getColumnIndex(CommonDataKinds.Phone.NUMBER)));
				r.add(e);
			} while (c.moveToNext());
		}
		if (c != null)
			c.close();

		return r;
	}

	private ArrayList<LinkedTreeMap<String, String>> validPhoneNumberObjectsWithPhoneNumbers(
			ArrayList<LinkedTreeMap<String, String>> phoneNumbers) {
		PhoneNumberUtil pu = PhoneNumberUtil.getInstance();
		ArrayList<LinkedTreeMap<String, String>> r = new ArrayList<LinkedTreeMap<String, String>>();
		for (LinkedTreeMap<String, String> phoneHash : phoneNumbers) {
			String ps = phoneHash.get(Contact.PhoneNumberKeys.PHONE_NUMBER);
			PhoneNumber pn = getPhoneObject(ps, UserFactory.current_user().getRegion(), UserFactory.current_user()
					.getAreaCode());
			if (pn != null && pu.isValidNumber(pn)) {
				phoneHash.put(Contact.PhoneNumberKeys.INTERNATIONAL, pu.format(pn, PhoneNumberFormat.INTERNATIONAL));
				phoneHash.put(Contact.PhoneNumberKeys.NATIONAL, pu.format(pn, PhoneNumberFormat.NATIONAL));
				phoneHash.put(Contact.PhoneNumberKeys.E164, pu.format(pn, PhoneNumberFormat.E164));
				phoneHash.put(Contact.PhoneNumberKeys.COUNTRY_CODE, pn.getCountryCode() + "");
				r.add(phoneHash);
			}
		}
		return r;
	}

	// Tries to prepend a default AreaCode in order to make a valid number.
	private static PhoneNumber getPhoneObject(String phone, String defaultRegion, String defaultAreaCode) {
		Log.i(TAG, "getPhoneObject:" + phone + " defaultRegion:" + defaultRegion + " defaultAreaCode:"
				+ defaultAreaCode);
		PhoneNumberUtil pu = PhoneNumberUtil.getInstance();
		PhoneNumber pn = null;
		try {
			pn = pu.parse(phone, defaultRegion);
			if (!pu.isValidNumber(pn)) {
				pn = pu.parse(defaultAreaCode + phone, defaultRegion);
			}
		} catch (NumberParseException e) {
			try {
				pn = pu.parse(defaultAreaCode + phone, defaultRegion);
			} catch (NumberParseException e1) {
                Dispatch.dispatch(TAG + phone + ":  NumberParseException was thrown: " + e.toString());
			}
		}
		return pn;
	}

    public static Set<String> getEmailsForPhone(Context context, String phone) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(android.provider.ContactsContract.Contacts.CONTENT_FILTER_URI,
                                       Uri.encode(phone));
        Cursor phoneCursor = cr.query(uri, null, null, null, null);
        Set<String> emails = new LinkedHashSet<>();
        if (phoneCursor != null && phoneCursor.moveToFirst()) {
            do {
                String id = phoneCursor.getString(phoneCursor.getColumnIndex(PhoneLookup._ID));
                Cursor emailCursor = context.getContentResolver().query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                        new String[] {id}, null);
                if (emailCursor != null && emailCursor.moveToFirst()) {
                    do {
                        String email = emailCursor.getString(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                        emails.add(email);
                    } while (emailCursor.moveToNext());
                }
                if (emailCursor != null) {
                    emailCursor.close();
                }
            } while (phoneCursor.moveToNext());

        }
        if (phoneCursor != null) {
            phoneCursor.close();
        }
        return emails;
    }

	// We want to be able to do this:
	// cursor = database.query(contentUri, projection, "columnName IN(?)", new
	// String[] {" 'value1' , 'value2' "}, sortOrder);
	// so I convert String[]{"value1", "value2"}
	// to String[]{"'vaulue1', 'value2'}
	// So that I can pass it into a query requesting multiple values for the
	// same column
	private String[] convertStringArrayToSingleElementWithQuotes(String[] in) {
		if (in == null)
			return null;

		String str = "";
		int i = 0;
		for (String v : in) {
			if (i != 0)
				str += ",";

			str = str + "'" + v + "'";
			i++;
		}
		return new String[] { str };
	}

	// ----------------------
	// Phone Number Matching
	// ----------------------
	public static boolean isPhoneNumberMatch(String p1, String p2) {
		PhoneNumberUtil pu = PhoneNumberUtil.getInstance();
		PhoneNumberUtil.MatchType mt = pu.isNumberMatch(p1, p2);
		if (mt == PhoneNumberUtil.MatchType.SHORT_NSN_MATCH || mt == PhoneNumberUtil.MatchType.EXACT_MATCH)
			return true;
		else
			return false;
	}

	// -------------
	// User Profile
	// -------------
	public Contact userProfile(Context context) {
		Log.i(TAG, "userProfile");
		String[] projection = { Contacts.DISPLAY_NAME };
		Cursor c = context.getContentResolver().query(Profile.CONTENT_URI, projection, null, null, null);

		Log.i(TAG, "cursor: " + c.getCount());

		if (c == null || c.getCount() == 0)
			return null;

		c.moveToFirst();
		String displayName = c.getString(c.getColumnIndex(Contacts.DISPLAY_NAME));

		return contactWithDisplayName(displayName);
	}

	// ----------------
	// Tests and debug
	// ----------------
	public static void allPhones(Context context) {
		String where = Data.MIMETYPE + "='" + CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";
		String[] projection = new String[] { CommonDataKinds.Phone.NUMBER };
		Cursor c = context.getContentResolver().query(Data.CONTENT_URI, projection, where, null, null);

		if (c == null)
			return;

		if (c.getCount() == 0) {
			c.close();
			return;
		}

		Log.i(TAG, "count = " + c.getCount());
		c.moveToFirst();
		do {
			String phone = c.getString(c.getColumnIndex("data1"));
			PhoneNumberUtil pu = PhoneNumberUtil.getInstance();
			PhoneNumber pn;
			try {
				pn = pu.parse(phone, "US");
				String valid = pu.isValidNumber(pn) ? "valid" : "invalid";
				Log.i(TAG, pu.format(pn, PhoneNumberFormat.E164) + " " + pu.getRegionCodeForNumber(pn) + " " + valid);
			} catch (NumberParseException e) {
				Log.e(TAG, phone + ":  NumberParseException was thrown: " + e.toString());
			}
		} while (c.moveToNext());
		c.close();
	}

//	private void printAutoCompleteNames() {
//		for (String n : autoCompleteNames) {
//			Log.i(TAG, n);
//		}
//	}
//
//	private void allDataWithRawContactId(String rawContactId) {
//		String selection = Data.RAW_CONTACT_ID + "='" + rawContactId + "'";
//		Cursor c = context.getContentResolver().query(Data.CONTENT_URI, null, selection, null, null);
//		Log.i(TAG, "Data Count: " + c.getCount());
//		c.moveToFirst();
//		do {
//			for (int i = 0; i < c.getColumnCount(); i++) {
//				Log.i(TAG, c.getColumnName(i) + ":" + c.getString(i));
//			}
//		} while (c.moveToNext());
//		c.close();
//	}
//
//	private void printAllPhoneNumberObjectForNames(String[] names) {
//		for (String name : names) {
//			Log.i(TAG, name + " " + validPhoneObjectsWithDisplayName(name).toString());
//		}
//	}

	private class AutocompleteBaseAdapter extends BaseAdapter implements Filterable{

		private Context context;
		private List<String> names;
		public List<String> originalNames;
		private SearchFilter filter;
		public Object mLock = new Object();
		
		public AutocompleteBaseAdapter(Context context, List<String> names) {
			this.context = context;
			this.names = names;
		}
		
		@Override
		public int getCount() {
			return names.size();
		}

		@Override
		public Object getItem(int position) {
			return names.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			TextView tw;
			ViewHolder holder = null;
			if(convertView == null){
				holder = new ViewHolder();
				v = LayoutInflater.from(context).inflate(R.layout.bench_search_list_item, parent, false);
				holder.name = (TextView) v.findViewById(R.id.name);
				holder.name.setBackgroundColor(context.getResources().getColor(R.color.contacts_search_bg));
				v.setTag(holder);
			}else{
				v = convertView;
				holder = (ViewHolder) v.getTag();
			}
			
			String name = names.get(position);
			
			holder.name.setText(name);
			
			return v;
		}

		@Override
		public Filter getFilter() {
	        if (filter == null) {
	            filter = new SearchFilter();
	        }
	        return filter;
		}
		
		private class ViewHolder{
			TextView name;
		}
		private class SearchFilter extends Filter {
			
		    @Override
		    protected FilterResults performFiltering(CharSequence filterString) {

		        FilterResults results = new FilterResults();
		        
	            if (originalNames == null) {
	                synchronized (mLock) {
	                    originalNames = new ArrayList<String>(names);
	                }
	            }
	            if (filterString == null || filterString.length() == 0) {
	                ArrayList<String> list;
	                synchronized (mLock) {
	                    list = new ArrayList<String>(originalNames);
	                }
	                results.values = list;
	                results.count = list.size();
	            } else {
	                String prefixString = filterString.toString().toLowerCase();
	                
	                ArrayList<String> values;
	                synchronized (mLock) {
	                    values = new ArrayList<String>(originalNames);
	                }


			        // find all matching objects here and add 
			        // them to allMatching, use filterString.
			        List<String> allMatching = new ArrayList<String>();

                    for (String name : values) {
                        if (name != null && name.toLowerCase().contains(prefixString)) {
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
	            names = (List<String>) results.values;
	            if (results.count > 0) {
	                notifyDataSetChanged();
	            } else {
	                notifyDataSetInvalidated();
	            }
		    }
		}
	}
}
