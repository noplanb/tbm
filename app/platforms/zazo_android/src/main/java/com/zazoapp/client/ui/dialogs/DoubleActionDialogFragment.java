package com.zazoapp.client.ui.dialogs;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
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
        Bundle data = prepareData(title, message, actionPositive, actionNegative);
        return getInstance(id, false, data, listener);
    }

    public static DialogFragment getInstance(int id, boolean editable, Bundle data, DialogListener listener) {
        DoubleActionDialogFragment fragment = new DoubleActionDialogFragment();
        putData(id, editable, data, listener, fragment);
        return fragment;
    }

    public static Bundle prepareData(String title, String message, String actionPositive, String actionNegative) {
        Bundle args = new Bundle();
        args.putString(TITLE, title);
        args.putString(MSG, message);
        args.putString(ACTION_POSITIVE, actionPositive);
        args.putString(ACTION_NEGATIVE, actionNegative);
        return args;
    }

    protected static void putData(int id, boolean editable, Bundle data, DialogListener listener, AbstractDialogFragment fragment) {
        fragment.setArguments(data);
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
            listener.onDialogActionClicked(getDialogId(), DoubleActionDialogListener.BUTTON_NEGATIVE, new Bundle(getArguments()));
        }
    }

    protected void doPositiveAction() {
        if (getListener() instanceof DoubleActionDialogListener) {
            DoubleActionDialogListener listener = ((DoubleActionDialogListener) getListener());
            Bundle bundle = new Bundle(getArguments());
            if (isEditable()) {
                putEditedMessage(bundle, getEditedMessage());
                listener.onDialogActionClicked(getDialogId(), DoubleActionDialogListener.BUTTON_POSITIVE, bundle);
            } else {
                listener.onDialogActionClicked(getDialogId(), DoubleActionDialogListener.BUTTON_POSITIVE, bundle);
            }
        }
    }
}
