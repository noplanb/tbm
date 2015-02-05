package com.noplanbees.tbm.bench;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.internal.LinkedTreeMap;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.noplanbees.tbm.ContactsManager;
import com.noplanbees.tbm.model.Contact;
import com.noplanbees.tbm.model.UserFactory;
import com.noplanbees.tbm.utilities.AsyncTaskManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class SmsStatsHandler {

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

	public static SmsStatsHandler instance;
	
	public static interface SmsManagerCallback{
		public void didRecieveRankedPhoneData(ArrayList<LinkedTreeMap<String, String>> rankedPhoneData);
	}

	private final String TAG = getClass().getSimpleName();
	private static final String STAG = SmsStatsHandler.class.getSimpleName();

	private Context context;
	private Cursor messagesCursor;
	private LinkedTreeMap<String, Integer> numMessages;

	private LinkedTreeMap<String, String>rawContactIdByPhone;

	private ArrayList<String> rankedPhones;
	private ArrayList<LinkedTreeMap<String, String>> rankedPhoneData;
	
	private SmsManagerCallback delegate;

    private boolean isRequestRunning;
    private boolean isRequestComplete;

	//------------
	// Constructor
	//------------
	private SmsStatsHandler(Context c){
		context = c;
        rankedPhoneData = new ArrayList<>();
	}
    
	// Singleton instance.
	public static SmsStatsHandler getInstance(Context c, SmsManagerCallback d){
		if (instance == null)
			instance = new SmsStatsHandler(c);
        
		instance.setDelegate(d);
		return instance;		
	}
	
    // Single delegate allowed.
	public void setDelegate(SmsManagerCallback d){
		delegate = d;
//		notifyDelegate();
	}
	
	private void notifyDelegate(){
		if (rankedPhoneData != null && delegate != null)
			delegate.didRecieveRankedPhoneData(rankedPhoneData);
	}

	//-------------------------------------------------
	// Ranking phone data by frequency of text messages
	//-------------------------------------------------
	public void getRankedPhoneData(){
        if(!isRequestComplete && !isRequestRunning)
		    AsyncTaskManager.executeAsyncTask(new GetRankedPhoneDataAsync(), new Void[]{});
	}
	
	private class GetRankedPhoneDataAsync extends AsyncTask<Void, Void, Void>{
		@Override
		protected Void doInBackground(Void... params) {
			Log.i(TAG, "GetRankedPhoneDataAsync");
            isRequestRunning = true;
			setMessagesCursor();
			rankPhoneData();
			printRankedPhoneData();
			return null;
		}
		
		// We notify delegate here so it runs in the UI thread which hopefully avoids any unlikely race conditions.
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			notifyDelegate();
            isRequestRunning = false;
            isRequestComplete = true;
		}
	}

	private void setMessagesCursor(){
		messagesCursor = context.getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
		Log.i(TAG, "setMessagesCursor: count=" + messagesCursor.getCount());
	}

	private void rankPhoneData(){
		setNumMessages();
		Log.i(TAG, "numMessages: " + numMessages);
		printColumnNames(messagesCursor);
//		rawContactIdByPhone();
		Log.i(TAG, "rawContactIdByPhone: " + rawContactIdByPhone );
		setRankedPhones();
		Log.i(TAG, "rankedPhones: " + rankedPhones);
		setRankedPhoneData();
		Log.i(TAG, "rankedPhoneData: " + rankedPhoneData);
	}



	private void setNumMessages(){
		if (messagesCursor == null || messagesCursor.getCount() == 0){
            Log.d(TAG, "setNumMessages: Got no SMS messages");
			return;
		}

		numMessages = new LinkedTreeMap<String, Integer>();
		int addrCol = messagesCursor.getColumnIndex(SmsColumnNames.ADDRESS);

		messagesCursor.moveToFirst();
		do {
			String mobileNumber = messagesCursor.getString(addrCol);
			mobileNumber = getValidE164ForNumber(mobileNumber);
			// Ignore if not valid number. Should always be a valid number though since it has received sms.
			if (mobileNumber == null)
				continue;
			
			Integer n = numMessages.get(mobileNumber);
			if (n == null)
				numMessages.put(mobileNumber, 1);
			else
				numMessages.put(mobileNumber, n+1);				
		} while (messagesCursor.moveToNext());
	}
	
	private String getValidE164ForNumber(String phone){
		PhoneNumberUtil pu = PhoneNumberUtil.getInstance();
		String r = null;
		try {
			PhoneNumber pn = pu.parse(phone, UserFactory.current_user().getRegion());
			if (pu.isValidNumber(pn))
				r = pu.format(pn, PhoneNumberUtil.PhoneNumberFormat.E164);
		} catch (NumberParseException e) {
            //Log.e(TAG, "ERROR: found sms number not valid. Not expected to ever happen.");
		}
		return r;
	}

	// Not used we get the contact from the phone number as the person field is not always filled
	// on jills samsung the person field is never filled.
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

			// Map<String, String> firstLast = ContactsManager.getFirstLastWithRawContactId(context, rawContactId);
			// This is more effective on my phone than using the RawContactId in person. It caught amit where the former did not.
			Map<String, String> firstLast = ContactsManager.getFirstLastWithPhone(context, mobileNumber);
			
			if (firstLast == null)
				continue;

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
		SmsStatsHandler.printRankedPhoneData(rankedPhoneData);
	}
	
	public static void printRankedPhoneData(ArrayList<LinkedTreeMap<String, String>> rankedPhoneData){
		Log.i(STAG, "printRankedPhoneData");
		if (rankedPhoneData == null){
			Log.i(STAG, "no Ranked Phone Data");
			return;
		}
		for (LinkedTreeMap<String, String>e : rankedPhoneData){
			Log.i(STAG, e.get(Keys.FIRST_NAME) + "-" + e.get(Keys.LAST_NAME) + " " + e.get(Keys.DISPLAY_NAME) + " " + e.get(Keys.MOBILE_NUMBER) + " " + e.get(Keys.NUM_MESSAGES));
		}
	}

	private void printColumnNames(Cursor c) {
		Log.i(TAG, "printColumnNames: ");
		if (c == null){
			Log.i(TAG, "printColumnNames: got null cursor");
			return;
		}
		
		String s = "";
		for (int i=0; i< c.getColumnCount(); i++){
			s += c.getColumnName(i) + " ";
		}
		Log.i(TAG, s);
		return;
	}

}
