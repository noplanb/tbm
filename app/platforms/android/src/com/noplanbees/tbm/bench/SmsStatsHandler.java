package com.noplanbees.tbm.bench;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Telephony;
import android.util.Log;
import com.google.gson.internal.LinkedTreeMap;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.noplanbees.tbm.ContactsManager;
import com.noplanbees.tbm.model.Contact;
import com.noplanbees.tbm.model.UserFactory;
import com.noplanbees.tbm.utilities.AsyncTaskManager;
import com.noplanbees.tbm.utilities.Logger;

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

    public static class SmsColumnNames {
        public static final String PERSON = Telephony.Sms.PERSON;
        public static final String ADDRESS = Telephony.Sms.ADDRESS;
        public static final String COUNT = "messages_count";
    }

	public static interface SmsManagerCallback{
		public void didRecieveRankedPhoneData(ArrayList<LinkedTreeMap<String, String>> rankedPhoneData);
	}

	private static final String TAG = SmsStatsHandler.class.getSimpleName();

	private Context context;
	private Cursor messagesCursor;
	private LinkedTreeMap<String, Integer> numMessages;

	private ArrayList<String> rankedPhones;
	private ArrayList<LinkedTreeMap<String, String>> rankedPhoneData;

	private SmsManagerCallback delegate;

    private volatile boolean isRequestRunning;
    private volatile boolean isRequestComplete;

    //------------
	// Constructor
	//------------
	public SmsStatsHandler(Context c) {
		context = c.getApplicationContext();
        rankedPhoneData = new ArrayList<>();
	}

	public void setListener(SmsManagerCallback d){
		delegate = d;
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
        final String[] projection = new String[] {SmsColumnNames.ADDRESS, "count(" + SmsColumnNames.ADDRESS + ") AS " + SmsColumnNames.COUNT};
        final String where = SmsColumnNames.ADDRESS + " != '') GROUP BY (" + SmsColumnNames.ADDRESS;
        final String orderBy = SmsColumnNames.COUNT + " DESC";
		messagesCursor = context.getContentResolver().query(Uri.parse("content://sms/inbox"), projection, where, null, orderBy);
		Log.i(TAG, "setMessagesContacts: count=" + messagesCursor.getCount());
	}

	private void rankPhoneData(){
		setNumMessages();
        Logger.d(TAG, "numMessages: " + numMessages);
		setRankedPhoneData();
        Logger.d(TAG, "rankedPhoneData: " + rankedPhoneData);
	}

	private void setNumMessages(){
		if (messagesCursor == null || messagesCursor.getCount() == 0){
            Log.d(TAG, "setNumMessages: Got no SMS messages");
			return;
		}

		numMessages = new LinkedTreeMap<String, Integer>();
		int addrCol = messagesCursor.getColumnIndex(SmsColumnNames.ADDRESS);
        int countCol = messagesCursor.getColumnIndex(SmsColumnNames.COUNT);
		messagesCursor.moveToFirst();
		do {
			String mobileNumber = messagesCursor.getString(addrCol);
            int count = messagesCursor.getInt(countCol);
			mobileNumber = getValidE164ForNumber(mobileNumber);
			// Ignore if not valid number. Should always be a valid number though since it has received sms.
			if (mobileNumber == null)
				continue;
            numMessages.put(mobileNumber, count);
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
		if (numMessages == null)
			return;

		rankedPhoneData = new ArrayList<LinkedTreeMap<String, String>>();
		for(String mobileNumber : numMessages.keySet()){
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
        Logger.d(TAG, "printRankedPhoneData");
		if (rankedPhoneData == null){
            Logger.d(TAG, "no Ranked Phone Data");
			return;
		}
		for (LinkedTreeMap<String, String>e : rankedPhoneData){
            Logger.d(TAG, e.get(Keys.FIRST_NAME) + "-" + e.get(Keys.LAST_NAME) + " " + e.get(Keys.DISPLAY_NAME) + " " + e.get(Keys.MOBILE_NUMBER) + " " + e.get(Keys.NUM_MESSAGES));
		}
	}

}
