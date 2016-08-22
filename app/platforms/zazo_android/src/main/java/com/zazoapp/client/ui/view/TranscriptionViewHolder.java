package com.zazoapp.client.ui.view;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.InjectView;
import com.zazoapp.client.R;
import com.zazoapp.client.core.MessageType;

/**
 * Created by skamenkovych@codeminders.com on 6/30/2016.
 */
public class TranscriptionViewHolder extends TextMessageViewHolder {

    @InjectView(R.id.progress_layout) ViewGroup progressLayout;
    @InjectView(R.id.progress_message) TextView progressMessage;
    @InjectView(R.id.progress) ProgressBar progressBar;

    public TranscriptionViewHolder(View transcriptionItemView) {
        super(transcriptionItemView);
        type = MessageType.VIDEO;
    }

    public enum ViewMode {
        MESSAGE,
        PROGRESS,
        STATUS
    }

    public void setInMode(ViewMode newMode, String mainText, String secondaryText) {
        switch (newMode) {
            case PROGRESS:
                mainLayout.setVisibility(View.GONE);
                progressLayout.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                progressMessage.setText(R.string.converting_to_text);
                break;
            case MESSAGE:
                mainLayout.setVisibility(View.VISIBLE);
                progressLayout.setVisibility(View.GONE);
                text.setText(mainText);
                date.setText(secondaryText);
                break;
            case STATUS:
                mainLayout.setVisibility(View.GONE);
                progressLayout.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                progressMessage.setText(mainText);
                break;
        }
    }

    @Override
    public void setName(String text) {
        throw new UnsupportedOperationException("name isn't supported for transcription item");
    }
}
