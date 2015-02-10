package com.noplanbees.tbm.ui.dialogs;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import com.noplanbees.tbm.R;

/**
 * Created by skamenkovych@codeminders.com on 2/10/2015.
 */
public class DoubleActionDialogFragment extends AbstractDialogFragment implements View.OnClickListener {

    public static final String TITLE = "title";
    public static final String MSG = "msg";
    public static final String ACTION_POSITIVE = "action_positive";
    public static final String ACTION_NEGATIVE = "action_negative";
    public static final String ID = "dialog_id";

    public interface DoubleActionDialogListener {
        public static final int BUTTON_POSITIVE = DialogInterface.BUTTON_POSITIVE;
        public static final int BUTTON_NEGATIVE = DialogInterface.BUTTON_NEGATIVE;

        void onExceptionDialogActionClicked(int id, int button);
    }

    private DoubleActionDialogListener listener;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String title = getArguments().getString(TITLE);
        String msg = getArguments().getString(MSG);
        String actionPositive = getArguments().getString(ACTION_POSITIVE);
        String actionNegative = getArguments().getString(ACTION_NEGATIVE);

        setTitle(title);
        setMessage(msg);
        setPositiveButton(actionPositive, this);
        setNegativeButton(actionNegative, this);
    }

    public void setListener(DoubleActionDialogListener listener) {
        this.listener = listener;
    }

    @Override
    public void onClick(View v) {
        if (listener != null) {
            switch (v.getId()) {
                case R.id.btn_dialog_ok:
                    listener.onExceptionDialogActionClicked(getArguments().getInt(ID, -1), DoubleActionDialogListener.BUTTON_POSITIVE);
                    break;
                case R.id.btn_dialog_cancel:
                    listener.onExceptionDialogActionClicked(getArguments().getInt(ID, -1), DoubleActionDialogListener.BUTTON_NEGATIVE);
                    break;
            }
        }
        dismiss();
    }
}
