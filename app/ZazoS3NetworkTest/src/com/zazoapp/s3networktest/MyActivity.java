package com.zazoapp.s3networktest;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class MyActivity extends Activity implements View.OnClickListener {
    private TextView mStatus;
    private TextView mTestLog;
    private Button mStopButton;
    private BroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mStatus = (TextView) findViewById(R.id.status);
        mTestLog = (TextView) findViewById(R.id.test_log);
        mStopButton = (Button) findViewById(R.id.stop_service);
        mReceiver = new ManagerServiceReceiver();
        mIntentFilter = new IntentFilter(ManagerService.ACTION_ON_START);
        mIntentFilter.addAction(ManagerService.ACTION_ON_STOP);
        mIntentFilter.addAction(ManagerService.ACTION_ON_FINISHED);
        mStopButton.setOnClickListener(this);
        startService(ManagerService.ACTION_START);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.stop_service:
                if (mStopButton.getText().toString().equals("Stop")) {
                    startService(ManagerService.ACTION_STOP);
                    mStopButton.setText("Start");
                } else {
                    startService(ManagerService.ACTION_START);
                    mStopButton.setText("Stop");
                }
                break;
            case R.id.reset_service:
                startService(ManagerService.ACTION_RESET);
                break;
        }
    }

    private class ManagerServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ManagerService.ACTION_ON_START.equals(action)) {
                mStatus.setText("Started");
            } else if (ManagerService.ACTION_ON_STOP.equals(action)) {
                mStatus.setText("Stopped");
            } else if (ManagerService.ACTION_ON_FINISHED.equals(action)) {
                if (intent.hasExtra(ManagerService.EXTRA_FILES_LIST)) {
                    ArrayList<String> list = intent.getStringArrayListExtra(ManagerService.EXTRA_FILES_LIST);
                    StringBuilder builder = new StringBuilder();
                    for (String s : list) {
                        builder.append(s);
                        builder.append('\n');
                    }
                    mTestLog.setText(builder.toString());
                }
            }
        }
    }

    private void startService(String action) {
        Intent managerIntent = new Intent(this, ManagerService.class);
        managerIntent.setAction(action);
        startService(managerIntent);
    }
}
