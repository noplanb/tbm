package com.zazoapp.client.ui.dialogs;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.View.OnClickListener;
import com.zazoapp.client.R;

public class ActionInfoDialogFragment extends AbstractDialogFragment {

    private static final String TITLE = "title";
    private static final String MSG = "msg";
    private static final String ACTION = "action";
    private static final String NEED_CANCEL = "need_cancel";

    public interface ActionInfoDialogListener extends DialogListener {
        void onActionClicked(int id, Bundle bundle);
    }

    public static DialogFragment getInstance(String title, String message, String action,
                                             boolean isNeedToCancel) {
        return getInstance(title, message, action, -1, isNeedToCancel, false, null);
    }

    public static DialogFragment getInstance(String title, String message, String action,
                                             int actionId, boolean isNeedToCancel, boolean editable, DialogListener listener) {
        AbstractDialogFragment fragment = new ActionInfoDialogFragment();
        Bundle args = new Bundle();
        args.putString(TITLE, title);
        args.putString(MSG, message);
        args.putString(ACTION, action);
        args.putBoolean(NEED_CANCEL, isNeedToCancel);
        fragment.setArguments(args);
        fragment.setDialogListener(listener, actionId);
        fragment.setEditable(editable);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String title = getArguments().getString(TITLE);
        String msg = getArguments().getString(MSG);
        String action = getArguments().getString(ACTION);
        boolean needCancel = getArguments().getBoolean(NEED_CANCEL, true);

        setTitle(title);
        setMessage(msg);
        setPositiveButton(action, new OnClickListener() {
            @Override
            public void onClick(View v) {
                doAction();
            }
        });

        setNegativeButton(needCancel ? view.getContext().getResources().getString(R.string.dialog_action_cancel) : null, null);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        boolean needCancel = getArguments().getBoolean(NEED_CANCEL, true);
        if (!needCancel) {
            doAction();
        }
    }

    private void doAction() {
        if (getListener() instanceof ActionInfoDialogListener) {
            if (isEditable()) {
                Bundle bundle = new Bundle();
                putEditedMessage(bundle, getEditedMessage());
                ((ActionInfoDialogListener) getListener()).onActionClicked(getDialogId(), bundle);
            } else {
                ((ActionInfoDialogListener) getListener()).onActionClicked(getDialogId(), null);
            }
        }
        dismiss();
    }

}