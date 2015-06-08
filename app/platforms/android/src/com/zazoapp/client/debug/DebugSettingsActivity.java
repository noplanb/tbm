package com.zazoapp.client.debug;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.google.i18n.phonenumbers.Phonenumber;
import com.zazoapp.client.model.GridManager;
import com.zazoapp.client.core.PreferencesHelper;
import com.zazoapp.client.R;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.model.ActiveModelsHandler;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.GridElementFactory;
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.model.IncomingVideoFactory;
import com.zazoapp.client.tutorial.HintType;
import com.zazoapp.client.ui.MainActivity;
import com.zazoapp.client.utilities.DialogShower;

/**
 * Created by skamenkovych@codeminders.com on 2/20/2015.
 */
public class DebugSettingsActivity extends Activity implements DebugConfig.DebugConfigChangesCallback {

    public static final String EXTRA_FROM_REGISTER_SCREEN = "from_register_screen";

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
        setUpTutorialOption();
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
        config.setCustomServerHost(serverHost.getText().toString().replace(" ", ""));
        config.setCustomServerUri(serverUri.getText().toString().replace(" ", ""));
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
        boolean serverOptionEnabled = getIntent().getBooleanExtra(EXTRA_FROM_REGISTER_SCREEN, false);
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
        serverHost.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    serverUri.setText("http://".concat(s.toString()));
                } else {
                    serverUri.setText("");
                }
            }
        });
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

                // Testers request
                if (isChecked) {
                    Switch debugMode = (Switch) findViewById(R.id.debug_mode);
                    debugMode.setChecked(true);
                }
            }
        });
    }

    private void setUpUserInfo() {
        StringBuilder info = new StringBuilder();
        User user = UserFactory.current_user();
        Button clearData = (Button) findViewById(R.id.clear_user_data);
        if (user == null || user.getId().isEmpty()) {
            info.append("Not signed in");
            clearData.setEnabled(false);
        } else {
            info.append(user.getFirstName()).append(", ").append(user.getLastName()).append("\n");
            Phonenumber.PhoneNumber phone = user.getPhoneNumberObj();
            info.append("(").append(phone.getCountryCode()).append(") ").append(phone.getNationalNumber());
            clearData.setEnabled(true);
        }
        final TextView userInfo = (TextView) findViewById(R.id.user_info);
        userInfo.setText(info.toString());

        findViewById(R.id.user_info_dispatch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dispatch.dispatchUserInfo(DebugSettingsActivity.this);
            }
        });
        final boolean restore = getIntent().getBooleanExtra(EXTRA_FROM_REGISTER_SCREEN, false);
        Button backup = (Button) findViewById(R.id.user_info_backup);
        backup.setText(restore ? "Restore" : "Backup");
        backup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (restore) {
                    Context context = DebugSettingsActivity.this;
                    ActiveModelsHandler models = ActiveModelsHandler.getInstance(context);
                    models.destroyAll();
                    DebugUtils.restoreBackup(context);
                    models.ensureAll();
                    GridManager.getInstance().initGrid(context);
                    if (User.isRegistered(context)) {
                        DialogShower.showToast(context, "Loaded");
                        finish();
                    } else {
                        DialogShower.showToast(context, "Nothing to restore");
                    }
                } else {
                    DebugUtils.requestCode(DebugSettingsActivity.this, new DebugUtils.InputDialogCallback() {
                        @Override
                        public void onReceive(String input) {
                            if ("Sani".equalsIgnoreCase(input)) {
                                DebugUtils.makeBackup(DebugSettingsActivity.this);
                            }
                        }
                    });
                }
            }
        });
        clearData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DebugUtils.requestConfirm(DebugSettingsActivity.this, "All user data will be deleted and application will be closed. Continue?", new DebugUtils.InputDialogCallback() {
                    @Override
                    public void onReceive(String input) {
                        if (input != null) {
                            Context context = DebugSettingsActivity.this;
                            FriendFactory.getFactoryInstance().destroyAll(context);
                            IncomingVideoFactory.getFactoryInstance().destroyAll(context);
                            GridElementFactory.getFactoryInstance().destroyAll(context);
                            ActiveModelsHandler.getInstance(context).ensureAll();
                            GridManager.getInstance().initGrid(context);
                            Intent intent = new Intent(context, MainActivity.class);
                            intent.setAction("_FINISH");
                            startActivity(intent);
                            finish();
                        }
                    }
                });
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

    private void setUpTutorialOption() {
        Button resetTutorial = (Button) findViewById(R.id.reset_tutorial);
        resetTutorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PreferencesHelper p = new PreferencesHelper(DebugSettingsActivity.this);
                p.putBoolean(HintType.PLAY.getPrefName(), true);
                p.putBoolean(HintType.RECORD.getPrefName(), true);
                p.putBoolean(HintType.RECORD.getPrefSessionName(), true);
                p.putBoolean(HintType.SENT.getPrefName(), true);
                p.putBoolean(HintType.VIEWED.getPrefName(), true);
                p.putBoolean(HintType.INVITE_2.getPrefSessionName(), true);
            }
        });
    }

    @Override
    public void onChange() {
    }
}
