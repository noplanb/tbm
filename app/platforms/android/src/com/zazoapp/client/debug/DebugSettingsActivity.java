package com.zazoapp.client.debug;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.google.i18n.phonenumbers.Phonenumber;
import com.zazoapp.client.R;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.UserFactory;

/**
 * Created by skamenkovych@codeminders.com on 2/20/2015.
 */
public class DebugSettingsActivity extends Activity implements DebugConfig.DebugConfigChangesCallback {

    public static final String EXTRA_SERVER_OPTION = "server_option";

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
        setUpUserInfo();
        setUpSendSms();
        setUpServer();
        setUpCameraOption();
        setUpCrashButton();
        setUpSendBrokenVideo();
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
                findViewById(R.id.crash_button_layout).setVisibility(isChecked ? View.VISIBLE : View.GONE);
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
        boolean serverOptionEnabled = getIntent().getBooleanExtra(EXTRA_SERVER_OPTION, false);
        boolean isEnabled = config.shouldUseCustomServer() && serverOptionEnabled;
        serverHostLayout.setEnabled(isEnabled);
        serverUriLayout.setEnabled(isEnabled);
        serverHostLayout.setVisibility(config.shouldUseCustomServer() ? View.VISIBLE : View.GONE);
        serverUriLayout.setVisibility(config.shouldUseCustomServer() ? View.VISIBLE : View.GONE);

        serverHost = (EditText) findViewById(R.id.server_host);
        serverUri = (EditText) findViewById(R.id.server_uri);
        serverHost.setText(config.getCustomHost());
        serverUri.setText(config.getCustomUri());
        serverHost.setEnabled(isEnabled);
        serverUri.setEnabled(isEnabled);

        Switch useCustomServer = (Switch) findViewById(R.id.custom_server);
        useCustomServer.setEnabled(serverOptionEnabled);
        useCustomServer.setChecked(config.shouldUseCustomServer());
        useCustomServer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                config.useCustomServer(isChecked);
                serverHost.setEnabled(isChecked);
                serverUri.setEnabled(isChecked);
                serverHostLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                serverUriLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void setUpUserInfo() {
        StringBuilder info = new StringBuilder();
        User user = UserFactory.current_user();
        if (user == null || user.getId().isEmpty()) {
            info.append("Not signed in");
        } else {
            info.append(user.getFirstName()).append(", ").append(user.getLastName()).append("\n");
            Phonenumber.PhoneNumber phone = user.getPhoneNumberObj();
            info.append("(").append(phone.getCountryCode()).append(") ").append(phone.getNationalNumber());
        }
        final TextView userInfo = (TextView) findViewById(R.id.user_info);
        userInfo.setText(info.toString());

        findViewById(R.id.user_info_dispatch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dispatch.dispatchUserInfo(DebugSettingsActivity.this);
            }
        });
    }

    private void setUpCameraOption() {
        Switch cameraOption = (Switch) findViewById(R.id.use_rear_camera);
        cameraOption.setChecked(config.shouldUseRearCamera());
        cameraOption.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                config.useRearCamera(isChecked);
            }
        });
    }


    private void setUpCrashButton() {
        Button crashMainButton = (Button) findViewById(R.id.crash_main_button);
        crashMainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                throw new NullPointerException("Main thread crash button: Stop touching me!");
            }
        });

        Button crashThreadButton = (Button) findViewById(R.id.crash_thread_button);
        crashThreadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        throw new NullPointerException("Other thread crash button: Stop touching me!");
                    }
                }.execute((Void[]) null);
            }
        });

        findViewById(R.id.crash_button_layout).setVisibility(config.isDebugEnabled() ? View.VISIBLE : View.GONE);
    }

    private void setUpSendBrokenVideo() {
        Switch sendBrokenVideoOption = (Switch) findViewById(R.id.send_broken_video);
        sendBrokenVideoOption.setChecked(config.shouldSendBrokenVideo());
        sendBrokenVideoOption.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                config.setSendBrokenVideo(isChecked);
            }
        });
    }

    @Override
    public void onChange() {

    }
}
