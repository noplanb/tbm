package com.noplanbees.tbm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.Profile;
import android.provider.ContactsContract.RawContacts;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.google.gson.internal.LinkedTreeMap;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

public class ContactsManager implements OnItemClickListener{

	private final static String STAG = ContactsManager.class.getSimpleName();
	private final String TAG = getClass().getSimpleName();

	public static interface ContactSelected{
		public void contactSelected(Contact contact);
	}


	private Context context;
	private ArrayList<String> autoCompleteNames;
	private ArrayAdapter<String> autoCompleteAdapter;
	private ContactSelected contactSelectedDelegate;
	private AutoCompleteTextView autoCompleteTextView;

	public ContactsManager(Context c, ContactSelected delegate){
		context = c;
		contactSelectedDelegate = delegate;
	}
	
	public ContactsManager(Context c){
		context = c;
	}

	//-------------------------
	// AutocompleteContactsView
	//-------------------------
	public void setupAutoComplete(AutoCompleteTextView view){
		new setupAutoCompleteAsync().execute(view);
	}

	public class setupAutoCompleteAsync extends AsyncTask<AutoCompleteTextView,Void,AutoCompleteTextView>{
		@Override
		protected AutoCompleteTextView doInBackground(AutoCompleteTextView... textViews) {
			setAutoCompleteNames();
			return textViews[0];
		}

		@Override
		protected void onPostExecute(AutoCompleteTextView textView) {
			super.onPostExecute(textView);
			setupContactsAutoCompleteView(textView);
		}
	}

	private void setAutoCompleteNames(){
		String[] projection = new String[]{Contacts.DISPLAY_NAME};
		// Show all contacts so user can enter the number in his contact list if it isnt there.
        // String selection = Contacts.HAS_PHONE_NUMBER + "=1";
		Cursor c = context.getContentResolver().query(Contacts.CONTENT_URI, projection, null, null , null);
		if (c == null || c.getCount() == 0){
			Log.e(TAG, "ERROR: setAutoCompleteData: got null cursor from contacs query");
			if (c != null)
				c.close();
			return;
		}
		
		Set<String> uniq = new HashSet<String>();
		int di = c.getColumnIndex(Contacts.DISPLAY_NAME);
		c.moveToFirst();
		do {
			uniq.add(c.getString(di));
		} while (c.moveToNext());
		c.close();
		autoCompleteNames = new ArrayList<String>();
	    autoCompleteNames.addAll(uniq);
		Log.i(STAG, "count = " + autoCompleteNames.size());
//		 printAllPhoneNumberObjectForNames(autoCompleteNames);
	}

