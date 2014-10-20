package com.noplanbees.tbm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.google.gson.internal.LinkedTreeMap;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

public class ContactsManager implements OnItemClickListener{

	private final static String STAG = ContactsManager.class.getSimpleName();
	private final String TAG = getClass().getSimpleName();
	
	public static interface ContactSelected{
		public void contactSelected(LinkedTreeMap<String, String> contact);
	}
	
	public static class Keys{
		public static final String FIRST_NAME = "firstName";
		public static final String LAST_NAME = "lastName";
		public static final String DISPLAY_NAME = "displayName";
		public static final String LOOKUP_KEY = "lookupKey";
		public static final String PHONE_TYPE = "phoneType";
		public static final String PHONE_NUMBER = "phoneNumber";
	}
		
	private Context context;
	private String[] autoCompleteNames;
	private ArrayAdapter<String> autoCompleteAdapter;
	private ContactSelected contactSelectedDelegate;
	private AutoCompleteTextView autoCompleteTextView;
	
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
		String selection = Contacts.HAS_PHONE_NUMBER + "=1";
		Cursor c = context.getContentResolver().query(Contacts.CONTENT_URI, projection, selection, null , null);
		if (c == null || c.getCount() == 0){
			Log.e(TAG, "ERROR: setAutoCompleteData: got null cursor from contacs query");
			return;
		}
		
		autoCompleteNames = new String[c.getCount()];
		int di = c.getColumnIndex(Contacts.DISPLAY_NAME);
		c.moveToFirst();
		int i = 0;
		do {
			autoCompleteNames[i] = c.getString(di);
			i++;
		} while (c.moveToNext());
		c.close();
		
