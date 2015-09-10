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
    private Button mDownloadButton;
    private Button mResetRecordsButton;
    private BroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mStatus = (TextView) findViewById(R.id.status);
        mTestLog = (TextView) findViewById(R.id.test_log);
        mStopButton = (Button) findViewById(R.id.stop_service);
        mDownloadButton = (Button) findViewById(R.id.download);
        mResetRecordsButton = (Button) findViewById(R.id.reset_stats);
        mReceiver = new ManagerServiceReceiver();
        mIntentFilter = new IntentFilter(ManagerService.ACTION_ON_START);
        mIntentFilter.addAction(ManagerService.ACTION_ON_STOP);
        mIntentFilter.addAction(ManagerService.ACTION_ON_FINISHED);
        mIntentFilter.addAction(ManagerService.ACTION_ON_INFO_UPDATED);
        mStopButton.setOnClickListener(this);
        mDownloadButton.setOnClickListener(this);
        mResetRecordsButton.setOnClickListener(this);
        handleIntent(getIntent());
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            if (ManagerService.ACTION_STOP.equals(intent.getAction())) {
                startService(ManagerService.ACTION_STOP);
            }
        }
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
                } else {
                    ManagerService.isStopped = false;
                    startService(ManagerService.ACTION_START);
                }
                break;
            case R.id.reset_service:
                startService(ManagerService.ACTION_RESET);
                break;
            case R.id.download:
                startService(ManagerService.ACTION_DOWNLOAD);
                break;
            case R.id.reset_stats:
                startService(ManagerService.ACTION_RESET_STATS);
                break;
        }
    }

    private class ManagerServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ManagerService.ACTION_ON_START.equals(action)) {
                mStopButton.setText("Stop");
                mResetRecordsButton.setEnabled(false);
                startService(ManagerService.ACTION_UPDATE_INFO);
            } else if (ManagerService.ACTION_ON_STOP.equals(action)) {
                mStopButton.setText("Start");
                mResetRecordsButton.setEnabled(true);
            } else if (ManagerService.ACTION_ON_FINISHED.equals(action)) {
                if (intent.hasExtra(ManagerService.EXTRA_FILES_LIST)) {
                    ArrayList<String> list = intent.getStringArrayListExtra(ManagerService.EXTRA_FILES_LIST);
                    StringBuilder builder = new StringBuilder();
                    mTestLog.setText(builder.toString());
                }
            } else if (ManagerService.ACTION_ON_INFO_UPDATED.equals(action)) {
                if (intent.hasExtra(ManagerService.EXTRA_INFO)) {
                    TestInfo info = intent.getParcelableExtra(ManagerService.EXTRA_INFO);
                    mStatus.setText((!ManagerService.isStopped ? "Started" : "Stopped") + "\n" + info.toString());
                    mStopButton.setText(ManagerService.isStopped ? "Start" : "Stop");
                    mResetRecordsButton.setEnabled(ManagerService.isStopped);
                }
                if (intent.hasExtra("services")) {
                    mTestLog.setText(intent.getStringExtra("services"));
                }
            }
        }
    }

    private void startService(String action) {
        Intent managerIntent = new Intent(this, ManagerService.class);
        managerIntent.setAction(action);
        startService(managerIntent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }
}
