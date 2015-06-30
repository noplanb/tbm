package com.zazoapp.client.ui.dialogs;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import com.zazoapp.client.R;

/**
 * Created by skamenkovych@codeminders.com on 2/10/2015.
 */
public class DoubleActionDialogFragment extends AbstractDialogFragment implements View.OnClickListener {

    private static final String TITLE = "title";
    private static final String MSG = "msg";
    private static final String ACTION_POSITIVE = "action_positive";
    private static final String ACTION_NEGATIVE = "action_negative";

    public interface DoubleActionDialogListener extends DialogListener {
        int BUTTON_POSITIVE = DialogInterface.BUTTON_POSITIVE;
        int BUTTON_NEGATIVE = DialogInterface.BUTTON_NEGATIVE;

        void onDialogActionClicked(int id, int button, Bundle bundle);
    }

    public static DialogFragment getInstance(int id, String title, String message, String actionPositive,
                                             String actionNegative, DialogListener listener) {
        return getInstance(id, title, message, actionPositive, actionNegative, false, listener);
    }

    public static DialogFragment getInstance(int id, String title, String message, String actionPositive,
                                             String actionNegative, boolean editable, DialogListener listener) {
        DoubleActionDialogFragment fragment = new DoubleActionDialogFragment();

        putData(id, title, message, actionPositive, actionNegative, editable, listener, fragment);
        return fragment;
    }

    protected static void putData(int id, String title, String message, String actionPositive, String actionNegative, boolean editable, DialogListener listener, AbstractDialogFragment fragment) {
        Bundle args = new Bundle();
        args.putString(TITLE, title);
        args.putString(MSG, message);
        args.putString(ACTION_POSITIVE, actionPositive);
        args.putString(ACTION_NEGATIVE, actionNegative);
        fragment.setArguments(args);
        fragment.setDialogListener(listener, id);
        fragment.setEditable(editable);
    }

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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_dialog_ok:
                doPositiveAction();
                break;
            case R.id.btn_dialog_cancel:
                doNegativeAction();
                break;
        }
        dismiss();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        doNegativeAction();
    }

    protected void doNegativeAction() {
        if (getListener() instanceof DoubleActionDialogListener) {
            DoubleActionDialogListener listener = ((DoubleActionDialogListener) getListener());
            listener.onDialogActionClicked(getDialogId(), DoubleActionDialogListener.BUTTON_NEGATIVE, null);
        }
    }

    protected void doPositiveAction() {
        if (getListener() instanceof DoubleActionDialogListener) {
            DoubleActionDialogListener listener = ((DoubleActionDialogListener) getListener());
            if (isEditable()) {
                Bundle bundle = new Bundle();
                putEditedMessage(bundle, getEditedMessage());
                listener.onDialogActionClicked(getDialogId(), DoubleActionDialogListener.BUTTON_POSITIVE, bundle);
            } else {
                listener.onDialogActionClicked(getDialogId(), DoubleActionDialogListener.BUTTON_POSITIVE, null);
            }
        }
    }
}
