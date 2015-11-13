package com.zazoapp.client.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.zazoapp.client.R;

public class ProgressDialogFragment extends DialogFragment {

	private static final String TITLE = "title";
	private static final String MSG = "msg";
    private TextView twMsg;
    private TextView twTitle;

    public static DialogFragment getInstance(String title, String msg){
        DialogFragment fragment = new ProgressDialogFragment();
        Bundle args = new Bundle();
        args.putString(TITLE, title);
        args.putString(MSG, msg);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.base_dialog);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(false);
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.progress_dialog_body, container, true);
        twMsg = (TextView) v.findViewById(R.id.message);
        twTitle = (TextView)v.findViewById(R.id.dlg_title);
        return v;
    }

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		String title = getArguments().getString(TITLE);
		String msg = getArguments().getString(MSG);

        setTitle(title);
        setMessage(msg);
    }


    protected void setTitle(String title){
        if(title!=null) {
            twTitle.setVisibility(View.VISIBLE);
            twTitle.setText(title);
        }
    }

    protected void setMessage(String message){
        if(message!=null){
            twMsg.setVisibility(View.VISIBLE);
            twMsg.setText(message);
        }
    }
}
