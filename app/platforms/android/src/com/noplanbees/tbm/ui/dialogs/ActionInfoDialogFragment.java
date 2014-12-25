package com.noplanbees.tbm.ui.dialogs;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class ActionInfoDialogFragment extends AbstractDialogFragment {

	public static final String TITLE = "title";
	public static final String MSG = "msg";
	public static final String ACTION = "action";
	public static final String ID = "dialog_id";
	public static final String NEED_CANCEL = "need_cancel";
	
	public interface Callbacks{
		void onActionClicked(int id);
	}
	
	private Callbacks callbacks;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		callbacks = (Callbacks) activity;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		String title = getArguments().getString(TITLE);
		String msg = getArguments().getString(MSG);
		String action = getArguments().getString(ACTION);
		final int id = getArguments().getInt(ID);
		boolean needCancel = getArguments().getBoolean(NEED_CANCEL, true);
		
		setTitle(title);
		setMessage(msg);
		setPositiveButton(action, new OnClickListener() {
			@Override
			public void onClick(View v) {
				callbacks.onActionClicked(id);
				dismiss();
			}
		});
		
		setNegativeButton(needCancel?"Cancel":null, null);
	}
}