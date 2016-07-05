package com.zazoapp.client.ui.view;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.zazoapp.client.R;
import com.zazoapp.client.core.MessageType;
import com.zazoapp.client.utilities.Convenience;

/**
 * Created by skamenkovych@codeminders.com on 6/30/2016.
 */
public class TranscriptionViewHolder extends MessageAdapter.MessageViewHolder {
    @InjectView(R.id.text) TextView text;
    @InjectView(R.id.date) TextView date;
    @InjectView(R.id.progress_layout) ViewGroup progressLayout;
    @InjectView(R.id.main_layout) ViewGroup mainLayout;
    @InjectView(R.id.progress_message) TextView progressMessage;
    @InjectView(R.id.progress) ProgressBar progressBar;
    private View itemView;

    public TranscriptionViewHolder(View transcriptionItemView) {
        super(transcriptionItemView);
        type = MessageType.VIDEO;
        ButterKnife.inject(this, transcriptionItemView);
        itemView = transcriptionItemView;
        date.setTypeface(Convenience.getTypeface(itemView.getContext(), "Roboto-Italic"));
        text.setTypeface(Convenience.getTypeface(itemView.getContext(), "Roboto-Regular"));
    }

    public enum ViewMode {
        MESSAGE,
        PROGRESS,
        STATUS
    }

    private ViewMode mode;

    public boolean setInMode(ViewMode newMode, String mainText, String secondaryText) {
        if (newMode != mode) {
            mode = newMode;
            switch (mode) {
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
            return true;
        }
        return false;
    }
}
