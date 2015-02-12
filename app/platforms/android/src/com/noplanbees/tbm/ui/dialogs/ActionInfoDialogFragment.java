package com.noplanbees.tbm.ui.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class ActionInfoDialogFragment extends AbstractDialogFragment {

	private static final String TITLE = "title";
	private static final String MSG = "msg";
	private static final String ACTION = "action";
	private static final String NEED_CANCEL = "need_cancel";
	
	public interface ActionInfoDialogListener extends DialogListener {
		void onActionClicked(int id);
	}

    public static DialogFragment getInstance(String title, String message, String action,
                                             boolean isNeedToCancel) {
        return getInstance(title, message, action, -1, isNeedToCancel, null);
    }

    public static DialogFragment getInstance(String title, String message, String action,
                                             int actionId, boolean isNeedToCancel, DialogListener listener) {
        AbstractDialogFragment fragment = new ActionInfoDialogFragment();
        Bundle args = new Bundle();
        args.putString(TITLE, title);
        args.putString(MSG, message);
        args.putString(ACTION, action);
        args.putBoolean(NEED_CANCEL, isNeedToCancel);
        fragment.setDialogListener(args, listener, actionId);
        fragment.setArguments(args);
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
                if (getListener() instanceof ActionInfoDialogListener) {
                    ((ActionInfoDialogListener) getListener()).onActionClicked(getDialogId());
                }
				dismiss();
			}
		});

		setNegativeButton(needCancel?"Cancel":null, null);
	}
}
