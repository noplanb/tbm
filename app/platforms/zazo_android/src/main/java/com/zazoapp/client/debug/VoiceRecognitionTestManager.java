package com.zazoapp.client.debug;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import com.zazoapp.client.model.Video;
import com.zazoapp.client.multimedia.ThumbnailRetriever;
import com.zazoapp.client.utilities.Convenience;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

/**
 * Created by skamenkovych@codeminders.com on 10/23/2015.
 */
public class VoiceRecognitionTestManager implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private static final String TAG = VoiceRecognitionTestManager.class.getSimpleName();
    private static final String PROVIDER_NUANCE = "nuance";
    private static final String PROVIDER_GOOGLE = "google";

    @InjectView(R.id.video_view_layout) FrameLayout videoViewLayout;
    @InjectView(R.id.video_view) VideoView videoView;
    @InjectView(R.id.thumb) ImageView imThumb;
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
    @InjectView(R.id.friends_selector) Spinner friendsSpinner;

    private ArrayList<IncomingVideo> videos;
    private LinkedTreeMap<String, String> transcriptions = new LinkedTreeMap<>();
    private int videoIndex = -1;
    private String transcriptionsPath;
    private boolean isVideoPrepared = false;
    private Friend selectedFriend;

    public VoiceRecognitionTestManager(View root) {
        ButterKnife.inject(this, root);
        init();
    }

    private void init() {
        videos = IncomingVideoFactory.getFactoryInstance().all();
        if (!videos.isEmpty()) {
            videoIndex = 0;
        }
        Collections.sort(videos, new Video.VideoTimestampReverseComparator<IncomingVideo>());
        loadTranscriptions();
        loadFriends();
        setVideo();
    }

    private void loadFriends() {
        friendsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int friendId = (int) parent.getAdapter().getItemId(position);
                if (friendId <= 0) {
                    selectedFriend = null;
                    videos = IncomingVideoFactory.getFactoryInstance().all();
                } else {
                    selectedFriend = FriendFactory.getFactoryInstance().find(String.valueOf(friendId));
                    videos = selectedFriend.getIncomingPlayableVideos();
                }
                Collections.sort(videos, new Video.VideoTimestampReverseComparator<IncomingVideo>());
                videoIndex = 0;
                setVideo();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedFriend = null;
                videos = IncomingVideoFactory.getFactoryInstance().all();
                videoIndex = 0;
                setVideo();
            }
        });
        friendsSpinner.setAdapter(new FriendsSpinnerAdapter());

    }

    private void loadTranscriptions() {
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
    }

    private void setVideo() {
        if (videoIndex < 0 || videoIndex >= videos.size()) {
            setNoVideos();
            return;
        }
        IncomingVideo video = videos.get(videoIndex);
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
            videoIndex = (videoIndex + 1) % videos.size();
            video = videos.get(videoIndex);
            friend = FriendFactory.getFactoryInstance().find(video.getFriendId());
            videoFile = friend.videoFromFile(video.getId());
        }
        tvName.setText(video.getId());
        tvDuration.setText("");
        tvFrom.setText(friend.getFullName() + " (" + friend.get(Friend.Attributes.MOBILE_NUMBER) + ")");
        tvGoogle.setText(getTranscription(video.getId(), PROVIDER_GOOGLE));
        tvNuance.setText(getTranscription(video.getId(), PROVIDER_NUANCE));
        btnGoogle.setEnabled(true);
        btnNuance.setEnabled(true);
        ThumbnailRetriever retriever = new ThumbnailRetriever();
        try {
            Bitmap thumb = retriever.getThumbnail(friend.videoFromPath(video.getId()));
            imThumb.setImageBitmap(thumb);
        } catch (ThumbnailRetriever.ThumbnailBrokenException e) {
            imThumb.setImageResource(R.drawable.ic_no_pic_z);
        }
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
        videoIndex = ((v.getId() == R.id.previous) ? (videoIndex - 1) : (videoIndex + 1)) % videos.size();
        setVideo();
    }

    @OnClick({R.id.google, R.id.nuance})
    public void onRequest(View v) {
        if (videoIndex < 0 || videoIndex >= videos.size()) {
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
        final IncomingVideo video = videos.get(videoIndex);
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
        }, null);

    }

    @OnClick(R.id.video_view_layout)
    public void onVideoViewClick(View v) {
        if (isVideoPrepared) {
            if (videoView.isPlaying()) {
                videoView.stopPlayback();
                videoView.resume();
                imThumb.setVisibility(View.VISIBLE);
            } else {
                videoView.start();
                imThumb.setVisibility(View.INVISIBLE);
            }
        }
    }

    @OnClick(R.id.all_transcriptions)
    public void onShowAllClick(final View v) {
        final ArrayList<Transcription> list = new ArrayList<>(transcriptions.size());
        final ArrayList<String> videoIds = new ArrayList<>(transcriptions.size());
        for (Map.Entry<String, String> entry : transcriptions.entrySet()) {
            list.add(Transcription.fromJson(entry.getValue()));
            videoIds.add(entry.getKey());
        }
        AlertDialog.Builder trDialog = new AlertDialog.Builder(v.getContext());
        trDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        trDialog.setAdapter(new TranscriptionsAdapter(list), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Transcription tr = list.get(which);
                IncomingVideo video = IncomingVideoFactory.getFactoryInstance().find(videoIds.get(which));
                String strName;
                if (video != null) {
                    strName = FriendFactory.getFactoryInstance().find(video.getFriendId()).getDisplayName() + " " + video.getId();
                } else {
                    strName = video.getId();
                }
                AlertDialog.Builder builderInner = new AlertDialog.Builder(v.getContext(), AlertDialog.THEME_DEVICE_DEFAULT_DARK);
                builderInner.setMessage("Google:\n" + tr.get(PROVIDER_GOOGLE) + "\n\nNuance:\n" + tr.get(PROVIDER_NUANCE));
                builderInner.setTitle(strName);
                builderInner.setPositiveButton(
                        "Ok",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                onShowAllClick(v);
                            }
                        });
                builderInner.show();
            }
        });
        trDialog.show();
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
        imThumb.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        videoView.stopPlayback();
        imThumb.setVisibility(View.GONE);
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
        tvDuration.setText("");
        tvFrom.setText("");
        tvGoogle.setText("");
        tvNuance.setText("");
        btnGoogle.setEnabled(false);
        btnNuance.setEnabled(false);
        imThumb.setImageResource(R.drawable.ic_no_pic_z);
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

    static class FriendsSpinnerAdapter extends BaseAdapter {
        ArrayList<Friend> friends = FriendFactory.getFactoryInstance().all();

        FriendsSpinnerAdapter() {
            friends = FriendFactory.getFactoryInstance().all();
            Collections.sort(friends, new Comparator<Friend>() {
                @Override
                public int compare(Friend lhs, Friend rhs) {
                    return lhs.getFullName().compareTo(rhs.getFullName());
                }
            });
        }

        @Override
        public int getCount() {
            return friends.size() + 1;
        }

        @Override
        public Friend getItem(int position) {
            if (position == 0) {
                return null;
            }
            return friends.get(position - 1);
        }

        @Override
        public long getItemId(int position) {
            if (position == 0) {
                return -1;
            }
            return Integer.parseInt(friends.get(position - 1).getId());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder h;
            if (convertView != null) {
                h = (Holder) convertView.getTag();
            } else {
                convertView = View.inflate(parent.getContext(), R.layout.phone_list_item, null);
                h = new Holder(convertView);
            }
            Friend friend = getItem(position);
            if (friend != null) {
                h.name.setText(friend.getFullName());
                h.num.setText("(" + friend.getIncomingPlayableVideos().size() + ")");
            } else {
                h.name.setText("All");
                h.num.setText("(~" + IncomingVideoFactory.getFactoryInstance().count() + ")");
            }

            return convertView;
        }

        static class Holder {
            @InjectView(R.id.left_text) TextView name;
            @InjectView(R.id.right_text) TextView num;

            Holder(View v) {
                ButterKnife.inject(this, v);
                v.setTag(this);
                name.setTextColor(Color.LTGRAY);
                num.setTextColor(Color.LTGRAY);
            }
        }
    }

    static class TranscriptionsAdapter extends BaseAdapter {
        ArrayList<Transcription> transcriptions;

        TranscriptionsAdapter(ArrayList<Transcription> list) {
            transcriptions = list;
        }

        @Override
        public int getCount() {
            return transcriptions.size();
        }

        @Override
        public Transcription getItem(int position) {
            return transcriptions.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder h;
            if (convertView != null) {
                h = (Holder) convertView.getTag();
            } else {
                convertView = View.inflate(parent.getContext(), R.layout.transcription_debug_item, null);
                h = new Holder(convertView);
            }
            Transcription transcription = getItem(position);
            h.setGoogle(transcription.google);
            h.setNuance(transcription.nuance);

            return convertView;
        }

        static class Holder {
            @InjectView(R.id.text1) TextView google;
            @InjectView(R.id.text2) TextView nuance;

            Holder(View v) {
                ButterKnife.inject(this, v);
                v.setTag(this);
                google.setTextColor(Color.LTGRAY);
                nuance.setTextColor(Color.LTGRAY);
            }

            public void setGoogle(String text) {
                google.setText("G: " + text);
            }

            public void setNuance(String text) {
                nuance.setText("N: " + text + " very long text to test how it will be for such guys like Sani with veeeery long Zazo messages");
            }
        }
    }
}
