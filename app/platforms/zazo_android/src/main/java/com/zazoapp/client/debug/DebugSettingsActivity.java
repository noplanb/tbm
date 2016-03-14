package com.zazoapp.client.debug;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.google.i18n.phonenumbers.Phonenumber;
import com.zazoapp.client.Config;
import com.zazoapp.client.R;
import com.zazoapp.client.core.FriendGetter;
import com.zazoapp.client.core.PreferencesHelper;
import com.zazoapp.client.core.RemoteStorageHandler;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.features.friendfinder.ContactsInfoCollector;
import com.zazoapp.client.model.ActiveModelsHandler;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.GridElementFactory;
import com.zazoapp.client.model.GridManager;
import com.zazoapp.client.model.IncomingVideoFactory;
import com.zazoapp.client.model.OutgoingVideoFactory;
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.network.FriendFinderRequests;
import com.zazoapp.client.network.HttpRequest;
import com.zazoapp.client.tutorial.HintType;
import com.zazoapp.client.ui.dialogs.ProgressDialogFragment;
import com.zazoapp.client.utilities.DialogShower;
import org.json.JSONArray;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by skamenkovych@codeminders.com on 2/20/2015.
 */
public class DebugSettingsActivity extends FragmentActivity implements DebugConfig.DebugConfigChangesCallback {

    public static final String EXTRA_FROM_REGISTER_SCREEN = "from_register_screen";

