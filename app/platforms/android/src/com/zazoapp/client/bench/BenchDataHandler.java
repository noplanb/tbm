package com.zazoapp.client.bench;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Log;
import com.google.gson.internal.LinkedTreeMap;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.zazoapp.client.ContactsManager;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.model.Contact;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.utilities.AsyncTaskManager;
import com.zazoapp.client.utilities.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BenchDataHandler {

	public static class Keys{
		// Conform ot BenchObject keys results of sms manager are typically passed to bench
		public static final String FIRST_NAME = "firstName";
		public static final String LAST_NAME = "lastName";
		public static final String DISPLAY_NAME = "displayName";
		public static final String MOBILE_NUMBER = "mobileNumber";
		public static final String NUM_MESSAGES = "numMessages";
		public static final String CONTACT_ID = "id";
	}

    public static class SmsColumnNames {
        public static final String PERSON = Telephony.Sms.PERSON;
        public static final String ADDRESS = Telephony.Sms.ADDRESS;
        public static final String COUNT = "messages_count";
    }

	public static interface BenchDataHandlerCallback {
		public void receivePhoneData(ArrayList<LinkedTreeMap<String, String>> phoneData);
	}

	private static final String TAG = BenchDataHandler.class.getSimpleName();

	private Context context;
	private ArrayList<LinkedTreeMap<String, String>> rankedPhoneData;

	private BenchDataHandlerCallback delegate;

    private volatile boolean isRequestRunning;
    private volatile boolean isRequestComplete;

    //------------
	// Constructor
	//------------
	public BenchDataHandler(Context c) {
		context = c.getApplicationContext();
        rankedPhoneData = new ArrayList<>();
	}

	public void setListener(BenchDataHandlerCallback d){
		delegate = d;
	}

	private void notifyDelegate(){
		if (rankedPhoneData != null && delegate != null)
			delegate.receivePhoneData(rankedPhoneData);
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
            // Issue 101. Add phone data from messages. If it is empty then add all contacts
            if (!addPhoneData(getMessagesPhoneData())) {
                addPhoneData(getContactsPhoneData());
            }
            Logger.d(TAG, "rankedPhoneData: " + rankedPhoneData);
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

    /**
     * Tries to return messages cursor with address and count of messages columns.
     * If operation is not successful returns null
     * @param context context
     * @return cursor with {@link SmsColumnNames#ADDRESS} and {@link SmsColumnNames#COUNT} columns
     */
    private static Cursor getMessagesCursor(Context context) {
        final String[] projection = new String[]{SmsColumnNames.ADDRESS, "count(" + SmsColumnNames.ADDRESS + ") AS " + SmsColumnNames.COUNT};
        final String where = SmsColumnNames.ADDRESS + " != '') GROUP BY (" + SmsColumnNames.ADDRESS;
        final String orderBy = SmsColumnNames.COUNT + " DESC";
        final Uri sentBox = Uri.parse("content://sms/sent");
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(sentBox, projection, where, null, orderBy);
        } catch (SQLiteException e) {
            // an issue on custom firmware on some devices like OnePlus One
            final String customWhere = SmsColumnNames.ADDRESS + " != '')) GROUP BY ((" + SmsColumnNames.ADDRESS;
            try {
                cursor = context.getContentResolver().query(sentBox, projection, customWhere, null, orderBy);
            } catch (SQLiteException e1) {
                // This case must be very specific so just dispatch an error
                // User will see contacts in bench as if he doesn't have sms contacts
                Log.e(TAG, e1.toString(), e1);
                Dispatch.dispatch(e1.getMessage());
            }
        }
        if (cursor != null) {
            Log.i(TAG, "getMessagesContacts: count=" + cursor.getCount());
        }
        return cursor;
    }

    private static Cursor getContactsCursor(Context context) {
        // get only display name of contacts
        String[] projection = new String[] { ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts._ID};
        // for contacts which have phone number
        String where = ContactsContract.Contacts.HAS_PHONE_NUMBER + " > 0";
        //String where = null; // to show all contacts, issue 265: Contact search and contact list
        // when no sms should include all contacts regardless of whether they have a phone

        // Starred contacts first, then other, alphabetically
        String orderBy = ContactsContract.Contacts.STARRED + " DESC, " + ContactsContract.Contacts.DISPLAY_NAME;
        return context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, projection, where, null, orderBy);
    }

    private List<Map<String, String>> getMessagesPhoneData() {
        Cursor cursor = getMessagesCursor(context);
        if (cursor == null || cursor.getCount() == 0) {
            Log.d(TAG, "getMessagesPhoneData: Got no SMS messages");
            if (cursor != null) {
                cursor.close();
            }
            return null;
        }
        // One contact may have address in both e164 and national formats with sent sms so we filter this case on map
        Map<String, String> smsPhones = new LinkedTreeMap<>();
        int addrCol = cursor.getColumnIndex(SmsColumnNames.ADDRESS);
        int countCol = cursor.getColumnIndex(SmsColumnNames.COUNT);
        cursor.moveToFirst();
        do {
            String mobileNumber = cursor.getString(addrCol);
            String count = cursor.getString(countCol);
            mobileNumber = getValidE164ForNumber(mobileNumber);
            // Ignore if not valid number. Should always be a valid number though since it has received sms.
            if (mobileNumber == null)
                continue;
            String currentCount = smsPhones.get(mobileNumber);
            if (currentCount != null) {
                int newCount = Integer.parseInt(currentCount) + Integer.parseInt(count);
                count = String.valueOf(newCount);
            }
            smsPhones.put(mobileNumber, count);

        } while (cursor.moveToNext());
        cursor.close();
        List<Map<String, String>> smsPhoneData = new ArrayList<>();
        for (Map.Entry<String, String> entry : smsPhones.entrySet()) {
            Map<String, String> map = new HashMap<>();
            map.put(Keys.MOBILE_NUMBER, entry.getKey());
            map.put(Keys.NUM_MESSAGES, entry.getValue());
            smsPhoneData.add(map);
        }
        Logger.d(TAG, "numMessages: " + smsPhoneData);
        return smsPhoneData;
    }

    private List<Map<String, String>> getContactsPhoneData() {
        Cursor cursor = getContactsCursor(context);
        if (cursor == null || cursor.getCount() == 0) {
            Log.d(TAG, "getContactsPhoneData: Got no contacts");
            if (cursor != null) {
                cursor.close();
            }
            return null;
        }
        List<Map<String, String>> contactsPhoneData = new ArrayList<>();
        int nameCol = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
        int idCol = cursor.getColumnIndex(ContactsContract.Contacts._ID);
        cursor.moveToFirst();
        do {
            String name = cursor.getString(nameCol);
            String id = cursor.getString(idCol);
            LinkedTreeMap<String, String> map = new LinkedTreeMap<String, String>();
            map.put(Keys.DISPLAY_NAME, name);
            map.put(Keys.CONTACT_ID, id);
            contactsPhoneData.add(map);
        } while (cursor.moveToNext());
        cursor.close();
        return contactsPhoneData;
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

    /**
     *
     * @param phoneData phone number and its number map
     * @return true if data was added
     */
    private boolean addPhoneData(List<Map<String, String>> phoneData){
        if (phoneData == null)
            return false;

        boolean result = false;
        rankedPhoneData = new ArrayList<LinkedTreeMap<String, String>>();
        Map<String, String> firstLast = null;

        for (Map<String, String> data : phoneData) {
            String id = null;
            String mobileNumber = null;
            String numMessage = null;
            if (data.containsKey(Keys.MOBILE_NUMBER)) {
                mobileNumber = data.get(Keys.MOBILE_NUMBER);
                numMessage = data.get(Keys.NUM_MESSAGES);
                firstLast = ContactsManager.getFirstLastWithPhone(context, mobileNumber);
            } else if (data.containsKey(Keys.CONTACT_ID)) {
                id = data.get(Keys.CONTACT_ID);
                numMessage = "0";
                firstLast = ContactsManager.getFirstLastWithDisplayName(data.get(Keys.DISPLAY_NAME));
            }

            if (firstLast == null || (mobileNumber == null && id == null))
                continue; // continue if current phoneData doesn't contain valid contact

            LinkedTreeMap<String, String> entry = new LinkedTreeMap<String, String>();
            entry.put(Keys.MOBILE_NUMBER, mobileNumber);
            entry.put(Keys.NUM_MESSAGES, numMessage);
            entry.put(Keys.CONTACT_ID, id);
            entry.put(Keys.FIRST_NAME, firstLast.get(Contact.ContactKeys.FIRST_NAME));
            entry.put(Keys.LAST_NAME, firstLast.get(Contact.ContactKeys.LAST_NAME));
            entry.put(Keys.DISPLAY_NAME, firstLast.get(Contact.ContactKeys.DISPLAY_NAME));
            rankedPhoneData.add(entry);
            result = true;
        }
        return result;
    }

	//------
	// Debug
	//------
	public void printRankedPhoneData(){
		BenchDataHandler.printRankedPhoneData(rankedPhoneData);
	}
	
	public static void printRankedPhoneData(ArrayList<LinkedTreeMap<String, String>> rankedPhoneData){
        Logger.d(TAG, "printRankedPhoneData");
		if (rankedPhoneData == null){
            Logger.d(TAG, "no Ranked Phone Data");
			return;
		}

        for (LinkedTreeMap<String, String> e : rankedPhoneData) {
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append(e.get(Keys.FIRST_NAME)).append("-").append(e.get(Keys.LAST_NAME)).append(" ");
            logBuilder.append(e.get(Keys.DISPLAY_NAME)).append(" ").append(e.get(Keys.MOBILE_NUMBER)).append(" ");
            logBuilder.append(e.get(Keys.NUM_MESSAGES)).append(" ").append(e.get(Keys.CONTACT_ID));
            Logger.d(TAG, logBuilder.toString());
        }
    }

}
