package com.zazoapp.client.contactsgetter;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
                    public void onInfoCollected(final JSONArray contacts) {
                        text.setText(contacts.length() + " contacts has been collected");
                        JSONObject object = new JSONObject();
                        try {
                            object.put("contacts", contacts);
                            new HttpRequest("api/v1/contacts", object, "POST", new HttpRequest.Callbacks() {
                                @Override
                                public void success(final String response) {
                                    text.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            text.setText(response);
                                        }
                                    }, 2000);
                                }

                                @Override
                                public void error(final String errorString) {
                                    text.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            text.setText(errorString);
                                        }
                                    }, 2000);

                                }
                            });
                        } catch (JSONException e) {
                        }
                    }
                });
            }
        });
    }
}