	public void setupContactsAutoCompleteView(AutoCompleteTextView atv) {
		autoCompleteTextView = atv;
		autoCompleteAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, autoCompleteNames);
		autoCompleteTextView.setAdapter(autoCompleteAdapter);
		autoCompleteTextView.setOnItemClickListener(this);
	}

	//-----------------------
	// AutoComplete itemClick
	//-----------------------
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		String displayName = autoCompleteTextView.getText().toString();
		Contact contact = contactWithDisplayName(displayName);

		hideKeyboard();
		notifyContactSelected(contact);
		resetViews();
	}

	private void notifyContactSelected (Contact contact){
		if (contactSelectedDelegate != null)
			contactSelectedDelegate.contactSelected(contact);
	}

	public void clearTextView(){
		if (autoCompleteTextView == null)
			return;
		// autoCompleteTextView.getText().clear();
		TextKeyListener.clear(autoCompleteTextView.getEditableText());
	}
	
	public void hideKeyboard(){
		if (autoCompleteTextView == null)
			return;
		
		InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(autoCompleteTextView.getWindowToken(), 0);
	}
	
	public void resetViews() {
		hideKeyboard();
		clearTextView();
	}

	//----------------
	// Contact look up used to get contact from sms.
	//----------------
	public static Map<String, String> getFirstLastWithRawContactId(Context context, String rawContactId){
		Log.i(STAG, "getFirstLast: rawContactId:" + rawContactId);

		String where = Data.RAW_CONTACT_ID + "=" + rawContactId + " AND " + 
				Data.MIMETYPE + "='" + CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE + "'";
		Cursor c = context.getContentResolver().query(Data.CONTENT_URI, null, where, null, null);
		Map<String, String> result = new HashMap<String, String>();
		if (c != null && c.getCount() != 0) {
			c.moveToFirst();
			result.put(Contact.ContactKeys.FIRST_NAME, c.getString(c.getColumnIndex(CommonDataKinds.StructuredName.GIVEN_NAME)));
			result.put(Contact.ContactKeys.LAST_NAME, c.getString(c.getColumnIndex(CommonDataKinds.StructuredName.FAMILY_NAME)));
			result.put(Contact.ContactKeys.DISPLAY_NAME, c.getString(c.getColumnIndex(CommonDataKinds.StructuredName.DISPLAY_NAME)));
		}
		if (c != null)
			c.close();
		return result;
	}

	public static LinkedTreeMap<String, String> getFirstLastWithPhone(Context context, String phoneNumber){
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
		Cursor c = context.getContentResolver().query(uri,null,null,null,null);

		String displayName = null;
		if (c != null && c.getCount() != 0){
			c.moveToFirst();
			displayName = c.getString(c.getColumnIndex(PhoneLookup.DISPLAY_NAME));
		}
		if (c != null)
			c.close();
		
		if (displayName == null)
			return null;
		
		return getFirstLastWithDisplayName(displayName);
	}

	private static LinkedTreeMap<String,String> getFirstLastWithDisplayName(String displayName){
		LinkedTreeMap<String, String> r = new LinkedTreeMap<String, String>();
		if (displayName != null){
			r.put(Contact.ContactKeys.DISPLAY_NAME, displayName);

			int spaceI = displayName.indexOf(' ');
			if (spaceI == -1){
				r.put(Contact.ContactKeys.FIRST_NAME, displayName);
				r.put(Contact.ContactKeys.LAST_NAME, "");
			} else {
				r.put(Contact.ContactKeys.FIRST_NAME, displayName.substring(0, spaceI));
				r.put(Contact.ContactKeys.LAST_NAME, displayName.substring(spaceI+1, displayName.length()));
			}
		}
		return r;
	}

	//--------------------------
	// Phone numbers for contact
	//--------------------------
	public Contact contactWithDisplayName(String displayName){
		LinkedTreeMap<String, String> c = getFirstLastWithDisplayName(displayName);
		c.put(Contact.ContactKeys.DISPLAY_NAME, displayName);
		ArrayList<LinkedTreeMap<String, String>> vpos = validPhoneObjectsWithDisplayName(displayName);
		return new Contact(c, vpos);
	}
	
	public ArrayList<LinkedTreeMap<String,String>> validPhoneObjectsWithDisplayName(String displayName){
		Log.i(TAG, "validPhoneObjectsWithDisplayName: " + displayName);
		String cIds = contactIdsWithDisplayName(displayName);
		if (cIds == null)
			return null;
		return validPhoneObjectsWithContactIds(cIds);
	}
	
	public ArrayList<LinkedTreeMap<String, String>> validPhoneObjectsWithContactIds(String cIds){
		if (cIds == null)
			return null;

		String rcIds = rawContactIdsWithContactIds(cIds);
		
		ArrayList<LinkedTreeMap<String, String>> pnos =  phoneNumberObjectsWithRawContactIds(rcIds);
		Log.i(TAG, "validPhoneObjectsWithContactIds: " + " cIds:" + cIds + "rcIds: " + rcIds + " pnos:" + pnos);
		return validPhoneNumberObjectsWithPhoneNumbers(pnos);
	}

	private String contactIdsWithDisplayName(String displayName){
		// Android substitution for ? does weird things sometimes so just create the sql query myself.
		String selection = Contacts.DISPLAY_NAME + "=" + DatabaseUtils.sqlEscapeString(displayName);
		String[] projection = {Contacts._ID};
		Cursor c = context.getContentResolver().query(Contacts.CONTENT_URI, projection, selection, null, null);
		String r = null;

		if (c != null && c.getCount() != 0){
			r = "";
			c.moveToFirst();
			int i = 0;
			do{
				if (i != 0)
					r += ",";
				
				r += "'" +  c.getString(c.getColumnIndex(Contacts._ID)) + "'";
				i++;
			} while (c.moveToNext());
		}

		if (c != null)
			c.close();
		return r;
	}

	private String rawContactIdsWithContactIds(String contactIds){
		String selection = RawContacts.CONTACT_ID + " IN(" + contactIds + ")";
		String[] projection = {RawContacts._ID};
		Cursor c = context.getContentResolver().query(RawContacts.CONTENT_URI, projection, selection, null, null);
		String r = null;

		if (c != null && c.getCount() != 0){
			r = "";
			// Log.i(TAG, "rawContactIds count: " + c.getCount());

			c.moveToFirst();
			int i=0;
			do {
				if (i!=0)
					r += ",";
				
				r += "'" + (c.getString(c.getColumnIndex(RawContacts._ID))) + "'";
				i++;
			} while (c.moveToNext());
		}
		if (c != null)
			c.close();
		// Log.i(TAG, "rawContactIds: " + r);
		return r;
	}

	private ArrayList<LinkedTreeMap<String, String>> phoneNumberObjectsWithRawContactIds(String rawContactIds){
		ArrayList <LinkedTreeMap<String, String>> r = new ArrayList <LinkedTreeMap<String,String>>();
		String[] projection = {CommonDataKinds.Phone.NUMBER, CommonDataKinds.Phone.TYPE};
		String selection = Data.RAW_CONTACT_ID + " IN(" + rawContactIds + ") AND " + Data.MIMETYPE +"= '"+ CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";

		Cursor c = context.getContentResolver().query(Data.CONTENT_URI, projection, selection, null, null);

		if (c != null && c.getCount() != 0){
			Log.i(TAG, " dataCount:" + c.getCount());
			c.moveToFirst();
			do {
				LinkedTreeMap<String, String>e = new LinkedTreeMap<String,String>();
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

	private ArrayList<LinkedTreeMap<String, String>> validPhoneNumberObjectsWithPhoneNumbers(ArrayList<LinkedTreeMap<String,String>> phoneNumbers){
		PhoneNumberUtil pu = PhoneNumberUtil.getInstance();
		ArrayList<LinkedTreeMap<String,String>> r = new ArrayList<LinkedTreeMap<String,String>>();
		for (LinkedTreeMap<String, String> phoneHash : phoneNumbers){
			String ps = phoneHash.get(Contact.PhoneNumberKeys.PHONE_NUMBER);
			PhoneNumber pn = getPhoneObject(ps, UserFactory.current_user().getRegion(), UserFactory.current_user().getAreaCode());
			if (pn != null && pu.isValidNumber(pn)){
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
	private static PhoneNumber getPhoneObject(String phone, String defaultRegion, String defaultAreaCode){
		Log.i(STAG, "getPhoneObject:" +  phone + " defaultRegion:" + defaultRegion + " defaultAreaCode:" + defaultAreaCode);
		PhoneNumberUtil pu = PhoneNumberUtil.getInstance();
		PhoneNumber pn = null;
		try {
			pn = pu.parse(phone, defaultRegion);
			if (!pu.isValidNumber(pn)){
				pn = pu.parse(defaultAreaCode + phone, defaultRegion);
			}
		} catch (NumberParseException e) {
			try {
				pn = pu.parse(defaultAreaCode + phone, defaultRegion);
			} catch (NumberParseException e1) {
				Log.e(STAG, phone + ":  NumberParseException was thrown: " + e.toString());
			}
		}
		return pn;
	}

	// We want to be able to do this:
	// cursor = database.query(contentUri, projection, "columnName IN(?)", new String[] {" 'value1' , 'value2' "}, sortOrder);
	// so I convert String[]{"value1", "value2"}
	// to String[]{"'vaulue1', 'value2'} 
	// So that I can pass it into a query requesting multiple values for the same column
	private String[] convertStringArrayToSingleElementWithQuotes(String[] in){
		if (in == null)
			return null;

		String str = "";
		int i=0;
		for (String v : in){
			if (i !=0 )
				str += ",";

			str = str +  "'" + v + "'";
			i++;
		}
		return new String[]{str};
	}

	//----------------------
	// Phone Number Matching
	//----------------------
	public static boolean isPhoneNumberMatch(String p1, String p2){
		PhoneNumberUtil pu = PhoneNumberUtil.getInstance();
		PhoneNumberUtil.MatchType mt = pu.isNumberMatch(p1, p2);
		if (mt == PhoneNumberUtil.MatchType.SHORT_NSN_MATCH || mt == PhoneNumberUtil.MatchType.EXACT_MATCH)
			return true;
		else
			return false;
	}

	//-------------
	// User Profile
	//-------------
	public Contact userProfile(Context context){
		Log.i(STAG, "userProfile");
		String[] projection = {Contacts.DISPLAY_NAME};
		Cursor c = context.getContentResolver().query(Profile.CONTENT_URI, projection, null, null, null);
		
		Log.i(TAG, "cursor: " + c.getCount());
		
		if (c==null || c.getCount()==0)
			return null;
		
		c.moveToFirst();
		String displayName = c.getString(c.getColumnIndex(Contacts.DISPLAY_NAME));
		
		return contactWithDisplayName(displayName);
	}
	
	//----------------
	// Tests and debug
	//----------------
	public static void allPhones(Context context){
		String where = Data.MIMETYPE + "='" + CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";
		String[] projection = new String[] {CommonDataKinds.Phone.NUMBER};
		Cursor c = context.getContentResolver().query(Data.CONTENT_URI, projection, where, null, null);

		if (c == null)
			return;

		if (c.getCount() == 0){
			c.close();
			return;
		}

		Log.i(STAG, "count = " + c.getCount());
		c.moveToFirst();
		do {
			String phone = c.getString(c.getColumnIndex("data1"));
			PhoneNumberUtil pu = PhoneNumberUtil.getInstance();
			PhoneNumber pn;
			try {
				pn = pu.parse(phone, "US");
				String valid = pu.isValidNumber(pn) ? "valid" : "invalid";
				Log.i(STAG, pu.format(pn, PhoneNumberFormat.E164) + " " + pu.getRegionCodeForNumber(pn) + " " + valid);
			} catch (NumberParseException e) {
				Log.e(STAG, phone + ":  NumberParseException was thrown: " + e.toString());
			}
		} while (c.moveToNext());
		c.close();
	}

	private void printAutoCompleteNames(){
		for (String n : autoCompleteNames){
			Log.i(TAG, n);
		}
	}

	private void allDataWithRawContactId(String rawContactId){
		String selection = Data.RAW_CONTACT_ID + "='" + rawContactId + "'";
		Cursor c = context.getContentResolver().query(Data.CONTENT_URI, null, selection, null, null);
		Log.i(TAG, "Data Count: " + c.getCount());
		c.moveToFirst();
		do {
			for (int i=0; i<c.getColumnCount(); i++){
				Log.i(TAG, c.getColumnName(i) +":" + c.getString(i));
			}
		} while (c.moveToNext());
		c.close();
	}

	private void printAllPhoneNumberObjectForNames(String[] names){
		for (String name : names){
			Log.i(TAG, name + " "  +validPhoneObjectsWithDisplayName(name).toString());
		}
	}


}
