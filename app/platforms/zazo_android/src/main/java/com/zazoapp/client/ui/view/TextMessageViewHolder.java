package com.zazoapp.client.ui.view;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import com.zazoapp.client.R;
import com.zazoapp.client.core.MessageType;
import com.zazoapp.client.utilities.Convenience;

/**
 * Created by skamenkovych@codeminders.com on 8/5/2016.
 */
public class TextMessageViewHolder extends MessageAdapter.MessageViewHolder {
    @InjectView(R.id.text) TextView text;
    @InjectView(R.id.date) TextView date;
    @Optional @InjectView(R.id.name) TextView name;
    @InjectView(R.id.main_layout) ViewGroup mainLayout;
    private View itemView;

    public TextMessageViewHolder(View transcriptionItemView) {
        super(transcriptionItemView);
        ButterKnife.inject(this, transcriptionItemView);
        type = MessageType.TEXT;
        itemView = transcriptionItemView;
        date.setTypeface(Convenience.getTypeface(itemView.getContext(), Convenience.ITALIC));
        text.setTypeface(Convenience.getTypeface(itemView.getContext(), Convenience.NORMAL));
    }

    public void setData(String mainText, String secondaryText) {
        text.setText(mainText);
        date.setText(secondaryText);
    }

    public void setName(String text) {
        if (name != null) {
            if (text != null) {
                name.setText(text);
                name.setVisibility(View.VISIBLE);
            } else {
                name.setVisibility(View.GONE);
            }
        }
    }

    public void setOnClickListener(View.OnClickListener listener) {
        itemView.setOnClickListener(listener);
        if (listener == null && itemView.isClickable()) {
            itemView.setClickable(false);
        }
    }
}
