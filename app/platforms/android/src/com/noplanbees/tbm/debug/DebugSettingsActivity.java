package com.noplanbees.tbm.debug;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import com.noplanbees.tbm.R;

/**
 * Created by skamenkovych@codeminders.com on 2/20/2015.
 */
public class DebugSettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug_settings);
        setUpVersion();
        setUpDebugMode();
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
        final DebugConfig config = DebugConfig.getInstance(this);
        debugMode.setChecked(config.isDebugEnabled());
        debugMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                config.enableDebug(isChecked);
            }
        });
    }
}
