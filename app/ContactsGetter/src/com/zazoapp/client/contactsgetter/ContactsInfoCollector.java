package com.zazoapp.client.contactsgetter;

import android.content.ContentResolver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.util.Log;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.contactsgetter.ContactsManager.SmsColumnNames;
import com.zazoapp.client.contactsgetter.vectors.ContactVector;
import com.zazoapp.client.contactsgetter.vectors.EmailVector;
import com.zazoapp.client.contactsgetter.vectors.MobileVector;
import com.zazoapp.client.contactsgetter.vectors.SNVectorFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by skamenkovych@codeminders.com on 9/16/2015.
 */
public class ContactsInfoCollector {
    private static final String TAG = ContactsInfoCollector.class.getSimpleName();

    public interface ContactsInfoCollectedCallback {
        void onInfoCollected(LinkedTreeMap<Integer, ContactInfo> contacts);
    }

    public static void collectContacts(@NonNull final ContentResolver cr, @NonNull final ContactsInfoCollectedCallback callback) {
        if (cr == null || callback == null) {
            return;
        }
        AsyncTaskManager.executeAsyncTask(false, new AsyncTask<Void, Void, LinkedTreeMap<Integer, ContactInfo>>() {
            @Override
            protected LinkedTreeMap<Integer, ContactInfo> doInBackground(Void... params) {
                LinkedTreeMap<Integer, ContactInfo> contacts = new LinkedTreeMap<>();
                String[] projection = new String[] {
                        ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts._ID, ContactsContract.Contacts.STARRED};
                Cursor c = cr.query(ContactsContract.Contacts.CONTENT_URI, projection, null, null, null);
                if (c != null && c.getCount() > 0) {
                    int nameId = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                    int contactIndex = c.getColumnIndex(ContactsContract.Contacts._ID);
                    int starredIndex = c.getColumnIndex(ContactsContract.Contacts.STARRED);
                    while (c.moveToNext()) {
                        String name = c.getString(nameId);
                        if (name != null) {
                            int id = c.getInt(contactIndex);
                            ContactInfo info = new ContactInfo(id, name);
                            info.setFavorite(c.getInt(starredIndex) == 1);
                            contacts.put(id, info);
                        }
                    }
                }
                if (c != null) {
                    c.close();
                }
                projection = new String[]{
                        ContactsContract.RawContacts.CONTACT_ID, ContactsContract.RawContacts.SOURCE_ID,
                        ContactsContract.RawContacts.SYNC1, ContactsContract.RawContacts.SYNC2,
                        ContactsContract.RawContacts.ACCOUNT_TYPE, ContactsContract.RawContacts.DATA_SET};
                c = cr.query(ContactsContract.RawContacts.CONTENT_URI, projection, null, null, null);
                if (c != null && c.getCount() > 0) {
                    int contactId = c.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID);
                    while (c.moveToNext()) {
                        ContactInfo contact = contacts.get(c.getInt(contactId));
                        if (contact != null) {
                            ContactVector socialNetworkVector = SNVectorFactory.produce(c);
                            if (socialNetworkVector != null) {
                                contact.addVector(socialNetworkVector);
                            }
                        }
                    }
                }
                if (c != null) {
                    c.close();
                }
                projection = new String[] {
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Email.ADDRESS,
                        ContactsContract.CommonDataKinds.Email.TIMES_CONTACTED};
                c = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, null, null, null);
                if (c != null && c.getCount() > 0) {
                    while (c.moveToNext()) {
                        int contactIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID);
                        int addressIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS);
                        int timesContactedIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Email.TIMES_CONTACTED);
                        ContactInfo contact = contacts.get(c.getInt(contactIndex));
                        String address = c.getString(addressIndex);
                        int timesContacted = c.getInt(timesContactedIndex);
                        if (contact != null && address != null) {
                            EmailVector emailVector = new EmailVector(address);
                            emailVector.addParam(EmailVector.ADDS_EMAIL_SENT, timesContacted);
                            contact.addVector(emailVector);
                        }
                    }
                }
                if (c != null) {
                    c.close();
                }
                Map<String, Integer> phoneSmsInfo = getPhoneSmsInfo(cr);
                projection = new String[] {
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.NUMBER};
                c = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null, null);
                if (c != null && c.getCount() > 0) {
                    while (c.moveToNext()) {
                        int contactIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
                        int numberIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                        if (c.getString(numberIndex) != null) {
                            int contact = c.getInt(contactIndex);
                            String number = c.getString(numberIndex);
                            number = ContactsManager.getValidE164ForNumber(number);
                            if (number != null) {
                                ContactVector phoneVector = new MobileVector(number);
                                if (phoneSmsInfo.containsKey(number)) {
                                    phoneVector.addParam(MobileVector.ADDS_SMS_SENT, phoneSmsInfo.get(number));
                                }
                            }
                        }
                    }
                }
                if (c != null) {
                    c.close();
                }
                return contacts;
            }

            @Override
            protected void onPostExecute(LinkedTreeMap<Integer, ContactInfo> s) {
                super.onPostExecute(s);
                for (ContactInfo info : s.values()) {
                    Log.i(TAG, info.toJson());
                }
            }
        });
    }

    private static void printAllFields(Cursor c) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < c.getColumnCount(); i++) {
            builder.append(c.getColumnName(i)).append(": ");
            switch (c.getType(i)) {
                case Cursor.FIELD_TYPE_INTEGER:
                    builder.append(c.getInt(i));
                    break;
                case Cursor.FIELD_TYPE_FLOAT:
                    builder.append(c.getFloat(i));
                    break;
                case Cursor.FIELD_TYPE_STRING:
                    builder.append(c.getString(i));
                    break;
                case Cursor.FIELD_TYPE_BLOB:
                    builder.append("~blob~");
                    break;
                default:
                    builder.append("null");
                    break;
            }
            builder.append("\n");
        }
        builder.append("\n-----------\n");
        Log.i(TAG, builder.toString());
    }

    /**
     * Tries to return messages cursor with address and count of messages columns.
     * If operation is not successful returns null
     * @param cr ContentResolver
     * @return cursor with {@link SmsColumnNames#ADDRESS} and {@link SmsColumnNames#COUNT} columns
     */
    private static Cursor getMessagesCursor(@NonNull ContentResolver cr) {
        final String[] projection = new String[] {
                SmsColumnNames.ADDRESS, "count(" + SmsColumnNames.ADDRESS + ") AS " + SmsColumnNames.COUNT};
        final String where = SmsColumnNames.ADDRESS + " != '') GROUP BY (" + SmsColumnNames.ADDRESS;
        final String orderBy = SmsColumnNames.COUNT + " DESC";
        final Uri sentBox = Uri.parse("content://sms/sent");
        Cursor cursor = null;
        try {
            cursor = cr.query(sentBox, projection, where, null, orderBy);
        } catch (SQLiteException e) {
            // an issue on custom firmware on some devices like OnePlus One
            final String customWhere = SmsColumnNames.ADDRESS + " != '')) GROUP BY ((" + SmsColumnNames.ADDRESS;
            try {
                cursor = cr.query(sentBox, projection, customWhere, null, orderBy);
            } catch (SQLiteException e1) {
                Log.e(TAG, e1.toString(), e1);
            }
        }
        return cursor;
    }

    private static Map<String, Integer> getPhoneSmsInfo(@NonNull ContentResolver cr) {
        Map<String, Integer> info = new HashMap<>();
        Cursor c = getMessagesCursor(cr);
        if (c != null && c.moveToFirst()) {
            int phoneIndex = c.getColumnIndex(SmsColumnNames.ADDRESS);
            int countIndex = c.getColumnIndex(SmsColumnNames.COUNT);
            do {
                String mobileNumber = c.getString(phoneIndex);
                int count = c.getInt(countIndex);
                mobileNumber = ContactsManager.getValidE164ForNumber(mobileNumber);
                // Ignore if not valid number. Should always be a valid number though since it has received sms.
                if (mobileNumber == null)
                    continue;
                Integer smsCount = info.get(mobileNumber);
                if (smsCount != null) {
                    info.put(mobileNumber, count + smsCount);
                } else {
                    info.put(mobileNumber, count);
                }
            } while (c.moveToNext());
        }
        if (c != null) {
            c.close();
        }
        return info;
    }
}
