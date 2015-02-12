package com.noplanbees.tbm.ui.dialogs;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class ActionInfoDialogFragment extends AbstractDialogFragment {

	private  static final String TITLE = "title";
	private static final String MSG = "msg";
	private static final String ACTION = "action";
	private static final String ID = "dialog_id";
	private static final String NEED_CANCEL = "need_cancel";
	
	public interface ActionInfoDialogListener {
		void onActionClicked(int id);
	}

    public static DialogFragment getInstance(String title, String message, String action,
                                             boolean isNeedToCancel){
        return getInstance(title, message, action, -1, isNeedToCancel);
    }

    public static DialogFragment getInstance(String title, String message, String action,
                                             int actionId, boolean isNeedToCancel){
        DialogFragment fragment = new ActionInfoDialogFragment();
        Bundle args = new Bundle();
        args.putString(ActionInfoDialogFragment.TITLE, title);
        args.putString(ActionInfoDialogFragment.MSG, message);
        args.putString(ActionInfoDialogFragment.ACTION, action);
        args.putInt(ActionInfoDialogFragment.ACTION, actionId);
        args.putBoolean(ActionInfoDialogFragment.NEED_CANCEL, isNeedToCancel);
        fragment.setArguments(args);
        return fragment;
    }
	
	private ActionInfoDialogListener actionInfoDialogListener;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		actionInfoDialogListener = (ActionInfoDialogListener) activity;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		String title = getArguments().getString(TITLE);
		String msg = getArguments().getString(MSG);
		String action = getArguments().getString(ACTION);
		final int id = getArguments().getInt(ID, -1);
		boolean needCancel = getArguments().getBoolean(NEED_CANCEL, true);
		
		setTitle(title);
		setMessage(msg);
		setPositiveButton(action, new OnClickListener() {
			@Override
			public void onClick(View v) {
				actionInfoDialogListener.onActionClicked(id);
				dismiss();
			}
		});
		
		setNegativeButton(needCancel?"Cancel":null, null);
	}
}
