package com.zazoapp.client.contactsgetter;

import android.app.Activity;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MyActivity extends Activity {
    public static final String TAG = MyActivity.class.getSimpleName();
    public static final String NAME = "Старикова";
    private ListView list;
    private ContactsAdapter adapter;
    private TextView text;
    private int saniId = 1172;// 1940
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        list = (ListView) findViewById(R.id.list);
        text = (TextView) findViewById(R.id.text);
        adapter = new ContactsAdapter();
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                Cursor c = getContentResolver().query(
                        ContactsContract.RawContacts.CONTENT_URI, null, null, null, null);
                StringBuilder builder = new StringBuilder();
                builder.append("\nRawContacts.CONTENT_URI\n");
                Log.i(TAG, builder.toString());
                if (c != null && c.getCount() > 0) {
                    int nameId = c.getColumnIndex(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY);
                    int contactId = c.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID);
                    while (c.moveToNext()) {
                        if (c.getString(nameId) != null && c.getString(nameId).contains(NAME)) {
                            printAllFields(c, builder);
                        } else if (c.getInt(contactId) == saniId) {
                            printAllFields(c, builder);
                        }
                    }
                }
                if (c != null) {
                    c.close();
                }
                builder = new StringBuilder();
                builder.append("\nContacts.CONTENT_URI\n");
                Log.i(TAG, builder.toString());
                c = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
                if (c != null && c.getCount() > 0) {
                    while (c.moveToNext()) {
                        int nameId = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                        int contactId = c.getColumnIndex(ContactsContract.Contacts._ID);
                        if (c.getString(nameId) != null && c.getString(nameId).contains(NAME)) {
                            printAllFields(c, builder);
                        } else if (c.getInt(contactId) == saniId) {
                            printAllFields(c, builder);
                        }
                    }
                }
                if (c != null) {
                    c.close();
                }
                builder = new StringBuilder();
                builder.append("\nCommonDataKinds.Email.CONTENT_URI\n");
                Log.i(TAG, builder.toString());
                c = getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, null, null, null);
                if (c != null && c.getCount() > 0) {
                    while (c.moveToNext()) {
                        int index = c.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME);
                        int contactId = c.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID);
                        if (c.getString(index) != null && c.getString(index).contains(NAME)) {
                            printAllFields(c, builder);
                        } else if (c.getInt(contactId) == saniId) {
                            printAllFields(c, builder);
                        }
                    }
                }
                if (c != null) {
                    c.close();
                }
                builder = new StringBuilder();
                builder.append("\nCommonDataKinds.Phone.CONTENT_URI\n");
                Log.i(TAG, builder.toString());
                c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
                if (c != null && c.getCount() > 0) {
                    while (c.moveToNext()) {
                        int index = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                        int contactId = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
                        if (c.getString(index) != null && c.getString(index).contains(NAME)) {
                            printAllFields(c, builder);
                        } else if (c.getInt(contactId) == saniId) {
                            printAllFields(c, builder);
                        }
                    }
                }
                if (c != null) {
                    c.close();
                }
                return builder.toString();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                text.setText(s);
            }
        }.execute(new Void[1]);
    }

    private void printAllFields(Cursor c, StringBuilder builder) {
        builder = new StringBuilder();
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

    private static class ContactsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }
    }
}
