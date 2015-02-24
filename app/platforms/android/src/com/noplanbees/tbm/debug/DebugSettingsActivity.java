package com.noplanbees.tbm.debug;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.noplanbees.tbm.R;

/**
 * Created by skamenkovych@codeminders.com on 2/20/2015.
 */
public class DebugSettingsActivity extends Activity implements DebugConfig.DebugConfigChangesCallback {

    private EditText serverHost;
    private EditText serverUri;
    private DebugConfig config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug_settings);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        config = DebugConfig.getInstance(this);
        setUpVersion();
        setUpDebugMode();
        setUpSendSms();
        setUpServer();
    }

    @Override
    protected void onStart() {
        super.onStart();
        config.addCallback(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        config.removeCallback(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        config.setCustomServerHost(serverHost.getText().toString());
        config.setCustomServerUri(serverUri.getText().toString());
    }

    private void setUpVersion() {
        String versionName = "";
        String versionCode = "";
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = info.versionName;
            versionCode = String.valueOf(info.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        TextView versionView = (TextView) findViewById(R.id.version_name);
        TextView buildView = (TextView) findViewById(R.id.version_code);
        versionView.setText(versionName);
        buildView.setText(versionCode);
    }

    private void setUpDebugMode() {
        Switch debugMode = (Switch) findViewById(R.id.debug_mode);
        debugMode.setChecked(config.isDebugEnabled());
        debugMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                config.enableDebug(isChecked);
            }
        });
    }

    private void setUpSendSms() {
        Switch debugMode = (Switch) findViewById(R.id.send_sms);
        debugMode.setChecked(config.shouldSendSms());
        debugMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                config.enableSendSms(isChecked);
            }
        });
    }

    private void setUpServer() {
        final LinearLayout serverHostLayout = (LinearLayout) findViewById(R.id.server_host_layout);
        final LinearLayout serverUriLayout = (LinearLayout) findViewById(R.id.server_uri_layout);
        boolean isEnabled = config.shouldUseCustomServer();
        serverHostLayout.setEnabled(isEnabled);
        serverUriLayout.setEnabled(isEnabled);

        serverHost = (EditText) findViewById(R.id.server_host);
        serverUri = (EditText) findViewById(R.id.server_uri);
        serverHost.setText(config.getCustomHost());
        serverUri.setText(config.getCustomUri());
        serverHost.setEnabled(isEnabled);
        serverUri.setEnabled(isEnabled);

        Switch useCustomServer = (Switch) findViewById(R.id.custom_server);
        useCustomServer.setChecked(config.shouldUseCustomServer());
        useCustomServer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                config.useCustomServer(isChecked);
                serverHost.setEnabled(isChecked);
                serverUri.setEnabled(isChecked);
            }
        });
    }

    @Override
    public void onChange() {

    }
}
