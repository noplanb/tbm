package com.zazoapp.client.debug;

import android.media.MediaPlayer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.R;
import com.zazoapp.client.asr.ASRProvider;
import com.zazoapp.client.asr.GoogleASRProvider;
import com.zazoapp.client.asr.NuanceASRProvider;
import com.zazoapp.client.asr.VoiceTranscriptor;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.IncomingVideo;
import com.zazoapp.client.model.IncomingVideoFactory;
import com.zazoapp.client.utilities.Convenience;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by skamenkovych@codeminders.com on 10/23/2015.
 */
public class VoiceRecognitionTestManager implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private static final String TAG = VoiceRecognitionTestManager.class.getSimpleName();
    private static final String PROVIDER_NUANCE = "nuance";
    private static final String PROVIDER_GOOGLE = "google";

    @InjectView(R.id.video_view_layout) FrameLayout videoViewLayout;
    @InjectView(R.id.video_view) VideoView videoView;
    @InjectView(R.id.previous) Button btnPrevious;
    @InjectView(R.id.next) Button btnNext;
    @InjectView(R.id.name) TextView tvName;
    @InjectView(R.id.duration) TextView tvDuration;
    @InjectView(R.id.from) TextView tvFrom;
    @InjectView(R.id.google) Button btnGoogle;
    @InjectView(R.id.google_progress) ProgressBar pbGoogle;
    @InjectView(R.id.google_transcription) TextView tvGoogle;
    @InjectView(R.id.nuance) Button btnNuance;
    @InjectView(R.id.nuance_progress) ProgressBar pbNuance;
    @InjectView(R.id.nuance_transcription) TextView tvNuance;

    private ArrayList<IncomingVideo> videos;
    private LinkedTreeMap<String, String> transcriptions = new LinkedTreeMap<>();
    private int position = -1;
    private String transcriptionsPath;
    private boolean isVideoPrepared = false;

    public VoiceRecognitionTestManager(View root) {
        ButterKnife.inject(this, root);
        init();
    }

    private void init() {
        videos = IncomingVideoFactory.getFactoryInstance().all();
        if (!videos.isEmpty()) {
            position = 0;
        }
        String json = Convenience.getJsonFromFile(getPath());
        if (json != null) {
            Gson g = new Gson();
            LinkedTreeMap<String, String> all = null;
            try {
                all = g.fromJson(json, LinkedTreeMap.class);
            } catch (JsonSyntaxException e) {
            }

            if (all == null || all.isEmpty()) {
                Log.i(TAG, "Couldn't get transcriptions");
            } else {
                transcriptions = all;
            }
        }
        setVideo();
    }

    private void setVideo() {
        if (position < 0 || position >= videos.size()) {
            setNoVideos();
            return;
        }
        IncomingVideo video = videos.get(position);
        Friend friend = FriendFactory.getFactoryInstance().find(video.getFriendId());
        File videoFile = friend.videoFromFile(video.getId());
        int counter = 0;
        while (!videoFile.exists()) {
            transcriptions.remove(video.getId());
            if (counter >= videos.size()) {
                setNoVideos();
                return;
            }
            counter++;
            position = (position + 1) % videos.size();
            video = videos.get(position);
            friend = FriendFactory.getFactoryInstance().find(video.getFriendId());
            videoFile = friend.videoFromFile(video.getId());
        }
        tvName.setText(video.getId());
        tvDuration.setText("");
        tvFrom.setText(friend.getFullName() + " (" + friend.get(Friend.Attributes.MOBILE_NUMBER) + ")");
        tvGoogle.setText(getTranscription(video.getId(), PROVIDER_GOOGLE));
        tvNuance.setText(getTranscription(video.getId(), PROVIDER_NUANCE));
        isVideoPrepared = false;
        if (videoView.isPlaying()) {
            videoView.stopPlayback();
        }
        videoView.setOnPreparedListener(this);
        videoView.setOnCompletionListener(this);
        videoView.setOnErrorListener(this);
        videoView.setVideoPath(friend.videoFromPath(video.getId()));
    }

    @OnClick({R.id.previous, R.id.next})
    public void onSkip(View v) {
        if (videos.isEmpty()) {
            return;
        }
        position = ((v.getId() == R.id.previous) ? (position - 1) : (position + 1)) % videos.size();
        setVideo();
    }

    @OnClick({R.id.google, R.id.nuance})
    public void onRequest(View v) {
        if (position < 0 || position >= videos.size()) {
            return;
        }
        float limit;
        final String provider;
        final ASRProvider asrProvider;
        final TextView trView;
        final ProgressBar trProgressView;
        switch (v.getId()) {
            case R.id.google:
                limit = GoogleASRProvider.DURATION_LIMIT;
                provider = PROVIDER_GOOGLE;
                asrProvider = new GoogleASRProvider();
                trView = tvGoogle;
                trProgressView = pbGoogle;
                break;
            case R.id.nuance:
            default:
                limit = NuanceASRProvider.DURATION_LIMIT;
                provider = PROVIDER_NUANCE;
                asrProvider = new NuanceASRProvider();
                trView = tvNuance;
                trProgressView = pbNuance;
                break;
        }
        final IncomingVideo video = videos.get(position);
        if (transcriptions.containsKey(video.getId())) {
            Transcription transcription = Transcription.fromJson(transcriptions.get(video.getId()));
            if (transcription.get(provider) != null) {
                return;
            }
        }
        VoiceTranscriptor transcriptor = new VoiceTranscriptor();
        Friend friend = FriendFactory.getFactoryInstance().find(video.getFriendId());
        trProgressView.setIndeterminate(false);
        transcriptor.extractVoiceFromVideo(friend.videoFromPath(video.getId()), limit, new VoiceTranscriptor.ExtractionCallbacks() {
            @Override
            public void onResult(VoiceTranscriptor transcriptor, String path) {
                trProgressView.setIndeterminate(true);
                transcriptor.requestTranscription(path, asrProvider, new ASRProvider.Callback() {
                    @Override
                    public void onResult(String transcription) {
                        if (transcription != null) {
                            Transcription trObject;
                            if (transcriptions.containsKey(video.getId())) {
                                trObject = Transcription.fromJson(transcriptions.get(video.getId()));
                            } else {
                                trObject = new Transcription();
                            }
                            trObject.put(provider, transcription);
                            transcriptions.put(video.getId(), trObject.toJson());
                            trView.setText(transcription);
                            saveTranscriptions();
                        }
                        trProgressView.setIndeterminate(false);
                        trProgressView.setProgress(0);
                    }
                });
            }

            @Override
            public void onError(String error) {
                trProgressView.setProgress(0);
            }

            @Override
            public void onProgressChanged(int progress) {
                trProgressView.setProgress(progress);
            }
        });

    }

    @OnClick(R.id.video_view_layout)
    public void onVideoViewClick(View v) {
        if (isVideoPrepared) {
            if (videoView.isPlaying()) {
                videoView.stopPlayback();
                videoView.resume();
            } else {
                videoView.start();
            }
        }
    }

    public void saveTranscriptions() {
        Gson gson = new Gson();
        String j = gson.toJson(transcriptions);
        Convenience.saveJsonToFile(j, getPath());
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        int millis = mp.getDuration();
        int SSS = millis % 1000;
        millis = millis / 1000;
        int ss = millis % 60;
        millis = millis / 60;
        int mm = millis % 60;
        millis = millis / 60;
        int HH = millis;
        tvDuration.setText(String.format("%02d:%02d:%02d.%03d", HH, mm, ss, SSS));
        isVideoPrepared = true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        videoView.stopPlayback();
        videoView.resume();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        videoView.stopPlayback();
        return true;
    }

    private String getPath() {
        if (transcriptionsPath == null) {
            transcriptionsPath = TbmApplication.getInstance().getCacheDir().getPath() + "/transcriptions.json";
        }
        return transcriptionsPath;
    }

    private void setNoVideos() {
        tvName.setText("No videos");
        btnGoogle.setEnabled(false);
        btnNuance.setEnabled(false);
    }

    static class Transcription {
        String google;
        String nuance;

        String get(String provider) {
            switch (provider) {
                case PROVIDER_NUANCE:
                    return nuance;
                case PROVIDER_GOOGLE:
                    return google;
            }
            return null;
        }

        void put(String provider, String value) {
            switch (provider) {
                case PROVIDER_NUANCE:
                    nuance = value;
                    break;
                case PROVIDER_GOOGLE:
                    google = value;
                    break;
            }
        }

        String toJson() {
            Gson gson = new Gson();
            return gson.toJson(this, this.getClass());
        }

        static Transcription fromJson(String json) {
            Gson gson = new Gson();
            return gson.fromJson(json, Transcription.class);
        }
    }

    private String getTranscription(String videoId, String provider) {
        Transcription transcription = Transcription.fromJson(transcriptions.get(videoId));
        if (transcription != null) {
            String tr = transcription.get(provider);
            if (tr != null) {
                return tr;
            }
        }
        return "";
    }
}
