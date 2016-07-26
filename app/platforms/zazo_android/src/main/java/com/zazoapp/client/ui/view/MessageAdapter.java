package com.zazoapp.client.ui.view;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.zazoapp.client.R;
import com.zazoapp.client.asr.ASRProvider;
import com.zazoapp.client.asr.NuanceASRProvider;
import com.zazoapp.client.asr.VoiceTranscriptor;
import com.zazoapp.client.core.MessageType;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.IncomingMessage;
import com.zazoapp.client.model.Transcription;
import com.zazoapp.client.multimedia.VideoIdUtils;
import com.zazoapp.client.utilities.StringUtils;

import java.io.File;
import java.util.HashSet;
import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 7/4/2016.
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private LayoutInflater layoutInflater;
    private Context context;
    private List<IncomingMessage> messages;
    private ASRProvider asrProvider = new NuanceASRProvider();

    private static final String TAG = MessageAdapter.class.getSimpleName();

    private static final int VIDEO_TRANSCRIPTION = 0;

    private HashSet<String> submittedRequests = new HashSet<>();

    public MessageAdapter(List<IncomingMessage> list, Context context) {
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.messages = list;
        this.context = context.getApplicationContext();
        for (int i = 0; i < list.size(); i++) {
            IncomingMessage video = list.get(i);
            Transcription transcription = video.getTranscription();
            if (Transcription.State.NONE.equals(transcription.state)) {
                checkAndRequestTranscription(i, video);
            }
            submittedRequests.add(video.getId());
        }
        setHasStableIds(true);
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIDEO_TRANSCRIPTION:
                View v = layoutInflater.inflate(R.layout.transcription_item, parent, false);
                return new TranscriptionViewHolder(v);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, final int position) {
        switch (holder.getType()) {
            case VIDEO:
                final TranscriptionViewHolder h = (TranscriptionViewHolder) holder;
                final IncomingMessage video = messages.get(position);
                final Transcription transcription = video.getTranscription();
                switch (transcription.state) {
                    case Transcription.State.NONE:
                        h.setInMode(TranscriptionViewHolder.ViewMode.PROGRESS, null, null);
                        h.setOnClickListener(null);
                        break;
                    case Transcription.State.OK:
                        h.setInMode(TranscriptionViewHolder.ViewMode.MESSAGE, transcription.text, StringUtils.getEventTime(video.getId()));
                        h.setOnClickListener(null);
                        break;
                    case Transcription.State.FAILED:
                        // TODO change text
                        h.setInMode(TranscriptionViewHolder.ViewMode.STATUS, context.getString(R.string.dialog_action_try_again), null);
                        h.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                transcription.state = Transcription.State.NONE;
                                video.setTranscription(transcription);
                                checkAndRequestTranscription(position, video);
                                notifyItemChanged(position);
                            }
                        });
                        break;
                }
                break;
        }
    }

    private void checkAndRequestTranscription(int position, IncomingMessage video) {
        Friend friend = FriendFactory.getFactoryInstance().find(video.getFriendId());
        if (friend != null) {
            File transcriptionFile = new File(friend.audioFromPath(video.getId()));
            if (transcriptionFile.exists()) {
                requestTranscriptionForVideo(position, video, transcriptionFile.getAbsolutePath());
            } else {
                requestTranscriptionForVideo(position, video);
            }
        }
    }

    private void requestTranscriptionForVideo(final int position, final IncomingMessage video) {
        VoiceTranscriptor transcriptor = new VoiceTranscriptor();
        Friend friend = FriendFactory.getFactoryInstance().find(video.getFriendId());
        transcriptor.extractVoiceFromVideo(friend.videoFromPath(video.getId()), asrProvider, new VoiceTranscriptor.ExtractionCallbacks() {
            private long startTime = System.nanoTime();
            @Override
            public void onResult(VoiceTranscriptor transcriptor, String path) {
                float extractionDuration = (System.nanoTime() - startTime) / 1000000000f;
                Log.i(TAG, "extractionDuration " + extractionDuration);
                requestTranscriptionForVideo(position, video, path);
            }

            @Override
            public void onError(String error) {
                Transcription t = new Transcription();
                t.state = Transcription.State.FAILED;
                video.setTranscription(t);
                notifyItemChanged(position);
            }

            @Override
            public void onProgressChanged(int progress) {
            }
        }, null);
    }

    private void requestTranscriptionForVideo(final int position, final IncomingMessage video, String path) {
        VoiceTranscriptor transcriptor = new VoiceTranscriptor();
        transcriptor.requestTranscription(path, asrProvider, new ASRProvider.Callback() {
            private long startTime;
            @Override
            public void onResult(String text) {
                float requestDuration = (System.nanoTime() - startTime) / 1000000000f;
                Log.i(TAG, "requestDuration " + requestDuration + " " + video.getId());
                Transcription t = new Transcription();
                if (text == null) {
                    t.state = Transcription.State.FAILED;
                } else {
                    t.state = Transcription.State.OK;
                    t.text = text;
                    t.asr = "nuance";
                    t.lang = asrProvider.getLanguage();
                    t.rate = String.valueOf(asrProvider.getSampleRate());
                }
                video.setTranscription(t);
                int pos = findPosition(video);
                if (pos >= 0) {
                    notifyItemChanged(pos);
                }
            }

            @Override
            public void onStart() {
                startTime = System.nanoTime();
            }
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public long getItemId(int position) {
        return VideoIdUtils.timeStampFromVideoId(messages.get(position).getId()); // for smooth animation of still unviewed messages
    }

    public void setList(List<IncomingMessage> list) {
        this.messages = list;
        for (int i = 0; i < list.size(); i++) {
            IncomingMessage video = list.get(i);
            if (!submittedRequests.contains(video.getId())) {
                Transcription transcription = video.getTranscription();
                if (Transcription.State.NONE.equals(transcription.state)) {
                    checkAndRequestTranscription(i, video);
                }
            }
        }
        submittedRequests.clear();
        for (IncomingMessage video : list) {
            submittedRequests.add(video.getId());
        }
    }

    private int findPosition(IncomingMessage video) {
        List<IncomingMessage> messagesList = messages;
        for (int i = 0; i < messagesList.size(); i++) {
            if (video.equals(messagesList.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {

        protected MessageType type;

        public MessageViewHolder(View itemView) {
            super(itemView);
        }

        public MessageType getType() {
            return type;
        }
    }

}
