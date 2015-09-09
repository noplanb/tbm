package com.zazoapp.SupportedCodecs;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.rollbar.android.Rollbar;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MyActivity extends Activity implements View.OnClickListener {

    private JSONArray mJSONArray;
    private EditText phoneNumber;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Rollbar.init(this, "acec6cd91e9948b090802a0a705ded6e", "codecs", true);
        Button send = (Button) findViewById(R.id.send);
        send.setOnClickListener(this);
        phoneNumber = (EditText) findViewById(R.id.number);
        TextView tw = (TextView) findViewById(R.id.codec_list);
        MediaCodecInfo[] infos = getCodecs();
        if (infos != null) {
            mJSONArray = new JSONArray();

            StringBuilder builder = new StringBuilder();

            MediaFormat qvgaPFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 240, 320);
            MediaFormat qvgaLFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 320, 240);
            MediaFormat audioFormat2 = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 48000, 2);
            MediaFormat audioFormat1 = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 48000, 1);
            JSONObject device = new JSONObject();
            try {
                device.put("device", String.format("%s %s", Build.BRAND, Build.MODEL));
                device.put("os_version", Build.VERSION.RELEASE);
            } catch (JSONException e) {
            }
            mJSONArray.put(device);
            for (MediaCodecInfo info : infos) {
                builder.append(info.getName()).append("\n");
                JSONObject json = new JSONObject();
                try {
                    json.put("name", info.getName());
                    json.put("type", info.isEncoder() ? "enc" : "dec");
                    JSONArray supported = new JSONArray();
                    for (String type : info.getSupportedTypes()) {
                        JSONObject jsonTypeCaps = new JSONObject();
                        MediaCodecInfo.CodecCapabilities caps = info.getCapabilitiesForType(type);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            jsonTypeCaps.put("mime", caps.getMimeType());
                            if (caps.getVideoCapabilities() != null) {
                                jsonTypeCaps.put("QVGA-15", caps.getVideoCapabilities().areSizeAndRateSupported(240, 320, 15));
                                jsonTypeCaps.put("QVGA-30", caps.getVideoCapabilities().areSizeAndRateSupported(240, 320, 30));
                                jsonTypeCaps.put("BitRange", caps.getVideoCapabilities().getBitrateRange().toString());
                                jsonTypeCaps.put("FrameRates", caps.getVideoCapabilities().getSupportedFrameRates().toString());
                                jsonTypeCaps.put("Heights", caps.getVideoCapabilities().getSupportedHeights().toString());
                                jsonTypeCaps.put("Widths", caps.getVideoCapabilities().getSupportedWidths().toString());
                                jsonTypeCaps.put("AVC_QVGAP", caps.isFormatSupported(qvgaPFormat));
                                jsonTypeCaps.put("AVC_QVGAL", caps.isFormatSupported(qvgaLFormat));
                            }
                            if (caps.getAudioCapabilities() != null) {
                                jsonTypeCaps.put("48Kbps", caps.getAudioCapabilities().isSampleRateSupported(48000));
                                jsonTypeCaps.put("BitRange", caps.getAudioCapabilities().getBitrateRange().toString());
                                jsonTypeCaps.put("SampleRates", Arrays.toString(
                                        caps.getAudioCapabilities().getSupportedSampleRateRanges()));
                                jsonTypeCaps.put("AAC_48_2", caps.isFormatSupported(audioFormat2));
                                jsonTypeCaps.put("AAC_48_1", caps.isFormatSupported(audioFormat1));
                            }
                        } else {
                            jsonTypeCaps.put("mimetype", type);
                            JSONArray profJson = new JSONArray();
                            if (caps.profileLevels != null) {
                                for (MediaCodecInfo.CodecProfileLevel pl : caps.profileLevels) {
                                    profJson.put(String.format("profile: %X, level %X", pl.profile, pl.level));
                                }
                            }
                            jsonTypeCaps.put("profiles", profJson);
                        }
                        supported.put(jsonTypeCaps);
                    }
                    json.put("supported", supported);
                } catch (JSONException e) {
                }
                mJSONArray.put(json);
            }
            tw.setText(builder.toString());
        }
    }

    @Override
    public void onClick(View v) {
        if (mJSONArray != null && mJSONArray.length() > 0 && !TextUtils.isEmpty(phoneNumber.getText().toString())) {
            try {
                JSONObject numberObject = new JSONObject();
                numberObject.put("number", phoneNumber.getText().toString());
                mJSONArray.put(0, numberObject);
                Rollbar.setPersonData("1", Build.MODEL, phoneNumber.getText().toString());
                Map<String, String> params = new HashMap<>();
                params.put("codecs", mJSONArray.toString(2));
                Rollbar.reportMessage("Codecs", "info", params);
            } catch (JSONException e) {
            }
        } else if (TextUtils.isEmpty(phoneNumber.getText().toString())) {
            Toast.makeText(this, R.string.enter_number_first, Toast.LENGTH_LONG).show();
        }
    }

    private static MediaCodecInfo[] getCodecs() {
        int numCodecs = MediaCodecList.getCodecCount();
        if (numCodecs <= 0) {
            return null;
        }
        MediaCodecInfo[] infos = new MediaCodecInfo[numCodecs];
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            infos[i] = codecInfo;
        }
        return infos;
    }

    public static final String PHONE_NUMBER_KEY = "phone_number_key";
    public static final String MESSAGE_KEY = "message_key";
    public static final String EMAIL_KEY = "email_key";
    public static final String SUBJECT_KEY = "subject_key";
    public Intent getIntent(Bundle bundle) {
        Intent i = new Intent(Intent.ACTION_SENDTO);
        String email = "mailto:" + bundle.getString(EMAIL_KEY)
                + "?subject=" + Uri.encode(bundle.getString(SUBJECT_KEY))
                + "&body=" + Uri.encode(bundle.getString(MESSAGE_KEY));
        i.setData(Uri.parse(email));
        return i;
    }
}