    @InjectView(R.id.server_host) EditText serverHost;
    @InjectView(R.id.server_uri) EditText serverUri;
    @InjectView(R.id.min_room_space) EditText minRoomSpace;
    @InjectView(R.id.debug_mode) Switch debugMode;
    private VoiceRecognitionTestManager voiceRecognitionTestManager;
    private DialogFragment pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug_settings);
        ButterKnife.inject(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setUpVersion();
        setUpDebugMode();
        setUpUserInfo();
        setUpServer();
        setUpCustomizationOptions();
        setUpCrashButton();
        setUpTutorialOption();
        setUpFeatureOptions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        DebugConfig.addCallback(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        DebugConfig.removeCallback(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        DebugConfig.set(DebugConfig.Str.CUSTOM_HOST, serverHost.getText().toString().replace(" ", ""));
        DebugConfig.set(DebugConfig.Str.CUSTOM_URI, serverUri.getText().toString().replace(" ", ""));
        setMinRoomSpace(minRoomSpace.getText());
        if (voiceRecognitionTestManager != null) {
            voiceRecognitionTestManager.saveTranscriptions();
        }
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
        debugMode.setChecked(DebugConfig.isDebugEnabled());
        debugMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DebugConfig.enableDebug(isChecked);
                findViewById(R.id.crash_button_layout).setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void setUpServer() {
        final LinearLayout serverHostLayout = (LinearLayout) findViewById(R.id.server_host_layout);
        final LinearLayout serverUriLayout = (LinearLayout) findViewById(R.id.server_uri_layout);
        final Switch forceSms = (Switch) findViewById(R.id.server_force_sms);
        final Switch forceCall = (Switch) findViewById(R.id.server_force_call);
        boolean serverOptionEnabled = getIntent().getBooleanExtra(EXTRA_FROM_REGISTER_SCREEN, false);
        boolean isEnabled = DebugConfig.Bool.USE_CUSTOM_SERVER.get() && serverOptionEnabled;
        serverHostLayout.setEnabled(isEnabled);
        serverUriLayout.setEnabled(isEnabled);
        forceSms.setEnabled(isEnabled);
        forceCall.setEnabled(isEnabled);
        serverHostLayout.setVisibility(DebugConfig.Bool.USE_CUSTOM_SERVER.get() ? View.VISIBLE : View.GONE);
        serverUriLayout.setVisibility(DebugConfig.Bool.USE_CUSTOM_SERVER.get() ? View.VISIBLE : View.GONE);

        serverHost.setText(DebugConfig.Str.CUSTOM_HOST.get());
        serverUri.setText(DebugConfig.Str.CUSTOM_URI.get());
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
        useCustomServer.setChecked(DebugConfig.Bool.USE_CUSTOM_SERVER.get());
        useCustomServer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DebugConfig.set(DebugConfig.Bool.USE_CUSTOM_SERVER, isChecked);
                serverHost.setEnabled(isChecked);
                serverUri.setEnabled(isChecked);
                forceSms.setEnabled(isChecked);
                forceCall.setEnabled(isChecked);
                forceSms.setChecked(false);
                forceCall.setChecked(false);
                serverHostLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                serverUriLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);

                // Testers request
                if (isChecked) {
                    debugMode.setChecked(true);
                }
            }
        });

        forceSms.setChecked(DebugConfig.Bool.FORCE_CONFIRMATION_SMS.get());
        forceCall.setChecked(DebugConfig.Bool.FORCE_CONFIRMATION_CALL.get());
        final CompoundButton.OnCheckedChangeListener onForceSwitchListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                switch (buttonView.getId()) {
                    case R.id.server_force_sms:
                        if (DebugConfig.Bool.FORCE_CONFIRMATION_SMS.get() != isChecked) {
                            DebugConfig.set(DebugConfig.Bool.FORCE_CONFIRMATION_SMS, isChecked);
                            DebugConfig.set(DebugConfig.Bool.FORCE_CONFIRMATION_CALL, false);
                            forceCall.setEnabled(!isChecked && DebugConfig.Bool.USE_CUSTOM_SERVER.get());
                        }
                        break;
                    case R.id.server_force_call:
                        if (DebugConfig.Bool.FORCE_CONFIRMATION_CALL.get() != isChecked) {
                            DebugConfig.set(DebugConfig.Bool.FORCE_CONFIRMATION_SMS, false);
                            DebugConfig.set(DebugConfig.Bool.FORCE_CONFIRMATION_CALL, isChecked);
                            forceSms.setEnabled(!isChecked && DebugConfig.Bool.USE_CUSTOM_SERVER.get());
                        }
                        break;
                }
                forceSms.setChecked(DebugConfig.Bool.FORCE_CONFIRMATION_SMS.get());
                forceCall.setChecked(DebugConfig.Bool.FORCE_CONFIRMATION_CALL.get());
            }
        };
        forceSms.setOnCheckedChangeListener(onForceSwitchListener);
        forceCall.setOnCheckedChangeListener(onForceSwitchListener);
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

        setUpExtendedLogsDispatch();
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
                    ActiveModelsHandler.getInstance(context).saveAll();
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
                            OutgoingVideoFactory.getFactoryInstance().destroyAll(context);
                            GridElementFactory.getFactoryInstance().destroyAll(context);
                            ActiveModelsHandler.getInstance(context).ensureAll();
                            GridManager.getInstance().initGrid(context);
                            new Features(DebugSettingsActivity.this).lockAll();
                            new ClearFriendGetter(context, true).getFriends();
                        }
                    }
                });
            }
        });
    }

    private void setUpExtendedLogsDispatch() {
        final EditText dispatchDateStart = (EditText) findViewById(R.id.dispatch_logs_date_start);
        final EditText dispatchDateEnd = (EditText) findViewById(R.id.dispatch_logs_date_end);
        final DispatchDateValidator startDateValidator = new DispatchDateValidator(dispatchDateStart);
        final DispatchDateValidator endDateValidator = new DispatchDateValidator(dispatchDateEnd);
        View.OnClickListener pickerListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar cal = Calendar.getInstance(TimeZone.getDefault());
                DispatchDateValidator validator = (v.getId() == R.id.dispatch_logs_date_start) ? startDateValidator : endDateValidator;
                DatePickerDialog datePicker = new DatePickerDialog(DebugSettingsActivity.this, validator,
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH));
                datePicker.setCancelable(false);
                datePicker.setTitle("Select the date");
                datePicker.show();
            }
        };
        dispatchDateStart.setOnClickListener(pickerListener);
        dispatchDateEnd.setOnClickListener(pickerListener);
        findViewById(R.id.dispatch_extended_logs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int days = DebugUtils.dispatchExtendedLogs(dispatchDateStart.getText(), dispatchDateEnd.getText());
                DialogShower.showToast(DebugSettingsActivity.this, "Sending logs from a period of " + days + " day(s)");
            }
        });

    }

    private static class DispatchDateValidator implements DatePickerDialog.OnDateSetListener {

        private EditText editText;
        private SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
        DispatchDateValidator(EditText editText) {
            this.editText = editText;
            setDate(new Date());
        }

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            String year1 = String.valueOf(year);
            String month1 = String.valueOf(monthOfYear + 1);
            String day1 = String.valueOf(dayOfMonth);
            try {
                setDate(format.parse(month1 + "/" + day1 + "/" + year1));
            } catch (ParseException e) {
                setDate(new Date());
            }
        }

        public void setDate(Date date) {
            editText.setText(format.format(date));
        }
    }

    private void setUpCustomizationOptions() {
        Switch cameraOption = (Switch) findViewById(R.id.use_rear_camera);
        cameraOption.setChecked(DebugConfig.Bool.USE_REAR_CAMERA.get());
        cameraOption.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DebugConfig.set(DebugConfig.Bool.USE_REAR_CAMERA, isChecked);
            }
        });

        Switch sendBrokenVideoOption = (Switch) findViewById(R.id.send_broken_video);
        sendBrokenVideoOption.setChecked(DebugConfig.Bool.SEND_BROKEN_VIDEO.get());
        sendBrokenVideoOption.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DebugConfig.set(DebugConfig.Bool.SEND_BROKEN_VIDEO, isChecked);
            }
        });

        Switch debugMode = (Switch) findViewById(R.id.send_sms);
        debugMode.setChecked(DebugConfig.Bool.SEND_SMS.get());
        debugMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DebugConfig.set(DebugConfig.Bool.SEND_SMS, isChecked);
            }
        });

        Switch disableGcm = (Switch) findViewById(R.id.disable_gcm_notifications);
        disableGcm.setChecked(DebugConfig.Bool.DISABLE_GCM_NOTIFICATIONS.get());
        disableGcm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DebugConfig.set(DebugConfig.Bool.DISABLE_GCM_NOTIFICATIONS, isChecked);
            }
        });

        minRoomSpace.setText(String.valueOf(DebugConfig.Int.MIN_ROOM_SPACE_RESTRICTION.get()));
        minRoomSpace.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    setMinRoomSpace(v.getText());
                    minRoomSpace.setText(String.valueOf(DebugConfig.Int.MIN_ROOM_SPACE_RESTRICTION.get()));
                }
                return false;
            }
        });

        Switch sendIncorrectFileSizeSwitch = (Switch) findViewById(R.id.send_incorrect_file_size);
        sendIncorrectFileSizeSwitch.setChecked(DebugConfig.Bool.SEND_INCORRECT_FILE_SIZE.get());
        sendIncorrectFileSizeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DebugConfig.set(DebugConfig.Bool.SEND_INCORRECT_FILE_SIZE, isChecked);
            }
        });

        Switch allowResendSwitch = (Switch) findViewById(R.id.allow_resend);
        allowResendSwitch.setChecked(DebugConfig.Bool.ALLOW_RESEND.get());
        allowResendSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DebugConfig.set(DebugConfig.Bool.ALLOW_RESEND, isChecked);
            }
        });

        Switch allowSendContacts = (Switch) findViewById(R.id.allow_send_contacts);
        allowSendContacts.setChecked(DebugConfig.Bool.ALLOW_SEND_CONTACTS.get());
        allowSendContacts.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DebugConfig.set(DebugConfig.Bool.ALLOW_SEND_CONTACTS, isChecked);
            }
        });

        Button sendContacts = (Button) findViewById(R.id.send_contacts_button);
        sendContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContactsInfoCollector.collectContacts(getContentResolver(),  new ContactsInfoCollector.ContactsInfoCollectedCallback() {
                    @Override
                    public void onInfoCollected(final JSONArray contacts) {
                        FriendFinderRequests.sendContacts(contacts, new HttpRequest.Callbacks() {
                            @Override
                            public void success(String response) {
                                DialogShower.showToast(DebugSettingsActivity.this, "Contacts sent");
                            }

                            @Override
                            public void error(String errorString) {
                                DialogShower.showToast(DebugSettingsActivity.this, errorString);
                            }
                        });
                    }
                });
            }
        });
    }

    private void setMinRoomSpace(CharSequence space) {
        if (!TextUtils.isEmpty(space)) {
            try {
                int value = Integer.parseInt(space.toString());
                DebugConfig.setMinRoomSpace(value);
                if (value != DebugConfig.DEFAULT_MIN_ROOM_SPACE_RESTRICTION) {
                    debugMode.setChecked(true);
                }
            } catch (NumberFormatException e) {
            }
        }
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

        findViewById(R.id.crash_button_layout).setVisibility(DebugConfig.isDebugEnabled() ? View.VISIBLE : View.GONE);
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

    private void setUpFeatureOptions() {
        boolean opened = DebugConfig.Bool.FEATURE_OPTIONS_OPENED.get();
        final Button openBtn = (Button) findViewById(R.id.feature_options_open_btn);
        openBtn.setEnabled(!opened);
        openBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DebugUtils.requestCode(DebugSettingsActivity.this, new DebugUtils.InputDialogCallback() {
                    @Override
                    public void onReceive(String input) {
                        if ("Sani".equalsIgnoreCase(input)) {
                            DebugConfig.set(DebugConfig.Bool.FEATURE_OPTIONS_OPENED, true);
                            openBtn.setEnabled(false);
                            findViewById(R.id.feature_options_layout).setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        });
        findViewById(R.id.feature_options_layout).setVisibility(opened ? View.VISIBLE : View.GONE);

        Switch enableFeatures = (Switch) findViewById(R.id.enable_all_features);
        enableFeatures.setChecked(DebugConfig.Bool.ENABLE_ALL_FEATURES.get());
        enableFeatures.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DebugConfig.set(DebugConfig.Bool.ENABLE_ALL_FEATURES, isChecked);
            }
        });

        findViewById(R.id.manage_features).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ManageFeaturesDialog manageFriendsDialog = new ManageFeaturesDialog();
                DialogShower.showDialog(getSupportFragmentManager(), manageFriendsDialog, "manageFriends");
            }
        });

        findViewById(R.id.delete_user_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RemoteStorageHandler.deleteUserSettings();
            }
        });

        findViewById(R.id.delete_welcomed_friends).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RemoteStorageHandler.deleteWelcomedFriends();
            }
        });
        findViewById(R.id.lock_all_features).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Features features = new Features(DebugSettingsActivity.this);
                for (Features.Feature feature : Features.Feature.values()) {
                    features.lock(feature);
                }
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            View voiceRecognitionRoot = findViewById(R.id.lollipop_specific_features).findViewById(R.id.voice_recognition_layout);
            voiceRecognitionTestManager = new VoiceRecognitionTestManager(voiceRecognitionRoot);
        }
    }

    @Override
    public void onChange() {
    }

    private class ClearFriendGetter extends FriendGetter {
        public ClearFriendGetter(Context c, boolean destroyAll) {
            super(c, destroyAll);
            showProgressDialog();
        }

        @Override
        protected void success() {
            new ClearSyncUserSettings(DebugSettingsActivity.this);
        }

        @Override
        protected void failure() {
            dismissProgressDialog();
            serverError();
        }
    }

    private class ClearSyncWelcomedFriends extends RemoteStorageHandler.GetWelcomedFriends {

        @Override
        protected void gotWelcomedFriends(List<String> mkeys) {
            if (mkeys != null && !mkeys.isEmpty()) {
                List<Friend> friends = FriendFactory.getFactoryInstance().all();
                for (Friend friend : friends) {
                    String mkey = friend.getMkey();
                    if (mkeys.contains(mkey)) {
                        friend.setEverSent(true);
                        mkeys.remove(mkey);
                    } else {
                        friend.setEverSent(false);
                    }
                }
            }
            RemoteStorageHandler.setWelcomedFriends();
            Features features = new Features(DebugSettingsActivity.this);
            features.checkAndUnlock();
            RemoteStorageHandler.setUserSettings();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dismissProgressDialog();
                    finish();
                }
            });
        }

        @Override
        protected void failure() {
            dismissProgressDialog();
            serverError();
        }
    }

    private class ClearSyncUserSettings extends RemoteStorageHandler.GetUserSettings {
        private Features features;

        ClearSyncUserSettings(FragmentActivity a) {
            features = new Features(a);
        }

        @Override
        protected void failure() {
            dismissProgressDialog();
            serverError();
        }

        @Override
        protected void gotUserSettings(@Nullable UserSettings settings) {
            if (settings != null && settings.openedFeatures != null) {
                for (String openedFeature : settings.openedFeatures) {
                    features.unlockByName(openedFeature);
                }
            }
            new ClearSyncWelcomedFriends();
        }
    }

    private void showProgressDialog() {
        dismissProgressDialog();
        pd = ProgressDialogFragment.getInstance(null, getString(R.string.dialog_syncing_title));
        pd.show(getSupportFragmentManager(), null);
    }

    private void dismissProgressDialog() {
        if (pd != null) {
            pd.dismissAllowingStateLoss();
            pd = null;
        }
    }

    private void serverError(){
        showErrorDialog(getString(R.string.dialog_server_error_title),
                getString(R.string.dialog_server_error_message, Config.appName));
    }

    private void showErrorDialog(String title, String message) {
        DialogShower.showInfoDialog(this, title, message);
    }
}