		Log.i(STAG, "count = " + autoCompleteNames.length);
	}

	public void setupContactsAutoCompleteView(AutoCompleteTextView atv) {
		autoCompleteTextView = atv;
		autoCompleteAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, autoCompleteNames);
		autoCompleteTextView.setAdapter(autoCompleteAdapter);
		autoCompleteTextView.setOnItemClickListener(this);
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		String name = autoCompleteTextView.getText().toString();
		Log.i(TAG, "onItemClick:" + name);	
		Log.i(TAG, "validPhones:" +validPhonesWithDisplayName(name));
	}

	public void setContactSelectedDelegate(ContactSelected csd){
		contactSelectedDelegate = csd;
	}
	
	private void notifyContactSelected (LinkedTreeMap<String, String> contact){
		if (contactSelectedDelegate != null)
			contactSelectedDelegate.contactSelected(contact);
	}
	
	//----------------
	// Contact look up
	//----------------
	public static Map<String, String> getFirstLastWithRawContactId(Context context, String rawContactId){
		Log.i(STAG, "getFirstLast: rawContactId:" + rawContactId);

		String where = Data.RAW_CONTACT_ID + "=" + rawContactId + " AND " + 
				Data.MIMETYPE + "='" + CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE + "'";
		Cursor c = context.getContentResolver().query(Data.CONTENT_URI, null, where, null, null);
		Map<String, String> result = new HashMap<String, String>();
		if (c != null && c.getCount() != 0) {
			c.moveToFirst();
			result.put(Keys.FIRST_NAME, c.getString(c.getColumnIndex(CommonDataKinds.StructuredName.GIVEN_NAME)));
			result.put(Keys.LAST_NAME, c.getString(c.getColumnIndex(CommonDataKinds.StructuredName.FAMILY_NAME)));
			result.put(Keys.DISPLAY_NAME, c.getString(c.getColumnIndex(CommonDataKinds.StructuredName.DISPLAY_NAME)));
			c.close();
		}
		return result;
	}

	public static Map<String, String> getFirstLastWithPhone(Context context, String phoneNumber){
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
		Cursor c = context.getContentResolver().query(uri,null,null,null,null);

		Map<String, String> r = new HashMap<String, String>();
		String displayName = null;
		if (c != null && c.getCount() != 0){
			c.moveToFirst();
			displayName = c.getString(c.getColumnIndex(PhoneLookup.DISPLAY_NAME));
		}
		c.close();
		return getFirstLastWithDisplayName(displayName);
	}
	
	private static Map<String,String> getFirstLastWithDisplayName(String displayName){
		Map<String, String> r = new HashMap<String, String>();
		if (displayName != null){
			r.put(Keys.DISPLAY_NAME, displayName);

			int spaceI = displayName.indexOf(' ');
			if (spaceI == -1){
				r.put(Keys.FIRST_NAME, displayName);
			} else {
				r.put(Keys.FIRST_NAME, displayName.substring(0, spaceI));
				r.put(Keys.LAST_NAME, displayName.substring(spaceI+1, displayName.length()));
			}
		}
		return r;
	}
	
	private ArrayList<LinkedTreeMap<String,String>> validPhonesWithDisplayName(String displayName){
		String cId = contactIdWithDisplayName(displayName);
		if (cId == null)
			return null;
		
		ArrayList<String>rcIds = rawContactIdsWithContactId(cId);
		return phoneNumbersWithRawContactIds(rcIds);
	}
	
	private String contactIdWithDisplayName(String displayName){
		String selection = Contacts.DISPLAY_NAME + "='" + displayName + "'";
		String[] projection = {Contacts._ID};
		Cursor c = context.getContentResolver().query(Contacts.CONTENT_URI, projection, selection, null, null);
		if (c.getCount() == 0)
			return null;
		
		c.moveToFirst();
		return c.getString(c.getColumnIndex(Contacts._ID));
	}
	
	private ArrayList<String> rawContactIdsWithContactId(String contactId){
		ArrayList<String> r = new ArrayList<String>();
		String selection = RawContacts.CONTACT_ID + "='" + contactId + "'";
		String[] projection = {RawContacts._ID};
		Cursor c = context.getContentResolver().query(RawContacts.CONTENT_URI, projection, selection, null, null);
		Log.i(TAG, "rawContactIds count: " + c.getCount());
		c.moveToFirst();
		do {
			r.add(c.getString(c.getColumnIndex(RawContacts._ID)));
		} while (c.moveToNext());
		Log.i(TAG, "rawContactIds: " + r);
		return r;
	}
	
	private ArrayList<LinkedTreeMap<String, String>> phoneNumbersWithRawContactIds(ArrayList<String>rawContactIds){
		ArrayList <LinkedTreeMap<String, String>> r = new ArrayList <LinkedTreeMap<String,String>>();
		String[] projection = {CommonDataKinds.Phone.NUMBER, CommonDataKinds.Phone.TYPE};
		String selection = Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE +"= '"+ CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";
		for (String rcId : rawContactIds){
//			allDataWithRawContactId(rcId);
			Cursor c = context.getContentResolver().query(Data.CONTENT_URI, projection, selection, new String[]{rcId}, null);
			if (c == null || c.getCount() == 0)
				continue;
			
			Log.i(TAG, "rawCId:" + rcId + " dataCount:" + c.getCount());
			c.moveToFirst();
			do {
				LinkedTreeMap<String, String>e = new LinkedTreeMap<String,String>();
				int pt = c.getInt(c.getColumnIndex(CommonDataKinds.Phone.TYPE));
				String phoneType = (String) CommonDataKinds.Phone.getTypeLabel(context.getResources(), pt, "");
				e.put(Keys.PHONE_TYPE, phoneType);
				e.put(Keys.PHONE_NUMBER, c.getString(c.getColumnIndex(CommonDataKinds.Phone.NUMBER)));
				r.add(e);
			} while (c.moveToNext());
		}
		return r;
	}
	
	private ArrayList<PhoneNumber> validPhoneNumbersWithPhoneNumbers(ArrayList<String> phoneNumbers){
		ArrayList<PhoneNumber> r = new ArrayList<PhoneNumber>();
		return r;
	}
	

	
	//----------------
	// Tests and debug
	//----------------
	public static void allPhones(Context context){
		String where = Data.MIMETYPE + "='" + CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";
		String[] projection = new String[] {CommonDataKinds.Phone.NUMBER};
		Cursor c = context.getContentResolver().query(Data.CONTENT_URI, projection, where, null, null);
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
	}
}
