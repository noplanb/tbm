package com.noplanbees.tbm.ui.dialogs;

import android.os.Bundle;
import android.view.View;

public class InfoDialogFragment extends AbstractDialogFragment {

	public static final String TITLE = "title";
	public static final String MSG = "msg";
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		String title = getArguments().getString(TITLE);
		String msg = getArguments().getString(MSG);
		setTitle(title);
		setMessage(msg);
		setPositiveButton("OK", null);
	}
}
