package com.noplanbees.tbm.ui.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;

public class InfoDialogFragment extends AbstractDialogFragment {

	private static final String TITLE = "title";
	private static final String MSG = "msg";

    public static DialogFragment getInstance(String title, String message){
        DialogFragment fragment = new InfoDialogFragment();
        Bundle args = new Bundle();
        args.putString(InfoDialogFragment.TITLE, title);
        args.putString(InfoDialogFragment.MSG, message);
        fragment.setArguments(args);
        return fragment;
    }

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
