package com.zazoapp.client.contactsgetter;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import org.json.JSONArray;

public class MyActivity extends Activity {
    public static final String TAG = MyActivity.class.getSimpleName();
    //public static final String NAME = "Старикова";
    private ListView list;
    private TextView text;
    //private int saniId = 1172;// 1940
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        text = (TextView) findViewById(R.id.text);
        findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContactsInfoCollector.collectContacts(getContentResolver(), new ContactsInfoCollector.ContactsInfoCollectedCallback() {
                    @Override
                    public void onInfoCollected(JSONArray contacts) {
                        text.setText(contacts.length() + " contacts has been collected");
                    }
                });
            }
        });
    }
}
