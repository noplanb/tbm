package com.noplanbees.tbm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.internal.LinkedTreeMap;

public class SmsManager {

	public static class Keys{
		// Conform ot BenchObject keys results of sms manager are typically passed to bench
		public static final String FIRST_NAME = "firstName";
		public static final String LAST_NAME = "lastName";
		public static final String DISPLAY_NAME = "displayName";
		public static final String MOBILE_NUMBER = "mobileNumber";
		public static final String NUM_MESSAGES = "numMessages";
		public static final String RAW_CONTACT_ID = "rawContactId";
	}
	
	public static class SmsColumnNames{
		
//		public static final String person = Sms.Inbox.PERSON;
//	    public static final String address = Sms.Inbox.ADDRESS;
		public static final String PERSON = "person";
	    public static final String ADDRESS = "address";
	}

	public static SmsManager instance;
	
	public static interface SmsManagerCallback{
		public void didRecieveRankedPhoneData(ArrayList<LinkedTreeMap<String, String>> rankedPhoneData);
	}

	private final String TAG = getClass().getSimpleName();
	private static final String STAG = SmsManager.class.getSimpleName();

	private Context context;
	private Cursor messagesCursor;
	private LinkedTreeMap<String, Integer> numMessages;

	private LinkedTreeMap<String, String>rawContactIdByPhone;

	private ArrayList<String> rankedPhones;
	private ArrayList<LinkedTreeMap<String, String>> rankedPhoneData;
	
	private SmsManagerCallback delegate;

	//------------
	// Constructor
	//------------
	public SmsManager(Context c, SmsManagerCallback d){
		instance = this;
		context = c;
		setDelegate(d);
		(new GetRankedPhoneDataAsync()).execute();
	}
    
	// Singleton instance.
	public static SmsManager getInstance(Context c, SmsManagerCallback d){
		if (instance == null)
			instance = new SmsManager(c,d);
        
		instance.setDelegate(d);
		return instance;		
	}
	
    // Single delegate allowed.
	public void setDelegate(SmsManagerCallback d){
		delegate = d;
		notifyDelegate();
	}
	
	private void notifyDelegate(){
		if (rankedPhoneData != null && delegate != null)
			delegate.didRecieveRankedPhoneData(rankedPhoneData);
	}


	//-------------------------------------------------
	// Ranking phone data by frequency of text messages
	//-------------------------------------------------
	private class GetRankedPhoneDataAsync extends AsyncTask<Void, Void, Void>{
		@Override
		protected Void doInBackground(Void... params) {
			setMessagesCursor();
			rankPhoneData();
//			printRankedPhoneData();
			notifyDelegate();
			return null;
		}
		
		// We notify delegate here so it runs in the UI thread which hopefully avoids any unlikely race conditions.
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			notifyDelegate();
		}
	}

	private void setMessagesCursor(){
		messagesCursor = context.getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
	}

	private void rankPhoneData(){
		setNumMessages();
		rawContactIdByPhone();
		setRankedPhones();
		setRankedPhoneData();
	}

	private void setNumMessages(){
		if (messagesCursor == null || messagesCursor.getCount() == 0)
			return;

		numMessages = new LinkedTreeMap<String, Integer>();
		int addrCol = messagesCursor.getColumnIndex(SmsColumnNames.ADDRESS);
		int personCol = messagesCursor.getColumnIndex(SmsColumnNames.PERSON);

		messagesCursor.moveToFirst();
		do {
			String rawContactId = messagesCursor.getString(personCol);
			// Ignore if the phone number isn't associated with a contact.
			if (rawContactId == null)
				continue;

			String mobileNumber = messagesCursor.getString(addrCol);
			Integer n = numMessages.get(mobileNumber);
			if (n == null)
				numMessages.put(mobileNumber, 1);
			else
				numMessages.put(mobileNumber, n+1);				
		} while (messagesCursor.moveToNext());
	}

	private void rawContactIdByPhone(){
		if (messagesCursor == null || messagesCursor.getCount() == 0)
			return;

		rawContactIdByPhone = new LinkedTreeMap<String, String>();

		int addrCol = messagesCursor.getColumnIndex(SmsColumnNames.ADDRESS);
		int persCol = messagesCursor.getColumnIndex(SmsColumnNames.PERSON);

		messagesCursor.moveToFirst();
		do {
			String phone = messagesCursor.getString(addrCol);
			String person = messagesCursor.getString(persCol);
			rawContactIdByPhone.put(phone, person);
		} while (messagesCursor.moveToNext());
	}

	private void setRankedPhones(){
		if (numMessages == null)
			return;

		rankedPhones = new ArrayList<String>(numMessages.keySet());
		Collections.sort(rankedPhones, new PhoneFreqComparator());
	}

	class PhoneFreqComparator implements Comparator<String> {
		public int compare(String p1, String p2) {
			return numMessages.get(p2).compareTo(numMessages.get(p1));
		}
	}

	private void setRankedPhoneData(){
		if (rankedPhones == null)
			return;

		rankedPhoneData = new ArrayList<LinkedTreeMap<String, String>>();
		for(String mobileNumber : rankedPhones){
			LinkedTreeMap<String, String> entry = new LinkedTreeMap<String, String>();
			entry.put(Keys.MOBILE_NUMBER, mobileNumber);
			entry.put(Keys.NUM_MESSAGES, numMessages.get(mobileNumber).toString());
			String rawContactId = rawContactIdByPhone.get(mobileNumber);
			entry.put(Keys.RAW_CONTACT_ID, rawContactId);

			// Map<String, String> firstLast = ContactsManager.getFirstLastWithRawContactId(context, rawContactId);
			// This is more effective on my phone than using the RawContactId in person. It caught amit where the former did not.
			Map<String, String> firstLast = ContactsManager.getFirstLastWithPhone(context, mobileNumber);

			entry.put(Keys.FIRST_NAME, firstLast.get(Contact.ContactKeys.FIRST_NAME));
			entry.put(Keys.LAST_NAME, firstLast.get(Contact.ContactKeys.LAST_NAME));
			entry.put(Keys.DISPLAY_NAME, firstLast.get(Contact.ContactKeys.DISPLAY_NAME));
			rankedPhoneData.add(entry);
		}
	}

	//------
	// Debug
	//------
	public void printRankedPhoneData(){
		SmsManager.printRankedPhoneData(rankedPhoneData);
	}
	
	public static void printRankedPhoneData(ArrayList<LinkedTreeMap<String, String>> rankedPhoneData){
		Log.i(STAG, "printRankedPhoneData");
		for (LinkedTreeMap<String, String>e : rankedPhoneData){
			Log.i(STAG, e.get(Keys.FIRST_NAME) + "-" + e.get(Keys.LAST_NAME) + " " + e.get(Keys.DISPLAY_NAME) + " " + e.get(Keys.MOBILE_NUMBER) + " " + e.get(Keys.NUM_MESSAGES));
		}
	}


}
