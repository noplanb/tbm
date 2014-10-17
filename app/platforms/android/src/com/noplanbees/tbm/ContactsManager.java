package com.noplanbees.tbm;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;

public class ContactsManager {

	private final static String TAG = ContactsManager.class.getSimpleName();

	public static class Keys{
		public static final String FIRST_NAME = "firstName";
		public static final String LAST_NAME = "lastName";
		public static final String DISPLAY_NAME = "displayName";
	}

	//	public ContactsManager(Context c){
	//		context = c;
	//		String[] ids = {"6208","6201","5830","6216","6116"};
	//		for (String id : ids){
	//			Log.i(TAG, getFirstLast(id).toString());
	//		}
	//	}


	public static Map<String, String> getFirstLastWithRawContactId(Context context, String rawContactId){
		Log.i(TAG, "getFirstLast: rawContactId:" + rawContactId);

		String where = ContactsContract.Data.RAW_CONTACT_ID + "=" + rawContactId + " AND " + 
				ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE + "'";
		Cursor c = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, null, where, null, null);
		Map<String, String> result = new HashMap<String, String>();
		if (c != null && c.getCount() != 0) {
			c.moveToFirst();
			result.put(Keys.FIRST_NAME, c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)));
			result.put(Keys.LAST_NAME, c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME)));
			result.put(Keys.DISPLAY_NAME, c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME)));
			c.close();
		}
		return result;
	}

	public static Map<String, String> getFirstLastWithPhone(Context context, String phoneNumber){
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
		Cursor c = context.getContentResolver().query(uri,null,null,null,null);

		Map<String, String> r = new HashMap<String, String>();
		if (c != null && c.getCount() != 0){
			c.moveToFirst();
			String displayName = c.getString(c.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));

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
		}
		c.close();
		return r;
	}
}
