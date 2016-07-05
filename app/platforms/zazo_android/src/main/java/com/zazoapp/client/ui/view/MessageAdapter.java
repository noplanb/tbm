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
import com.zazoapp.client.model.IncomingVideo;
import com.zazoapp.client.model.Transcription;
import com.zazoapp.client.utilities.StringUtils;

import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 7/4/2016.
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private LayoutInflater layoutInflater;
    private Context context;
    private List<IncomingVideo> messages;

    private static final String TAG = MessageAdapter.class.getSimpleName();

    private static final int VIDEO_TRANSCRIPTION = 0;

    public MessageAdapter(List<IncomingVideo> list, Context context) {
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.messages = list;
        this.context = context.getApplicationContext();
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
                final IncomingVideo video = messages.get(position);
                final Transcription transcription = video.getTranscription();
                switch (transcription.state) {
                    case Transcription.State.NONE:
                        h.setInMode(TranscriptionViewHolder.ViewMode.PROGRESS, null, null);
                        requestTranscriptionForVideo(position, video);
                        break;
                    case Transcription.State.OK:
                        h.setInMode(TranscriptionViewHolder.ViewMode.MESSAGE, transcription.text, StringUtils.getEventTime(video.getId()));
                        break;
                    case Transcription.State.FAILED:
                        // TODO change text
                        h.setInMode(TranscriptionViewHolder.ViewMode.STATUS, context.getString(R.string.dialog_action_try_again), null);
                        break;
                }
                break;
        }
    }

    private void requestTranscriptionForVideo(final int position, final IncomingVideo video) {
        VoiceTranscriptor transcriptor = new VoiceTranscriptor();
        Friend friend = FriendFactory.getFactoryInstance().find(video.getFriendId());
        transcriptor.extractVoiceFromVideo(friend.videoFromPath(video.getId()), NuanceASRProvider.DURATION_LIMIT, new VoiceTranscriptor.ExtractionCallbacks() {
            private long startTime = System.nanoTime();
            @Override
            public void onResult(VoiceTranscriptor transcriptor, String path) {
                float extractionDuration = (System.nanoTime() - startTime) / 1000000000f;
                Log.i(TAG, "extractionDuration " + extractionDuration);
                final ASRProvider provider = new NuanceASRProvider();
                transcriptor.requestTranscription(path, provider, new ASRProvider.Callback() {
                    @Override
                    public void onResult(String text) {
                        if (text == null) {
                            Transcription t = new Transcription();
                            t.state = Transcription.State.FAILED;
                            video.setTranscription(t);
                            notifyItemChanged(position);
                        } else {
                            Transcription t = new Transcription();
                            t.state = Transcription.State.OK;
                            t.text = text;
                            t.asr = "nuance";
                            t.lang = provider.getLanguage();
                            t.rate = String.valueOf(provider.getSampleRate());
                            video.setTranscription(t);
                            notifyItemChanged(position);
                        }
                    }
                });
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
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
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
