package com.zazoapp.client.contactsgetter;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.google.gson.internal.LinkedTreeMap;

public class MyActivity extends Activity {
    public static final String TAG = MyActivity.class.getSimpleName();
    //public static final String NAME = "Старикова";
    private ListView list;
    private ContactsAdapter adapter;
    private TextView text;
    //private int saniId = 1172;// 1940
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
        ContactsInfoCollector.collectContacts(getContentResolver(), new ContactsInfoCollector.ContactsInfoCollectedCallback() {
            @Override
            public void onInfoCollected(LinkedTreeMap<Integer, ContactInfo> contacts) {

            }
        });
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
